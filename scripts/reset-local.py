#!/usr/bin/env python3
"""로컬 DB(soo_shinsa_local)의 애플리케이션 데이터를 전부 비웁니다(테이블 구조와 배치 메타테이블은 유지).
사용: python3 scripts/reset-local.py --yes   →  이후 서버 재시작 없이  seed-local.py → seed-stats.py 순으로 다시 시드.
Redis 세션/블랙리스트도 함께 비웁니다(redis-cli 필요)."""
import os, re, subprocess, sys
if "--yes" not in sys.argv:
    sys.exit("모든 로컬 데이터가 삭제됩니다. 확인했으면 --yes 를 붙여 실행하세요.")
props = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "application-local.properties")
kv = dict(re.findall(r"^\s*(spring\.datasource\.\w+)\s*=\s*(.*?)\s*$", open(props, encoding="utf-8").read(), re.M))
m = re.search(r"jdbc:mysql://([^:/]+)(?::(\d+))?/([^?]+)", kv["spring.datasource.url"])
host, port, db = m.group(1), m.group(2) or "3306", m.group(3)
MYSQL = next((p for p in ["/opt/homebrew/opt/mysql@8.0/bin/mysql", "/opt/homebrew/opt/mysql-client/bin/mysql", "/usr/local/bin/mysql", "mysql"] if os.path.exists(p) or p == "mysql"), "mysql")
def q(sql):
    r = subprocess.run([MYSQL, f"-h{host}", f"-P{port}", f"-u{kv['spring.datasource.username']}", f"-p{kv['spring.datasource.password']}", db, "-N", "-e", sql], capture_output=True, text=True)
    if r.returncode: sys.exit(r.stderr.strip())
    return r.stdout.split()
tables = [t for t in q("SHOW TABLES") if not t.upper().startswith("BATCH_") and t != "statistics_seq"]
q("SET FOREIGN_KEY_CHECKS=0; " + " ".join(f"TRUNCATE TABLE `{t}`;" for t in tables) + " SET FOREIGN_KEY_CHECKS=1;")
print(f"{db}: {len(tables)}개 테이블 비움 ({', '.join(tables)})")
if subprocess.run(["redis-cli", "FLUSHDB"], capture_output=True).returncode == 0: print("redis: 세션/블랙리스트 비움")
print("다음: python3 scripts/seed-local.py && python3 scripts/seed-stats.py")
