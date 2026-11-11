#!/usr/bin/env python3
"""통계 데모용 mock 주문. 브랜드마다 지난 30일에 걸쳐 주문을 만들고(실제 API), 주문일을 과거로 돌린 뒤(MySQL), 날짜별로 집계합니다.
사용: 서버 기동 + seed-local.py 실행 후  python3 scripts/seed-stats.py  [--days 30] [--per-brand 25]
재실행하면 그만큼 주문이 더 쌓입니다(멱등 아님)."""
import argparse, json, os, random, re, subprocess, sys, urllib.request, urllib.error
from datetime import date, timedelta

BASE = os.environ.get("BASE_URL", "http://localhost:8080")
PW = "Passw0rd!"
MYSQL = next((p for p in ["/opt/homebrew/opt/mysql@8.0/bin/mysql", "/opt/homebrew/opt/mysql-client/bin/mysql", "/usr/local/bin/mysql", "mysql"] if os.path.exists(p) or p == "mysql"), "mysql")
TOK = None


def call(method, path, body=None):
    headers = {"Authorization": "Bearer " + TOK} if TOK else {}
    data = None
    if body is not None:
        data = json.dumps(body).encode(); headers["Content-Type"] = "application/json"
    req = urllib.request.Request(BASE + path, data=data, method=method, headers=headers)
    try:
        with urllib.request.urlopen(req) as r:
            return r.status, json.loads(r.read() or b"null")
    except urllib.error.HTTPError as e:
        return e.code, (json.loads(e.read() or b"null") if e.headers.get_content_type() == "application/json" else None)


def login(role):
    global TOK
    s, j = call("POST", "/users/login", {"email": f"{role}@test.com", "password": PW})
    assert s == 200, f"login {role}: {j}"
    TOK = j["data"]["accessToken"]


def db_conf():
    props = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "application-local.properties")
    kv = dict(re.findall(r"^\s*(spring\.datasource\.\w+)\s*=\s*(.*?)\s*$", open(props, encoding="utf-8").read(), re.M))
    m = re.search(r"jdbc:mysql://([^:/]+)(?::(\d+))?/([^?]+)", kv["spring.datasource.url"])
    return m.group(1), m.group(2) or "3306", m.group(3), kv["spring.datasource.username"], kv["spring.datasource.password"]


ap = argparse.ArgumentParser()
ap.add_argument("--days", type=int, default=30)
ap.add_argument("--per-brand", type=int, default=25)
args = ap.parse_args()
random.seed(42)

login("CUSTOMER")
s, j = call("GET", "/brands?page=0&size=100")
brands = j["data"]["content"]
plan = []  # (orderId, date)
STATUSES = ["PAYMENTCOMPLETED", "ONDELIVERY", "DELIVERYCOMPLETED", "ORDERCOMPLETED", "ORDERCOMPLETED", "ORDERCOMPLETED", "CANCEL"]
for b in brands:
    s, j = call("GET", f"/products/brands/{b['id']}?page=0&size=100")
    products = j["data"]["content"]
    if not products:
        print(f"brand {b['name']}: 상품 없음, 건너뜀"); continue
    # 브랜드마다 규모를 다르게 (가게별로 매출 차이가 보이도록)
    weight = random.choice([0.5, 1, 1.5, 2])
    n = max(3, int(args.per_brand * weight))
    for _ in range(n):
        p = random.choice(products)
        s, j = call("POST", "/orders/single", {"productId": p["id"], "quantity": random.choice([1, 1, 1, 2, 3])})
        if s != 201:
            print(f"  주문 실패 {p['name']}: {j}"); continue
        plan.append((j["data"]["id"], date.today() - timedelta(days=random.randint(0, args.days - 1))))
    print(f"brand {b['name']}: 주문 {n}건 생성")

login("ADMIN")
for oid, _ in plan:
    call("PATCH", "/orders", {"orderId": oid, "status": random.choice(STATUSES)})

host, port, db, user, pw = db_conf()
sql = "UPDATE orders SET created_at = CASE id " + " ".join(f"WHEN {oid} THEN '{d} {random.randint(9, 22):02d}:{random.randint(0, 59):02d}:00'" for oid, d in plan) + \
      f" END WHERE id IN ({','.join(str(oid) for oid, _ in plan)});"
r = subprocess.run([MYSQL, f"-h{host}", f"-P{port}", f"-u{user}", f"-p{pw}", db, "-e", sql], capture_output=True, text=True)
if r.returncode != 0:
    sys.exit(f"MySQL 업데이트 실패 ({MYSQL}): {r.stderr.strip()}")
print(f"주문일을 최근 {args.days}일로 분산했습니다 ({len(plan)}건).")

total = 0
for d in sorted({d for _, d in plan}):
    s, j = call("POST", f"/admin/statistics/aggregate?date={d}")
    assert s == 200, f"aggregate {d}: {j}"
    total += j["rows"]
print(f"집계 완료: {len({d for _, d in plan})}일, 통계 행 {total}개. 관리 > 통계 탭에서 바로 조회됩니다.")
