#!/bin/bash

# 🚀 실시간 TPS 모니터링 대시보드
clear

SERVER_URL="http://localhost:8080"
THREADS_TO_TEST=(50 100 200 300 500)
CURRENT_TEST=0
TOTAL_TESTS=${#THREADS_TO_TEST[@]}

# 결과 저장 파일
LIVE_RESULTS="results/live-tps-monitor.csv"
mkdir -p results
echo "시간,스레드수,TPS,평균응답시간ms,성공률,오류수,P95응답시간ms" > $LIVE_RESULTS

# 실시간 모니터링 함수
show_dashboard() {
    clear
    echo "🔥 SooShinsa 실시간 TPS 모니터링 대시보드"
    echo "=========================================="
    echo "📊 $(date '+%Y-%m-%d %H:%M:%S') 현재 상황"
    echo ""
    
    # 진행 상황 표시
    local progress=$((CURRENT_TEST * 100 / TOTAL_TESTS))
    local bar_length=50
    local filled_length=$((progress * bar_length / 100))
    
    printf "진행률: ["
    for ((i=0; i<filled_length; i++)); do printf "█"; done
    for ((i=filled_length; i<bar_length; i++)); do printf "░"; done
    printf "] %d%% (%d/%d)\n" $progress $CURRENT_TEST $TOTAL_TESTS
    
    echo ""
    echo "📈 실시간 TPS 결과:"
    echo "=================="
    printf "%-8s %-10s %-10s %-15s %-10s %-10s\n" "시간" "스레드수" "TPS" "평균응답ms" "성공률%" "오류수"
    printf "%-8s %-10s %-10s %-15s %-10s %-10s\n" "----" "----" "---" "--------" "-----" "----"
    
    if [ -f "$LIVE_RESULTS" ]; then
        tail -n +2 "$LIVE_RESULTS" | tail -10 | while IFS=',' read -r time threads tps avg_time success_rate errors p95; do
            printf "%-8s %-10s %-10s %-15s %-10s %-10s\n" "$time" "$threads" "$tps" "$avg_time" "$success_rate" "$errors"
        done
    fi
    
    echo ""
    echo "📊 현재 최고 성능:"
    if [ -f "$LIVE_RESULTS" ]; then
        local best_tps=$(tail -n +2 "$LIVE_RESULTS" | awk -F',' 'BEGIN{max=0} {if($3>max) max=$3} END {print max}')
        local best_threads=$(tail -n +2 "$LIVE_RESULTS" | awk -F',' -v max="$best_tps" '$3==max {print $2; exit}')
        echo "🏆 최고 TPS: $best_tps (스레드 ${best_threads}명)"
    fi
    
    echo ""
    echo "⏳ 테스트 진행 중... (Ctrl+C로 중단)"
}

# JMeter 테스트 실행 함수
run_test() {
    local threads=$1
    local test_duration=60
    
    echo "🧪 ${threads}명 동시 사용자 테스트 시작..."
    
    local result_dir="results/live-${threads}-threads"
    mkdir -p "$result_dir"
    
    # JMeter를 백그라운드에서 실행
    jmeter -n -t coupon-stock-concurrency-test.jmx \
        -JSERVER_URL=${SERVER_URL} \
        -JTHREADS=${threads} \
        -JRAMP_UP=10 \
        -JDURATION=${test_duration} \
        -JLOOPS=30 \
        -l "${result_dir}/results.jtl" \
        > "${result_dir}/console.log" 2>&1 &
    
    local jmeter_pid=$!
    local start_time=$(date +%s)
    local test_end_time=$((start_time + test_duration + 15)) # 15초 여유
    
    # 테스트 진행 중 실시간 모니터링
    while kill -0 $jmeter_pid 2>/dev/null && [ $(date +%s) -lt $test_end_time ]; do
        show_dashboard
        sleep 3
    done
    
    # JMeter 완료 대기
    wait $jmeter_pid 2>/dev/null
    
    # 결과 분석
    if [ -f "${result_dir}/results.jtl" ]; then
        local total=$(tail -n +2 "${result_dir}/results.jtl" | wc -l | tr -d ' ')
        local success=$(tail -n +2 "${result_dir}/results.jtl" | awk -F',' '$8=="true"' | wc -l | tr -d ' ')
        local errors=$((total - success))
        
        if [ $total -gt 0 ]; then
            local success_rate=$(echo "scale=1; $success * 100 / $total" | bc 2>/dev/null || echo "0")
            local tps=$(echo "scale=1; $success / $test_duration" | bc 2>/dev/null || echo "0")
            local avg_time=$(tail -n +2 "${result_dir}/results.jtl" | awk -F',' '{sum+=$2; count++} END {if(count>0) print int(sum/count); else print 0}')
            
            # P95 계산
            local p95_time=$(tail -n +2 "${result_dir}/results.jtl" | awk -F',' '{print $2}' | sort -n | awk 'BEGIN{count=0} {values[count++]=$1} END {p95_index=int(count*0.95); print int(values[p95_index])}')
            
            # 결과를 CSV에 저장
            local current_time=$(date '+%H:%M')
            echo "${current_time},${threads},${tps},${avg_time},${success_rate},${errors},${p95_time}" >> $LIVE_RESULTS
        fi
    fi
    
    CURRENT_TEST=$((CURRENT_TEST + 1))
}

# 메인 실행
echo "🚀 실시간 TPS vs 스레드 수 모니터링 시작!"
echo "============================================"
echo ""
echo "애플리케이션 상태 확인 중..."

# 애플리케이션 상태 확인
if ! curl -f ${SERVER_URL}/actuator/health > /dev/null 2>&1; then
    echo "❌ 애플리케이션이 실행되지 않고 있습니다."
    echo "먼저 애플리케이션을 시작해주세요: ./gradlew bootRun"
    exit 1
fi

echo "✅ 애플리케이션 정상 동작 확인"
echo ""
echo "📊 테스트 시작: $(date '+%Y-%m-%d %H:%M:%S')"
echo "테스트 예상 소요시간: $((TOTAL_TESTS * 80 / 60))분"
echo ""

# 각 스레드 수별 테스트 실행
for threads in "${THREADS_TO_TEST[@]}"; do
    run_test $threads
    
    # 마지막 테스트가 아니면 쿨다운
    if [ $CURRENT_TEST -lt $TOTAL_TESTS ]; then
        echo ""
        echo "⏳ 다음 테스트 준비 중... (20초 쿨다운)"
        for i in {20..1}; do
            echo -ne "\r⏳ 다음 테스트까지 ${i}초     "
            sleep 1
        done
        echo ""
    fi
done

# 최종 결과 표시
clear
echo "🎉 실시간 TPS 모니터링 완료!"
echo "==========================="
echo ""
echo "📊 최종 결과 요약:"
echo "=================="

printf "%-10s %-10s %-15s %-10s %-10s %-15s\n" "스레드수" "TPS" "평균응답시간ms" "성공률%" "오류수" "P95응답시간ms"
printf "%-10s %-10s %-15s %-10s %-10s %-15s\n" "------" "---" "----------" "-----" "----" "----------"

tail -n +2 "$LIVE_RESULTS" | while IFS=',' read -r time threads tps avg_time success_rate errors p95; do
    printf "%-10s %-10s %-15s %-10s %-10s %-15s\n" "$threads" "$tps" "$avg_time" "$success_rate" "$errors" "$p95"
done

echo ""
echo "🏆 성능 분석 결과:"
echo "================"

# 최고 TPS와 권장 스레드 수 계산
BEST_TPS=$(tail -n +2 "$LIVE_RESULTS" | awk -F',' 'BEGIN{max=0} {if($3>max) max=$3} END {print max}')
BEST_THREADS=$(tail -n +2 "$LIVE_RESULTS" | awk -F',' -v max="$BEST_TPS" '$3==max {print $2; exit}')

echo "✅ 최고 TPS: $BEST_TPS (스레드 ${BEST_THREADS}명)"
echo "💡 권장 운영 스레드 수: ${BEST_THREADS}명"
echo ""
echo "📂 상세 결과: $LIVE_RESULTS"
echo "📊 HTML 리포트: results/live-*-threads/html-report/"

# 차트 생성을 위한 Python 스크립트 제안
echo ""
echo "📈 TPS 차트 생성을 위해 다음을 실행하세요:"
echo "python3 -c \"
import matplotlib.pyplot as plt
import pandas as pd
df = pd.read_csv('$LIVE_RESULTS')
plt.figure(figsize=(12, 8))
plt.subplot(2, 2, 1)
plt.plot(df['스레드수'], df['TPS'], 'bo-')
plt.title('TPS vs 스레드 수')
plt.xlabel('동시 사용자 수')
plt.ylabel('TPS')
plt.grid(True)
plt.subplot(2, 2, 2)
plt.plot(df['스레드수'], df['평균응답시간ms'], 'ro-')
plt.title('응답시간 vs 스레드 수')
plt.xlabel('동시 사용자 수')
plt.ylabel('평균 응답시간 (ms)')
plt.grid(True)
plt.subplot(2, 2, 3)
plt.plot(df['스레드수'], df['성공률'], 'go-')
plt.title('성공률 vs 스레드 수')
plt.xlabel('동시 사용자 수')
plt.ylabel('성공률 (%)')
plt.grid(True)
plt.subplot(2, 2, 4)
plt.bar(df['스레드수'], df['오류수'], color='red', alpha=0.7)
plt.title('오류 수 vs 스레드 수')
plt.xlabel('동시 사용자 수')
plt.ylabel('오류 수')
plt.grid(True)
plt.tight_layout()
plt.savefig('results/tps-analysis-chart.png', dpi=300, bbox_inches='tight')
plt.show()
print('차트가 results/tps-analysis-chart.png에 저장되었습니다!')
\""