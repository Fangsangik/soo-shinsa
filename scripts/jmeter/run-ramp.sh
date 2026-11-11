#!/bin/bash
# 스레드 수를 단계적으로 올리며 TPS/지연/에러율을 기록. 사용: scripts/jmeter/run-ramp.sh [duration=60] [steps="25 50 100 200 400"]
cd "$(dirname "$0")/../.." || exit 1
DUR=${1:-60}; STEPS=${2:-"25 50 100 200 400"}; OUT=build/jmeter; mkdir -p $OUT
printf "%-8s %8s %8s %8s %8s %8s\n" threads TPS avg_ms p90_ms p99_ms err%
for T in $STEPS; do
  rm -f $OUT/r$T.jtl
  jmeter -n -t scripts/jmeter/soo-shinsa.jmx -Jthreads=$T -Jrampup=$((T/10+1)) -Jduration=$DUR -Jthink=${THINK:-300} -l $OUT/r$T.jtl -j $OUT/r$T.log >/dev/null 2>&1
  python3 - "$OUT/r$T.jtl" "$T" <<'PY'
import csv,sys
rows=list(csv.DictReader(open(sys.argv[1])))
rows=[r for r in rows if r['label']!='POST /users/login']   # 로그인(스레드당 1회, bcrypt)은 제외하고 측정
if not rows: print(sys.argv[2],'no data'); sys.exit()
lat=sorted(int(r['elapsed']) for r in rows); n=len(lat)
t0=min(int(r['timeStamp']) for r in rows); t1=max(int(r['timeStamp'])+int(r['elapsed']) for r in rows)
err=sum(r['success']!='true' for r in rows)
print(f"{sys.argv[2]:<8} {n/((t1-t0)/1000):8.0f} {sum(lat)/n:8.0f} {lat[int(n*.9)]:8d} {lat[int(n*.99)]:8d} {100*err/n:8.2f}")
PY
done
