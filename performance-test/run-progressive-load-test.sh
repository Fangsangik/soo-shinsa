#!/bin/bash

# 🚀 SooShinsa 점진적 부하 테스트 - TPS vs 스레드 수 분석
echo "🔥 SooShinsa 점진적 부하 테스트 시작"
echo "====================================="
echo "목표: 스레드 수별 TPS 한계점 찾기"
echo ""

# 환경 변수 설정
SERVER_URL="http://localhost:8080"
TEST_DURATION=120  # 각 단계별 2분 테스트

# 점진적 스레드 수 배열
THREAD_COUNTS=(50 100 200 300 500 750 1000 1500 2000)

# 결과 저장용 CSV 파일
RESULT_CSV="results/progressive-load-results.csv"
mkdir -p results

# CSV 헤더 생성
echo "스레드수,TPS,평균응답시간ms,P95응답시간ms,P99응답시간ms,성공률,오류수,메모리사용률,CPU사용률" > $RESULT_CSV

echo "📊 테스트 진행 상황:"
echo "=================="

# 각 스레드 수별 테스트 실행
for THREADS in "${THREAD_COUNTS[@]}"; do
    echo ""
    echo "🧪 테스트 단계: ${THREADS}명 동시 사용자"
    echo "  - 지속시간: ${TEST_DURATION}초"
    echo "  - 예상 완료시간: $(date -d "+${TEST_DURATION} seconds" "+%H:%M:%S")"
    
    # 결과 디렉토리 생성
    RESULT_DIR="results/progressive-${THREADS}-threads"
    mkdir -p "$RESULT_DIR"
    
    # JMeter 테스트 실행
    echo "  ⏳ JMeter 실행 중..."
    jmeter -n -t coupon-stock-concurrency-test.jmx \
        -JSERVER_URL=${SERVER_URL} \
        -JTHREADS=${THREADS} \
        -JRAMP_UP=30 \
        -JDURATION=${TEST_DURATION} \
        -JLOOPS=50 \
        -l "${RESULT_DIR}/results.jtl" \
        -e -o "${RESULT_DIR}/html-report" \
        > "${RESULT_DIR}/console.log" 2>&1
    
    # 결과 분석
    if [ -f "${RESULT_DIR}/results.jtl" ]; then
        echo "  📊 결과 분석 중..."
        
        # 기본 통계 계산
        TOTAL=$(tail -n +2 "${RESULT_DIR}/results.jtl" | wc -l | tr -d ' ')
        SUCCESS=$(tail -n +2 "${RESULT_DIR}/results.jtl" | awk -F',' '$8=="true"' | wc -l | tr -d ' ')
        ERRORS=$((TOTAL - SUCCESS))
        
        if [ $TOTAL -gt 0 ]; then
            SUCCESS_RATE=$(echo "scale=2; $SUCCESS * 100 / $TOTAL" | bc 2>/dev/null || echo "0")
            TPS=$(echo "scale=2; $SUCCESS / $TEST_DURATION" | bc 2>/dev/null || echo "0")
            
            # 응답시간 계산
            AVG_TIME=$(tail -n +2 "${RESULT_DIR}/results.jtl" | awk -F',' '{sum+=$2; count++} END {if(count>0) print int(sum/count); else print 0}')
            
            # P95, P99 계산 (정렬 후 백분위수)
            SORTED_TIMES=$(tail -n +2 "${RESULT_DIR}/results.jtl" | awk -F',' '{print $2}' | sort -n)
            P95_INDEX=$(echo "scale=0; $TOTAL * 0.95" | bc | cut -d. -f1)
            P99_INDEX=$(echo "scale=0; $TOTAL * 0.99" | bc | cut -d. -f1)
            P95_TIME=$(echo "$SORTED_TIMES" | sed -n "${P95_INDEX}p" | head -1)
            P99_TIME=$(echo "$SORTED_TIMES" | sed -n "${P99_INDEX}p" | head -1)
            
            # 시스템 메트릭 수집 (근사치)
            MEMORY_USAGE=$(ps -p $(cat ../app.pid) -o %mem --no-headers 2>/dev/null | tr -d ' ' || echo "0")
            CPU_USAGE=$(ps -p $(cat ../app.pid) -o %cpu --no-headers 2>/dev/null | tr -d ' ' || echo "0")
            
            # CSV에 결과 저장
            echo "${THREADS},${TPS},${AVG_TIME},${P95_TIME:-0},${P99_TIME:-0},${SUCCESS_RATE},${ERRORS},${MEMORY_USAGE},${CPU_USAGE}" >> $RESULT_CSV
            
            # 콘솔 출력
            echo "  ✅ 결과:"
            echo "    - TPS: ${TPS}"
            echo "    - 평균 응답시간: ${AVG_TIME}ms"
            echo "    - P95 응답시간: ${P95_TIME:-N/A}ms"
            echo "    - 성공률: ${SUCCESS_RATE}%"
            echo "    - 오류 수: ${ERRORS}건"
            echo "    - 메모리: ${MEMORY_USAGE}%"
            echo "    - CPU: ${CPU_USAGE}%"
            
            # 성능 저하 감지
            if [ $(echo "${SUCCESS_RATE} < 95" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
                echo "  ⚠️ 경고: 성공률 95% 미만! 한계점 근접"
            fi
            
            if [ $AVG_TIME -gt 1000 ]; then
                echo "  ⚠️ 경고: 평균 응답시간 1초 초과! 성능 저하"
            fi
            
        else
            echo "  ❌ 유효한 결과 없음"
            echo "${THREADS},0,0,0,0,0,$TOTAL,0,0" >> $RESULT_CSV
        fi
    else
        echo "  ❌ 테스트 실패"
        echo "${THREADS},0,0,0,0,0,0,0,0" >> $RESULT_CSV
    fi
    
    # 다음 테스트 전 쿨다운 (시스템 안정화)
    if [ "$THREADS" != "${THREAD_COUNTS[-1]}" ]; then
        echo "  ⏳ 시스템 안정화 대기 (30초)..."
        sleep 30
    fi
done

echo ""
echo "🏁 점진적 부하 테스트 완료!"
echo "=========================="

# 최종 분석 및 권장사항
echo ""
echo "📈 TPS vs 스레드 수 분석:"
echo "========================"

# CSV 파싱하여 최고 TPS 찾기
BEST_TPS=0
BEST_THREADS=0
BREAKING_POINT=""

while IFS=',' read -r threads tps avg_time p95 p99 success_rate errors memory cpu; do
    if [ "$threads" != "스레드수" ]; then  # 헤더 제외
        echo "스레드 ${threads}명: TPS=${tps}, 응답시간=${avg_time}ms, 성공률=${success_rate}%"
        
        # 최고 TPS 찾기
        if [ $(echo "$tps > $BEST_TPS" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
            BEST_TPS=$tps
            BEST_THREADS=$threads
        fi
        
        # 성능 저하 포인트 찾기
        if [ $(echo "$success_rate < 95" | bc -l 2>/dev/null || echo "0") -eq 1 ] && [ -z "$BREAKING_POINT" ]; then
            BREAKING_POINT=$threads
        fi
    fi
done < $RESULT_CSV

echo ""
echo "🏆 성능 분석 결과:"
echo "================"
echo "✅ 최고 TPS: ${BEST_TPS} (스레드 ${BEST_THREADS}명)"

if [ -n "$BREAKING_POINT" ]; then
    echo "⚠️ 성능 한계점: 스레드 ${BREAKING_POINT}명부터 성공률 95% 미만"
    echo "💡 권장 최대 동시 사용자: $((BREAKING_POINT - 100))명"
else
    echo "✅ 테스트 범위 내에서 성능 저하 없음"
    echo "💡 권장 최대 동시 사용자: ${BEST_THREADS}명"
fi

echo ""
echo "📊 그래프 데이터 생성 중..."

# Grafana용 간단한 데이터 요약 생성
cat > results/tps-analysis.txt << EOF
# SooShinsa TPS vs 스레드 수 분석 결과

## 최고 성능
- 최고 TPS: ${BEST_TPS}
- 최적 스레드 수: ${BEST_THREADS}명
- 성능 한계점: ${BREAKING_POINT:-"테스트 범위 내 없음"}

## 상세 결과
$(cat $RESULT_CSV)

## Grafana 대시보드
- URL: http://localhost:3000
- 계정: admin/admin
- 대시보드: SooShinsa Performance Dashboard

## 권장사항
1. 최적 동시 사용자: ${BEST_THREADS}명
2. 모니터링 알림 설정: TPS < $(echo "scale=0; $BEST_TPS * 0.8" | bc) 시 알림
3. 오토스케일링 기준: 평균 응답시간 > 500ms
EOF

echo ""
echo "📂 결과 파일:"
echo "============"
echo "- CSV 데이터: $RESULT_CSV"
echo "- 분석 요약: results/tps-analysis.txt"
echo "- 상세 결과: results/progressive-*-threads/"
echo ""
echo "🌐 Grafana 대시보드: http://localhost:3000 (admin/admin)"
echo "📊 Prometheus 메트릭: http://localhost:9090"

echo ""
echo "💡 다음 단계:"
echo "============"
echo "1. docker-compose -f docker-compose-monitoring.yml up -d"
echo "2. Grafana에서 실시간 TPS 모니터링"
echo "3. 최적 스레드 수로 실제 운영 설정"