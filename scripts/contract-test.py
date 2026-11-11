#!/usr/bin/env python3
"""프론트(app.js) ↔ 백엔드 응답 계약 검증.
app.js 가 읽는 필드/ID 가 실제 응답에 존재하고 타입이 맞는지, 그리고 한 응답의 ID 가 다음 요청의 PK 로 그대로 쓰이는지 확인한다.
사용: 서버 기동 + python3 scripts/seed-local.py 후  python3 scripts/contract-test.py"""
import json, os, re, sys, time, urllib.parse, urllib.request, urllib.error, uuid

BASE = os.environ.get("BASE_URL", "http://localhost:8080")
PW = "Passw0rd!"
TOK = None
PASS = FAIL = 0


def call(method, path, body=None, multipart=None):
    headers = {}
    if TOK:
        headers["Authorization"] = "Bearer " + TOK
    data = None
    if multipart:
        b = uuid.uuid4().hex
        parts = []
        for name, val in multipart.items():
            parts.append(f'--{b}\r\nContent-Disposition: form-data; name="{name}"\r\nContent-Type: application/json\r\n\r\n{json.dumps(val, ensure_ascii=False)}\r\n'.encode())
        data = b"".join(parts) + f"--{b}--\r\n".encode()
        headers["Content-Type"] = f"multipart/form-data; boundary={b}"
    elif body is not None:
        data = json.dumps(body).encode()
        headers["Content-Type"] = "application/json"
    req = urllib.request.Request(BASE + path, data=data, method=method, headers=headers)
    try:
        with urllib.request.urlopen(req) as r:
            raw = r.read()
            return r.status, (json.loads(raw) if raw else None)
    except urllib.error.HTTPError as e:
        raw = e.read()
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, raw.decode(errors="replace")


def expect(cond, msg, detail=""):
    global PASS, FAIL
    if cond:
        PASS += 1
        print(f"  ✅ {msg}")
    else:
        FAIL += 1
        print(f"  ❌ {msg}  {detail}"[:300])


def fields(obj, spec, where):
    """spec: {field: type or (type, ...)}; None 허용은 type 에 type(None) 포함."""
    for f, t in spec.items():
        ok = f in obj and isinstance(obj[f], t)
        expect(ok, f"{where}.{f} : {getattr(t, '__name__', t)}", f"got {obj.get(f, '<missing>')!r}")


def login(role):
    global TOK
    s, j = call("POST", "/users/login", {"email": f"{role}@test.com", "password": PW})
    assert s == 200, f"login {role}: {j}"
    fields(j["data"], {"accessToken": str, "refreshToken": str, "email": str}, "login.data")
    TOK = j["data"]["accessToken"]


NUM = (int, float)
PAGE = {"content": list, "totalElements": int, "last": bool, "number": int}

print("== 로그인 / 내 정보  (app.js: tokens.set, loadMe, pages.me)")
login("CUSTOMER")
s, j = call("GET", "/users")
fields(j["data"], {"name": str, "phoneNum": str, "role": str, "userGrade": str, "pointRate": str, "requirement": str}, "GET /users.data")
expect(j["data"]["role"] in ("ADMIN", "VENDOR", "CUSTOMER"), "role is one of the enum values used by isStaff()")

print("== 홈: 브랜드 + 전체 상품 검색  (pages.home)")
s, bj = call("GET", "/brands?page=0&size=100")
fields(bj["data"], PAGE, "GET /brands.data")
expect(len(bj["data"]["content"]) > 0, "at least one brand (seed)")
brand = next(b for b in bj["data"]["content"] if call("GET", f"/products/brands/{b['id']}?page=0&size=1")[1]["data"]["totalElements"] > 0)  # 상품이 있는 브랜드
fields(brand, {"id": int, "name": str}, "brands.content[]")
s, pj = call("GET", "/products?page=0&size=12")
fields(pj["data"], PAGE, "GET /products.data")
expect(len(pj["data"]["content"]) > 0, "products present")
p0 = pj["data"]["content"][0]
fields(p0, {"id": int, "name": str, "price": NUM, "status": str, "brandId": int, "imageUrl": (str, type(None))}, "products.content[]")

print("== 브랜드 페이지  (pages.brand)  — brand.id → /brands/{id}, /products/brands/{id}")
s, j = call("GET", f"/brands/{brand['id']}")
fields(j["data"], {"id": int, "name": str, "context": (str, type(None))}, "GET /brands/{id}.data")
expect(j["data"]["id"] == brand["id"], "brand PK round-trips (list id == detail id)")
s, j = call("GET", f"/products/brands/{brand['id']}?page=0&size=60&nameKeyword=&minPrice=&maxPrice=")
fields(j["data"], PAGE, "GET /products/brands/{id}.data (empty filter params accepted)")
expect(all(p["brandId"] == brand["id"] for p in j["data"]["content"]), "every product.brandId == requested brand id")
prod = j["data"]["content"][0]

print("== 상품 상세  (pages.product)  — product.id → /products/{id}, options[].id → cart productOptionId")
s, j = call("GET", f"/products/{prod['id']}")
d = j["data"]
fields(d, {"id": int, "name": str, "price": NUM, "imageUrl": (str, type(None)), "options": list}, "GET /products/{id}.data")
expect(d["id"] == prod["id"] and d["name"] == prod["name"], "product PK/name consistent between list and detail")
expect(len(d["options"]) > 0, "product has options (seed)")
opt = d["options"][0]
fields(opt, {"id": int, "size": str, "color": str, "status": str, "product": int}, "options[]")
expect(opt["product"] == prod["id"], "option.product FK == product id")
s, j = call("GET", f"/reviews/products/{prod['id']}?page=0&size=50&minRate=&maxRate=")
fields(j["data"], PAGE, "GET /reviews/products/{id}.data (empty rate params accepted)")

print("== 장바구니  (pages.cart)  — POST body {productId, quantity, productOptionId}; reads cartItemId, productId, productPrice, quantity, productOptions[0].id")
s, j = call("GET", "/carts/users?page=0&size=100")
for x in j["data"]["content"]:  # 이전 실행/부하테스트 잔여물 제거 → 주문에 우리 항목만 들어가게
    call("DELETE", f"/carts/{x['cartItemId']}")
s, j = call("POST", "/carts", {"productId": prod["id"], "quantity": 2, "productOptionId": opt["id"]})
expect(s == 201, "POST /carts 201", j)
c = j["data"]
fields(c, {"cartItemId": int, "quantity": int, "productId": int, "productPrice": NUM, "productOptions": list}, "POST /carts.data")
expect(c["productId"] == prod["id"], "cart.productId == product id sent")
expect(c["quantity"] == 2, "cart.quantity echoes request")
expect(float(c["productPrice"]) == float(prod["price"]), "cart.productPrice == product.price")
s, j = call("GET", "/carts/users?page=0&size=100")
fields(j["data"], PAGE, "GET /carts/users.data")
row = next((x for x in j["data"]["content"] if x["cartItemId"] == c["cartItemId"]), None)
expect(row is not None, "created cart item appears in list by cartItemId")
expect(bool(row and row["productOptions"] and isinstance(row["productOptions"][0]["id"], int)), "cart row has productOptions[0].id (used for qty PATCH)")
s, j = call("PATCH", f"/carts/{c['cartItemId']}", {"productId": prod["id"], "quantity": 3, "productOptionId": row["productOptions"][0]["id"]})
expect(s == 200 and j["data"]["quantity"] == 3, "PATCH /carts/{cartItemId} updates quantity", j)
login("VENDOR")  # 이 고객이 아직 안 쓴 쿠폰을 보장하기 위해 새로 발급
s, j = call("POST", "/coupons", {"couponName": "계약테스트 10%", "discountRate": 10, "couponType": "UNIVERSAL", "isLimited": True, "maxCount": 50, "brands": []})
expect(s == 200 and isinstance(j["data"]["id"], int), "vendor creates coupon (admin tab form shape)", j)
new_cp = j["data"]["id"]
login("CUSTOMER")
s, cj = call("GET", "/coupons")
expect(s == 200 and isinstance(cj["data"], list), "GET /coupons list")
expect(any(c["id"] == new_cp for c in cj["data"]), "freshly created coupon is in the cart dropdown list")
if cj["data"]:
    fields(cj["data"][0], {"id": int, "couponName": str, "discountRate": NUM, "expirationDate": str}, "coupons[]")
    s, j = call("POST", f"/carts/{c['cartItemId']}/apply-coupon", {"couponId": new_cp})
    expect(s == 200, "apply-coupon by listed coupon id", j)
    fields(j["data"], {"cartItemId": int, "discountedPrice": NUM, "originalPrice": NUM}, "apply-coupon.data")
    expect(float(j["data"]["discountedPrice"]) < float(j["data"]["originalPrice"]), "discountedPrice < originalPrice")

print("== 주문  (pages.cart → POST /orders/carts → #/pay/{data.id}; pages.orders)")
s, j = call("POST", "/orders/carts")
expect(s == 201, "POST /orders/carts 201", j)
o = j["data"]
fields(o, {"id": int, "orderId": str, "totalPrice": NUM, "status": str, "userId": int, "orderItems": list}, "orders.data")
expect(o["status"] == "BEFOREPAYMENT", "new order status BEFOREPAYMENT (pay button shown)")
expect(len(o["orderItems"]) == 1, "order contains exactly the one cart item", o["orderItems"])
it = o["orderItems"][0]
fields(it, {"orderItemId": int, "productId": int, "productName": str, "quantity": int, "price": NUM, "discountPrice": (NUM, type(None))}, "orderItems[]")
expect(it["productId"] == prod["id"] and it["quantity"] == 3, "orderItem carries product id + updated qty from cart", it)
line_sum = sum(float(x["discountPrice"] if x["discountPrice"] is not None else x["price"]) * x["quantity"] for x in o["orderItems"])
expect(abs(line_sum - float(o["totalPrice"])) < 0.01, "sum(orderItems discountPrice x qty) == totalPrice (what the orders page displays)", f"{line_sum} vs {o['totalPrice']}")
if cj["data"]:
    expect(float(it["discountPrice"]) < float(it["price"]), "orderItem.discountPrice reflects applied coupon", it)
s, j = call("GET", "/carts/users?page=0&size=100")
expect(all(x["cartItemId"] != c["cartItemId"] for x in j["data"]["content"]), "cart emptied after order")
s, j = call("GET", f"/orders/{o['id']}")
expect(s == 200 and j["data"]["id"] == o["id"] and j["data"]["orderId"] == o["orderId"], "GET /orders/{id} by numeric PK matches (pay page uses this)")
s, j = call("GET", "/orders/users?page=0&size=50&startDate=&endDate=")
fields(j["data"], PAGE, "GET /orders/users.data (empty date params accepted)")
expect(any(x["id"] == o["id"] for x in j["data"]["content"]), "order appears in my list")

print("== 결제 페이지  (pages.pay)  — /api/client-key + POST /api/create {method, order: id}")
s, j = call("GET", "/api/client-key")
expect(s == 200 and isinstance(j.get("clientKey"), str), "client-key JSON {clientKey}", j)
s, j = call("POST", "/api/create", {"method": "CARD", "order": o["id"]})
expect(s == 201, "POST /api/create 201", j)
fields(j, {"id": int, "orderId": str, "amount": NUM, "status": str}, "payment (unwrapped, no CommonResponse)")
expect(j["orderId"] == o["orderId"] and float(j["amount"]) == float(o["totalPrice"]), "payment.orderId == order.orderId (Toss orderId) and amount == totalPrice")

print("== 주문 상태 버튼  (ORDER_ACTIONS)  — PATCH /orders {orderId: numeric id, status}")
s, j = call("PATCH", "/orders", {"orderId": o["id"], "status": "CANCEL"})
expect(s == 200 and j["data"]["status"] == "CANCEL", "customer CANCEL from BEFOREPAYMENT", j)
s, j = call("PATCH", "/orders", {"orderId": o["id"], "status": "ONDELIVERY"})
expect(s == 400, "customer ONDELIVERY rejected 400 (front hides this button)", j)

print("== 리뷰 작성  (orders page modal)  — multipart requestDto{rate, content, productId} to /reviews/order-item/{orderItemId}")
login("ADMIN")
s, j = call("PATCH", "/orders", {"orderId": o["id"], "status": "DELIVERYCOMPLETED"})
expect(s == 200, "admin sets DELIVERYCOMPLETED")
login("CUSTOMER")
s, j = call("POST", f"/reviews/order-item/{it['orderItemId']}", multipart={"requestDto": {"rate": 5, "content": "계약 테스트", "productId": it["productId"]}})
expect(s == 201, "review create via orderItemId", j)
fields(j["data"], {"id": int, "productId": int, "orderItemId": int, "rate": int, "content": str, "imageUrl": (str, type(None))}, "review.data")
expect(j["data"]["orderItemId"] == it["orderItemId"] and j["data"]["productId"] == it["productId"], "review FKs match orderItem/product")
rv = j["data"]["id"]
s, j = call("GET", f"/reviews/products/{prod['id']}?page=0&size=50")
expect(any(r["id"] == rv for r in j["data"]["content"]), "review appears on product page list")
call("DELETE", f"/reviews/{rv}")

print("== 신고  (reportDialog)  — {targetId, targetType, status:'OPEN', content}")
s, j = call("POST", "/reports", {"targetId": prod["id"], "targetType": "PRODUCT", "status": "OPEN", "content": "계약"})
expect(s == 201, "report create", j)
fields(j["data"], {"id": int, "targetId": int, "targetType": str, "status": str, "content": str}, "report.data")
rp = j["data"]["id"]
login("ADMIN")
s, j = call("GET", "/reports?page=0&size=100")
fields(j["data"], PAGE, "GET /reports.data")
expect(any(r["id"] == rp for r in j["data"]["content"]), "report listed for admin")
s, j = call("PATCH", f"/reports/{rp}/status", {"status": "RESOLVED", "rejectReason": None})
expect(s == 200, "report process with null rejectReason")
call("DELETE", f"/reports/{rp}")

print("== 관리 탭  (adminTabs)  — categories, sub-categories, brands/owners, statistics")
s, j = call("GET", "/categories?page=0&size=100")
fields(j["data"], PAGE, "GET /categories.data")
fields(j["data"]["content"][0], {"id": int, "name": str}, "categories[]")
s, j = call("GET", "/sub-categories")
expect(s == 200 and isinstance(j["data"], list) and len(j["data"]) > 0, "GET /sub-categories list")
fields(j["data"][0], {"id": int, "parentId": int, "name": str}, "sub-categories[]")
s, j = call("GET", f"/sub-categories?categoryId={j['data'][0]['parentId']}")
expect(s == 200 and all(x["parentId"] == int(x["parentId"]) for x in j["data"]), "sub-categories filtered by categoryId")
s, j = call("GET", "/admin/statistics/sales?periodType=MONTH&startDate=&endDate=")
expect(s == 200 and "headerData" in j and "bodyDataList" in j, "statistics shape {headerData, bodyDataList} (unwrapped)", j)
login("VENDOR")
s, j = call("GET", "/brands/owners")
expect(s == 200 and isinstance(j["data"], list) and len(j["data"]) > 0, "GET /brands/owners list for vendor")
fields(j["data"][0], {"id": int, "name": str, "registrationNum": str}, "brands/owners[]")

print("== 쿠폰 UX  (cart dropdown excludes used; pages.me 내 쿠폰 = GET /coupons/me)")
login("CUSTOMER")
s, j = call("GET", "/coupons")
expect(s == 200 and all(c["id"] != new_cp for c in j["data"]), "coupon just applied is no longer in GET /coupons for this user")
s, j = call("GET", "/coupons/me")
expect(s == 200 and isinstance(j["data"], list) and len(j["data"]) > 0, "GET /coupons/me list")
mine = next((c for c in j["data"] if c["couponId"] == new_cp), None)
expect(mine is not None, "applied coupon appears in /coupons/me by couponId")
if mine:
    fields(mine, {"couponUserId": int, "couponId": int, "couponName": str, "couponCode": str, "discountRate": NUM, "expirationDate": str, "used": bool}, "coupons/me[]")
    expect(mine["used"] is True and isinstance(mine["usedAt"], str), "applied coupon is used=true with usedAt")

print("== 페이지네이션  (pager(): totalPages, number, first, last 를 읽는다)")
PAGER = {"totalPages": int, "number": int, "first": bool, "last": bool, "totalElements": int}
for path in [f"/orders/users?page=0&size=1", f"/reviews/products/{prod['id']}?page=0&size=1", f"/products/brands/{brand['id']}?page=0&size=1", "/products?page=0&size=1"]:
    s, j = call("GET", path)
    expect(s == 200, f"GET {path} 200", j)
    fields(j["data"], PAGER, path.split('?')[0])
    if j["data"]["totalElements"] > 1:
        expect(j["data"]["totalPages"] == j["data"]["totalElements"], f"{path.split('?')[0]} size=1 → totalPages == totalElements")
        s2, j2 = call("GET", path.replace("page=0", "page=1"))
        expect(s2 == 200 and j2["data"]["number"] == 1 and j2["data"]["first"] is False, f"{path.split('?')[0]} page=1 is navigable")

print("== 주문 관리  (adminTabs.orders: GET /orders/manage, STAFF_ACTIONS → PATCH /orders)")
login("ADMIN")
s, j = call("GET", "/orders/manage?page=0&size=5")
expect(s == 200, "ADMIN GET /orders/manage 200", j)
fields(j["data"], PAGER, "orders/manage.data")
if j["data"]["content"]:
    fields(j["data"]["content"][0], {"id": int, "orderId": str, "userName": str, "status": str, "totalPrice": NUM, "orderItems": list}, "orders/manage[]")
s, j = call("GET", "/orders/manage?status=CANCEL&size=50")
expect(s == 200 and all(x["status"] == "CANCEL" for x in j["data"]["content"]), "status filter returns only that status")
s, j = call("GET", "/orders/manage?startDate=2000-01-01&endDate=2000-01-02")
expect(s == 200 and j["data"]["totalElements"] == 0, "date range filter (iso) binds and filters")
s, j = call("GET", "/reports?page=0&size=1")
fields(j["data"], PAGER, "reports.data (admin pager)")
login("VENDOR")
s, j = call("GET", "/orders/manage?size=50")
expect(s == 200, "VENDOR GET /orders/manage 200", j)
ids = [x["id"] for x in j["data"]["content"]]
expect(o["id"] in ids, "vendor sees the order containing its own brand's product")
s, j = call("GET", f"/orders/{o['id']}")  # 현재 상태 확인 후 입점사 전이 1회
cur = j["data"]["status"] if s == 200 else None
nxt = {"BEFOREPAYMENT": "PAYMENTCOMPLETED", "PAYMENTCOMPLETED": "ONDELIVERY", "ONDELIVERY": "DELIVERYCOMPLETED", "CANCEL": "RETURN", "DELIVERYCOMPLETED": "ORDERCOMPLETED"}.get(cur)
if nxt:
    s, j = call("PATCH", "/orders", {"orderId": o["id"], "status": nxt})
    expect(s == 200 and j["data"]["status"] == nxt, f"vendor staff transition {cur} → {nxt}", j)
login("CUSTOMER")
s, j = call("GET", "/orders/manage")
expect(s in (401, 403), "CUSTOMER cannot read /orders/manage", s)

print("== 입점사 상품 관리  (adminTabs.brand: PATCH /products/{id}, POST/PATCH options, DELETE /products/{id})")
login("VENDOR")
s, j = call("GET", "/brands/owners"); bid = j["data"][0]["id"]
s, j = call("POST", f"/products/brands/{bid}", multipart={"productRequestDto": {"name": "계약테스트 상품", "price": 10000, "brandId": bid, "status": "AVAILABLE"}})
expect(s == 201, "vendor creates product (no image → imageUrl null is fine)", j); pid = j["data"]["id"]
s, j = call("PATCH", f"/products/{pid}", multipart={"productUpdateDto": {"name": "계약테스트 상품(수정)", "price": 12000, "status": "SOLD_OUT"}})
expect(s == 200, "PATCH /products/{id} multipart productUpdateDto", j)
if s == 200:
    fields(j["data"], {"id": int, "name": str, "price": NUM, "status": str}, "product update.data")
    expect(float(j["data"]["price"]) == 12000 and j["data"]["status"] == "SOLD_OUT", "update applied (price/status)")
s, j = call("POST", f"/options/products/{pid}", {"size": "XL", "color": "Navy", "status": "SOLD_OUT"})
expect(s == 201 and j["data"]["status"] == "SOLD_OUT", "option create echoes requested status (not hardcoded AVAILABLE)", j); oid = j["data"]["id"]
s, j = call("PATCH", f"/options/{oid}", {"size": "XL", "color": "Navy", "status": "AVAILABLE"})
expect(s == 200 and j["data"]["status"] == "AVAILABLE", "option status update", j)
s, j = call("GET", f"/products/{pid}")
expect(any(x["id"] == oid and x["status"] == "AVAILABLE" for x in j["data"]["options"]), "product detail options reflect option update")
ts = str(int(time.time()))
call("POST", "/users/signin", {"email": f"v2{ts}@r.test", "phoneNum": "010-2222-3333", "password": PW, "name": "다른입점사", "role": "VENDOR"})
s, j = call("POST", "/users/login", {"email": f"v2{ts}@r.test", "password": PW}); TOK = j["data"]["accessToken"]
s, j = call("PATCH", f"/products/{pid}", multipart={"productUpdateDto": {"price": 1}})
expect(s in (401, 403), "another vendor cannot edit this vendor's product", s)
s, j = call("POST", f"/options/products/{pid}", {"size": "S", "color": "Red", "status": "AVAILABLE"})
expect(s in (401, 403), "another vendor cannot add options to this vendor's product", s)
s, j = call("DELETE", f"/products/{pid}")
expect(s in (401, 403), "another vendor cannot delete this vendor's product", s)
login("VENDOR")
s, j = call("DELETE", f"/products/{pid}")
expect(s == 204, "owner deletes product (cascade options)", s)

print("== 통계  (adminTabs.stats: POST /admin/statistics/aggregate → GET sales/count; rowList=날짜, columnList=브랜드+총합)")
login("ADMIN")
today = time.strftime("%Y-%m-%d")
s, j = call("POST", f"/admin/statistics/aggregate?date={today}")
expect(s == 200 and j.get("date") == today and isinstance(j.get("rows"), int), "aggregate returns {date, rows}", j)
rows_first = j.get("rows")
s, j2 = call("POST", f"/admin/statistics/aggregate?date={today}")
expect(s == 200 and j2.get("rows") == rows_first, "re-running the same date is idempotent (same row count)", j2)
expect(rows_first > 0, "today's orders produced statistics rows (orders were created by this test)")
s, j = call("GET", f"/admin/statistics/sales?periodType=DAY&startDate={today}&endDate={today}")
expect(s == 200 and today in j["headerData"]["rowList"], "sales DAY includes today's row", j)
expect(j["headerData"]["columnList"][-1] == "총합" and len(j["bodyDataList"]) == len(j["headerData"]["columnList"]), "bodyDataList aligns with columnList (brands + 총합)")
expect(all(len(b["dataList"]) == len(j["headerData"]["rowList"]) for b in j["bodyDataList"]), "each brand row has one value per date")
s, j = call("GET", f"/admin/statistics/count?periodType=DAY&startDate={today}&endDate={today}&orderStatus=BEFOREPAYMENT")
expect(s == 200 and "headerData" in j, "count with orderStatus filter", j)
s, j = call("GET", f"/admin/statistics/sales?periodType=DAY&startDate={today}&endDate={today}&brandList={urllib.parse.quote(j['headerData']['columnList'][0]) if j['headerData']['columnList'] and j['headerData']['columnList'][0] != '총합' else ''}")
expect(s == 200, "sales with (url-encoded) brandList filter", j)
login("VENDOR")
s, j = call("POST", f"/admin/statistics/aggregate?date={today}")
expect(s == 403, "VENDOR cannot trigger aggregation", s)

print("== 에러 응답 형태  (api(): json.message 를 토스트로 표시)")
TOK = None
s, j = call("GET", "/carts/users")
expect(s == 401 and isinstance(j, dict) and "message" in j, "401 body has .message", j)
s, j = call("POST", "/users/login", {"email": "nope@x.test", "password": "x"})
expect(s in (400, 404) and "message" in j, "login failure body has .message", j)
s, j = call("POST", "/users/signin", {"email": "bad", "password": "x", "name": "", "role": "CUSTOMER"})
expect(s == 400 and "message" in j, "validation 400 body has .message", j)

print(f"\nRESULT: {PASS} passed, {FAIL} failed")
sys.exit(1 if FAIL else 0)
