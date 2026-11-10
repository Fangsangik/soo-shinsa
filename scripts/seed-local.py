#!/usr/bin/env python3
"""로컬 개발용 시드. 서버 기동 후 실행:  python3 scripts/seed-local.py
계정 3개(ADMIN/VENDOR/CUSTOMER, pw Passw0rd!) + 카테고리/서브카테고리/브랜드/상품/옵션(mock 이미지) + 쿠폰.
이미 존재하는 계정/데이터는 건너뜁니다(재실행 안전)."""
import json, os, sys, urllib.request, urllib.error, uuid

BASE = os.environ.get("BASE_URL", "http://localhost:8080")
PW = "Passw0rd!"
TOK = None


def call(method, path, body=None, files=None):
    headers = {}
    if TOK:
        headers["Authorization"] = "Bearer " + TOK
    if files:  # multipart: {"jsonPartName": dict, "imageFile": (filename, bytes)}
        boundary = uuid.uuid4().hex
        parts = []
        for name, val in files.items():
            if isinstance(val, dict):
                parts.append(f'--{boundary}\r\nContent-Disposition: form-data; name="{name}"\r\nContent-Type: application/json\r\n\r\n{json.dumps(val, ensure_ascii=False)}\r\n'.encode())
            else:
                fn, data = val
                parts.append(f'--{boundary}\r\nContent-Disposition: form-data; name="{name}"; filename="{fn}"\r\nContent-Type: image/png\r\n\r\n'.encode() + data + b"\r\n")
        data = b"".join(parts) + f"--{boundary}--\r\n".encode()
        headers["Content-Type"] = f"multipart/form-data; boundary={boundary}"
    else:
        data = json.dumps(body).encode() if body is not None else None
        if data:
            headers["Content-Type"] = "application/json"
    req = urllib.request.Request(BASE + path, data=data, method=method, headers=headers)
    try:
        with urllib.request.urlopen(req) as r:
            return r.status, json.loads(r.read() or b"null")
    except urllib.error.HTTPError as e:
        try:
            return e.code, json.loads(e.read())
        except Exception:
            return e.code, None


def login(role):
    global TOK
    s, j = call("POST", "/users/login", {"email": f"{role}@test.com", "password": PW})
    assert s == 200, f"login {role} failed: {j}"
    TOK = j["data"]["accessToken"]


TOK = None
for role in ["ADMIN", "VENDOR", "CUSTOMER"]:
    s, j = call("POST", "/users/signin", {"email": f"{role}@test.com", "phoneNum": "010-1111-2222", "password": PW, "name": role, "role": role})
    print(f"signup {role}: {s} {j and j.get('message')}")

login("ADMIN")
s, j = call("GET", "/categories?page=0&size=100")
existing = {c["name"]: c["id"] for c in j["data"]["content"]}
subs = {}
tree = {"상의": ["티셔츠", "셔츠", "니트"], "하의": ["데님", "슬랙스"], "신발": ["스니커즈"]}
for cat, children in tree.items():
    if cat in existing:
        print(f"category {cat}: exists, skip")
        continue
    s, j = call("POST", "/categories", {"name": cat})
    cat_id = j["data"]["id"]
    for sub in children:
        s, j = call("POST", "/sub-categories", {"parentId": cat_id, "name": sub})
        subs[sub] = j["data"]["id"]
    print(f"category {cat} -> subs {children}")

login("VENDOR")
s, j = call("GET", "/brands/owners")
if j["data"]:
    print(f"brands already exist for VENDOR ({len(j['data'])}), skip products")
    sys.exit(0)
if not subs:
    print("서브카테고리가 없어 브랜드를 만들 수 없습니다. (카테고리가 이미 있으면 관리 화면에서 직접 등록)")
    sys.exit(1)

png = b"\x89PNG\r\n\x1a\n"  # 실제 바이너리는 저장되지 않고 mock URL만 기록됨
brands = [("101-11-11111", "무신사 스탠다드", "베이직 데일리웨어", "티셔츠"),
          ("202-22-22222", "커버낫", "스트릿 캐주얼", "셔츠"),
          ("303-33-33333", "뉴발란스", "러닝과 라이프스타일", "스니커즈")]
products = {"무신사 스탠다드": [("코튼 티셔츠", 19900), ("헤비웨이트 맨투맨", 39900), ("와이드 데님", 49900)],
            "커버낫": [("옥스포드 셔츠", 59000), ("로고 후드", 79000)],
            "뉴발란스": [("993 그레이", 279000), ("530 화이트", 129000)]}
for reg, name, ctx, sub in brands:
    s, j = call("POST", "/brands", {"registrationNum": reg, "name": name, "context": ctx, "status": "OPEN", "subCategoryId": subs[sub]})
    assert s == 201, j
    bid = j["data"]["id"]
    for pname, price in products[name]:
        s, j = call("POST", f"/products/brands/{bid}", files={"productRequestDto": {"name": pname, "price": price, "brandId": bid, "status": "AVAILABLE"}, "imageFile": ("mock.png", png)})
        assert s == 201, j
        pid = j["data"]["id"]
        for size, color in [("S", "Black"), ("M", "Black"), ("L", "White")]:
            call("POST", f"/options/products/{pid}", {"size": size, "color": color, "status": "AVAILABLE"})
        print(f"  brand {name} / product #{pid} {pname} {price:,}원")

s, j = call("POST", "/coupons", {"couponName": "신규회원 10%", "discountRate": 10, "couponType": "UNIVERSAL", "isLimited": True, "maxCount": 100, "brands": []})
print(f"coupon: {j and j.get('message')} (id={j and j.get('data', {}).get('id')})")
print(f"\ndone. 로그인: ADMIN@test.com / VENDOR@test.com / CUSTOMER@test.com  (pw: {PW})")
