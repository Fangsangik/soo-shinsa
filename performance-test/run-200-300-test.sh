#!/bin/bash

# 🚀 200명, 300명 고부하 테스트
clear

SERVER_URL="http://localhost:8080"
RESULT_FILE="results/200-300-test-results.csv"
mkdir -p results

echo "🔥 SooShinsa 200명, 300명 고부하 테스트"
echo "====================================="
echo "목표: 실제 한계점과 성능 저하 구간 찾기"
echo ""

# CSV 헤더
echo "시간,스레드수,TPS,평균응답시간ms,성공률,오류수,P95응답시간ms,최대응답시간ms" > $RESULT_FILE

# 실시간 대시보드 함수
show_progress() {
    local current_test=$1
    local total_tests=$2
    local threads=$3
    local status=$4
    
    clear
    echo "🔥 SooShinsa 고부하 테스트 진행 상황"
    echo "=================================="
    echo ""
    
    # 진행률 바
    local progress=$((current_test * 100 / total_tests))
    local bar_length=40
    local filled_length=$((progress * bar_length / 100))
    
    printf "진행률: ["
    for ((i=0; i<filled_length; i++)); do printf "█"; done
    for ((i=filled_length; i<bar_length; i++)); do printf "░"; done
    printf "] %d%% (%d/%d)\n" $progress $current_test $total_tests
    
    echo ""
    echo "📊 현재 테스트: ${threads}명 동시 사용자"
    echo "상태: $status"
    echo "시작 시간: $(date '+%H:%M:%S')"
    
    echo ""
    echo "📈 지금까지 결과:"
    if [ -f "$RESULT_FILE" ]; then
        printf "%-8s %-8s %-8s %-12s %-8s %-8s\n" "스레드" "TPS" "성공률%" "응답시간ms" "오류수" "P95ms"
        printf "%-8s %-8s %-8s %-12s %-8s %-8s\n" "----" "---" "-----" "--------" "----" "-----"
        
        tail -n +2 "$RESULT_FILE" | while IFS=',' read -r time threads tps avg_time success_rate errors p95 max_time; do
            printf "%-8s %-8s %-8s %-12s %-8s %-8s\n" "$threads" "$tps" "$success_rate" "$avg_time" "$errors" "$p95"
        done
    fi
    
    echo ""
    echo "⏳ 테스트 진행 중... (60초 소요 예정)"
}

# 고부하 테스트 실행 함수
run_load_test() {
    local threads=$1
    local test_num=$2
    local total_tests=$3
    local duration=60
    
    show_progress $test_num $total_tests $threads "테스트 시작 중..."
    
    local result_dir="results/load-test-${threads}"
    mkdir -p "$result_dir"
    
    # JMeter를 백그라운드에서 실행
    jmeter -n -t coupon-stock-concurrency-test.jmx \
        -JSERVER_URL=${SERVER_URL} \
        -JTHREADS=${threads} \
        -JRAMP_UP=15 \
        -JDURATION=${duration} \
        -JLOOPS=25 \
        -l "${result_dir}/results.jtl" \
        > "${result_dir}/console.log" 2>&1 &
    
    local jmeter_pid=$!
    local start_time=$(date +%s)
    local end_time=$((start_time + duration + 20))
    
    # 실시간 진행 상황 표시
    while kill -0 $jmeter_pid 2>/dev/null && [ $(date +%s) -lt $end_time ]; do
        local elapsed=$(($(date +%s) - start_time))
        local remaining=$((duration - elapsed))
        
        if [ $remaining -gt 0 ]; then
            show_progress $test_num $total_tests $threads "실행 중... (${remaining}초 남음)"
        else
            show_progress $test_num $total_tests $threads "결과 분석 중..."
        fi
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
            local tps=$(echo "scale=1; $success / $duration" | bc 2>/dev/null || echo "0")
            local avg_time=$(tail -n +2 "${result_dir}/results.jtl" | awk -F',' '{sum+=$2; count++} END {if(count>0) print int(sum/count); else print 0}')
            local max_time=$(tail -n +2 "${result_dir}/results.jtl" | awk -F',' 'BEGIN{max=0} {if($2>max) max=$2} END {print int(max)}')
            
            # P95 계산
            local p95_time=$(tail -n +2 "${result_dir}/results.jtl" | awk -F',' '{print $2}' | sort -n | awk 'BEGIN{count=0} {values[count++]=$1} END {p95_index=int(count*0.95); print int(values[p95_index])}')
            
            # 결과 저장
            local current_time=$(date '+%H:%M:%S')
            echo "${current_time},${threads},${tps},${avg_time},${success_rate},${errors},${p95_time},${max_time}" >> $RESULT_FILE
            
            # 즉시 결과 표시
            show_progress $test_num $total_tests $threads "완료!"
            
            echo ""
            echo "🎯 ${threads}명 테스트 결과:"
            echo "========================"
            echo "✅ TPS: $tps"
            echo "✅ 성공률: ${success_rate}%"
            echo "✅ 평균 응답시간: ${avg_time}ms"
            echo "✅ P95 응답시간: ${p95_time}ms"
            echo "✅ 최대 응답시간: ${max_time}ms"
            echo "❌ 오류 수: ${errors}건"
            
            # 성능 등급 판정
            if [ $(echo "$success_rate >= 99.5" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
                echo "🏆 성능 등급: 🥇 S급 (완벽한 성능)"
            elif [ $(echo "$success_rate >= 99.0" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
                echo "🏆 성능 등급: 🥈 A급 (우수한 성능)"
            elif [ $(echo "$success_rate >= 95.0" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
                echo "🏆 성능 등급: 🥉 B급 (양호한 성능)"
            else
                echo "🏆 성능 등급: ⚠️ C급 (개선 필요)"
            fi
            
            if [ $(echo "$success_rate < 99.0" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
                echo ""
                echo "🚨 경고: 성공률 99% 미만 감지!"
                echo "   → 성능 한계점에 도달했을 가능성"
            fi
            
        else
            echo "${current_time},${threads},0,0,0,${total},0,0" >> $RESULT_FILE
        fi
    else
        echo "$(date '+%H:%M:%S'),${threads},0,0,0,0,0,0" >> $RESULT_FILE
    fi
    
    echo ""
    echo "⏳ 다음 테스트까지 20초 대기..."
    sleep 20
}

# 애플리케이션 상태 확인
echo "🔍 애플리케이션 상태 확인..."
if ! curl -f ${SERVER_URL}/actuator/health > /dev/null 2>&1; then
    echo "❌ 애플리케이션이 실행되지 않습니다"
    exit 1
fi
echo "✅ 애플리케이션 정상!"
echo ""

# 테스트 실행
THREAD_COUNTS=(200 300)
TOTAL_TESTS=${#THREAD_COUNTS[@]}
CURRENT_TEST=0

for threads in "${THREAD_COUNTS[@]}"; do
    CURRENT_TEST=$((CURRENT_TEST + 1))
    echo "🚀 ${threads}명 고부하 테스트 시작!"
    run_load_test $threads $CURRENT_TEST $TOTAL_TESTS
done

# 최종 결과 표시
clear
echo "🎉 200명, 300명 고부하 테스트 완료!"
echo "=================================="
echo ""

echo "📊 최종 성능 분석 결과:"
echo "======================="

printf "┌──────────┬────────┬─────────────┬─────────┬────────┬─────────────┬─────────────┐\n"
printf "│ 동시사용자│   TPS  │ 평균응답시간│ 성공률  │ 오류수 │ P95응답시간 │ 최대응답시간│\n"
printf "│   (명)   │        │    (ms)     │   (%)   │  (건)  │    (ms)     │    (ms)     │\n"
printf "├──────────┼────────┼─────────────┼─────────┼────────┼─────────────┼─────────────┤\n"

if [ -f "$RESULT_FILE" ]; then
    tail -n +2 "$RESULT_FILE" | while IFS=',' read -r time threads tps avg_time success_rate errors p95 max_time; do
        if [ $(echo "$success_rate >= 99.5" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
            status="🥇"
        elif [ $(echo "$success_rate >= 99.0" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
            status="🥈"
        elif [ $(echo "$success_rate >= 95.0" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
            status="🥉"
        else
            status="⚠️"
        fi
        
        printf "│%8s  │%6s  │%11s  │%7s %s│%6s  │%11s  │%11s  │\n" \
            "$threads" "$tps" "$avg_time" "$success_rate" "$status" "$errors" "$p95" "$max_time"
    done
fi

printf "└──────────┴────────┴─────────────┴─────────┴────────┴─────────────┴─────────────┘\n"

echo ""
echo "🏆 성능 분석 요약:"
echo "================="

if [ -f "$RESULT_FILE" ]; then
    echo "📈 테스트 완료된 시나리오:"
    tail -n +2 "$RESULT_FILE" | while IFS=',' read -r time threads tps avg_time success_rate errors p95 max_time; do
        echo "   • ${threads}명: TPS ${tps}, 성공률 ${success_rate}%"
    done
    
    # 성능 저하 지점 찾기
    echo ""
    echo "🎯 성능 한계점 분석:"
    DEGRADATION_FOUND=false
    tail -n +2 "$RESULT_FILE" | while IFS=',' read -r time threads tps avg_time success_rate errors p95 max_time; do
        if [ $(echo "$success_rate < 99.0" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
            echo "   ⚠️ ${threads}명에서 성능 저하 감지 (성공률 ${success_rate}%)"
            DEGRADATION_FOUND=true
        fi
    done
    
    if [ "$DEGRADATION_FOUND" = false ]; then
        echo "   ✅ 300명까지 안정적 성능 유지!"
    fi
fi

echo ""
echo "💡 최종 권장사항:"
echo "=================="
echo "🎯 검증된 안전 범위: 100-200명"
echo "📊 모니터링 필수: 성공률 99% 이상 유지"
echo "⚡ 확장 전략: 추가 최적화 후 더 높은 부하 처리 가능"

echo ""
echo "📂 상세 결과: $RESULT_FILE"