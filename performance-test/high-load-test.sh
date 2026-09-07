#!/bin/bash

# 🚀 고부하 테스트 - 실제 한계점 찾기
clear

SERVER_URL="http://localhost:8080"
RESULT_FILE="results/high-load-test.csv"
mkdir -p results

echo "🔥 SooShinsa 고부하 테스트 - 실제 한계점 찾기"
echo "============================================="
echo ""

# CSV 헤더
echo "시간,스레드수,TPS,평균응답시간ms,성공률,오류수,P95응답시간ms" > $RESULT_FILE

# 고부하 테스트 함수
run_high_load_test() {
    local threads=$1
    local duration=60
    
    echo "🧪 ${threads}명 동시 사용자 고부하 테스트"
    echo "======================================"
    echo "⏱️ 시작: $(date '+%H:%M:%S')"
    echo "📊 목표: 성공률 99% 이상 유지 확인"
    echo ""
    
    local result_dir="results/high-load-${threads}"
    mkdir -p "$result_dir"
    
    # JMeter 실행
    jmeter -n -t coupon-stock-concurrency-test.jmx \
        -JSERVER_URL=${SERVER_URL} \
        -JTHREADS=${threads} \
        -JRAMP_UP=15 \
        -JDURATION=${duration} \
        -JLOOPS=30 \
        -l "${result_dir}/results.jtl" \
        > "${result_dir}/console.log" 2>&1
    
    echo "⏱️ 완료: $(date '+%H:%M:%S')"
    echo ""
    
    # 즉시 결과 분석
    if [ -f "${result_dir}/results.jtl" ]; then
        echo "📊 성능 분석 결과:"
        echo "=================="
        
        local total=$(tail -n +2 "${result_dir}/results.jtl" | wc -l | tr -d ' ')
        local success=$(tail -n +2 "${result_dir}/results.jtl" | awk -F',' '$8=="true"' | wc -l | tr -d ' ')
        local errors=$((total - success))
        
        if [ $total -gt 0 ]; then
            local success_rate=$(echo "scale=1; $success * 100 / $total" | bc 2>/dev/null || echo "0")
            local tps=$(echo "scale=1; $success / $duration" | bc 2>/dev/null || echo "0")
            local avg_time=$(tail -n +2 "${result_dir}/results.jtl" | awk -F',' '{sum+=$2; count++} END {if(count>0) print int(sum/count); else print 0}')
            
            # P95 계산
            local p95_time=$(tail -n +2 "${result_dir}/results.jtl" | awk -F',' '{print $2}' | sort -n | awk 'BEGIN{count=0} {values[count++]=$1} END {p95_index=int(count*0.95); print int(values[p95_index])}')
            
            # 결과 저장
            local current_time=$(date '+%H:%M:%S')
            echo "${current_time},${threads},${tps},${avg_time},${success_rate},${errors},${p95_time}" >> $RESULT_FILE
            
            # 상세 결과 출력
            echo "✅ 총 요청: ${total}건"
            echo "✅ 성공 요청: ${success}건"
            echo "❌ 실패 요청: ${errors}건"
            echo ""
            echo "📈 핵심 지표:"
            printf "   TPS: %s\n" "$tps"
            printf "   성공률: %s%%\n" "$success_rate"
            printf "   평균 응답시간: %sms\n" "$avg_time"
            printf "   P95 응답시간: %sms\n" "$p95_time"
            echo ""
            
            # 성능 판정
            echo "🏆 성능 판정:"
            if [ $(echo "$success_rate >= 99.5" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
                echo "   🥇 S급 - 완벽한 성능 (성공률 99.5% 이상)"
            elif [ $(echo "$success_rate >= 99.0" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
                echo "   🥈 A급 - 우수한 성능 (성공률 99.0-99.5%)"
            elif [ $(echo "$success_rate >= 95.0" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
                echo "   🥉 B급 - 양호한 성능 (성공률 95.0-99.0%)"
            elif [ $(echo "$success_rate >= 90.0" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
                echo "   📊 C급 - 개선 필요 (성공률 90.0-95.0%)"
            else
                echo "   ⚠️ D급 - 심각한 문제 (성공률 90% 미만)"
            fi
            
            # 경고 메시지
            if [ $(echo "$success_rate < 99.0" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
                echo ""
                echo "🚨 경고: 성공률이 99% 미만입니다!"
                echo "   프로덕션 환경에서는 위험한 수준입니다."
                echo "   즉시 최적화가 필요합니다."
            fi
            
            if [ $avg_time -gt 200 ]; then
                echo ""
                echo "⚠️ 주의: 평균 응답시간이 200ms를 초과했습니다."
                echo "   사용자 경험에 영향을 줄 수 있습니다."
            fi
            
        else
            echo "❌ 유효한 결과가 없습니다."
            echo "0,${threads},0,0,0,${total},0" >> $RESULT_FILE
        fi
    else
        echo "❌ 테스트 실패"
        echo "0,${threads},0,0,0,0,0" >> $RESULT_FILE
    fi
    
    echo ""
    echo "📂 상세 결과: ${result_dir}/"
    echo "========================================"
    echo ""
}

# 애플리케이션 상태 확인
echo "🔍 애플리케이션 상태 확인..."
if ! curl -f ${SERVER_URL}/actuator/health > /dev/null 2>&1; then
    echo "❌ 애플리케이션이 실행되지 않습니다"
    exit 1
fi
echo "✅ 애플리케이션 정상!"
echo ""

# 고부하 테스트 실행 (점진적으로 부하 증가)
THREAD_COUNTS=(100 200 300 500)

for threads in "${THREAD_COUNTS[@]}"; do
    run_high_load_test $threads
    
    # 마지막이 아니면 쿨다운
    if [ "$threads" != "${THREAD_COUNTS[-1]}" ]; then
        echo "⏳ 시스템 안정화 대기 (30초)..."
        echo "   다음 테스트: $(($(echo "${THREAD_COUNTS[@]}" | tr ' ' '\n' | grep -n "$threads" | cut -d: -f1) + 1))/${#THREAD_COUNTS[@]}"
        sleep 30
        echo ""
    fi
done

# 최종 종합 분석
echo "🎉 고부하 테스트 완료!"
echo "===================="
echo ""
echo "📊 종합 성능 분석:"
echo "=================="

printf "%-8s %-8s %-8s %-12s %-8s %-8s %-12s\n" "시간" "스레드" "TPS" "응답시간ms" "성공률%" "오류" "P95ms"
printf "%-8s %-8s %-8s %-12s %-8s %-8s %-12s\n" "----" "----" "---" "--------" "-----" "----" "-----"

tail -n +2 "$RESULT_FILE" | while IFS=',' read -r time threads tps avg_time success_rate errors p95; do
    printf "%-8s %-8s %-8s %-12s %-8s %-8s %-12s\n" "$time" "$threads" "$tps" "$avg_time" "$success_rate" "$errors" "$p95"
done

echo ""
echo "🏆 최종 분석 결과:"
echo "================="

# 최고 성능과 한계점 찾기
BEST_TPS=$(tail -n +2 "$RESULT_FILE" | awk -F',' 'BEGIN{max=0} {if($3>max) max=$3} END {print max}' 2>/dev/null || echo "0")
BEST_THREADS=$(tail -n +2 "$RESULT_FILE" | awk -F',' -v max="$BEST_TPS" '$3==max {print $2; exit}' 2>/dev/null || echo "100")

# 성능 저하 시점 찾기
BREAKING_POINT=""
tail -n +2 "$RESULT_FILE" | while IFS=',' read -r time threads tps avg_time success_rate errors p95; do
    if [ $(echo "$success_rate < 99.0" | bc -l 2>/dev/null || echo "0") -eq 1 ] && [ -z "$BREAKING_POINT" ]; then
        BREAKING_POINT=$threads
        break
    fi
done

echo "✅ 최고 TPS: $BEST_TPS (스레드 ${BEST_THREADS}명)"

if [ -n "$BREAKING_POINT" ]; then
    echo "⚠️ 성능 저하 시점: 스레드 ${BREAKING_POINT}명부터 성공률 99% 미만"
    SAFE_LIMIT=$((BREAKING_POINT - 50))
    [ $SAFE_LIMIT -lt 50 ] && SAFE_LIMIT=50
    echo "💡 권장 운영 한계: ${SAFE_LIMIT}명 (안전 여유율 포함)"
else
    echo "✅ 테스트 범위 내에서 99% 성공률 유지"
    echo "💡 운영 권장: ${BEST_THREADS}명까지 안전"
fi

echo ""
echo "📈 성능 최적화 권장사항:"
echo "======================="
echo "1. 성공률 99% 이상 유지를 최우선으로 설정"
echo "2. 평균 응답시간 200ms 이하 목표"
echo "3. 실시간 모니터링으로 이상 징후 즉시 감지"
echo "4. 오토스케일링으로 부하 분산"

echo ""
echo "📂 상세 결과 파일: $RESULT_FILE"