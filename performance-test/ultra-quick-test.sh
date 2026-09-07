#!/bin/bash

# 🚀 초고속 극한 부하 테스트 - 500, 750, 1000명
SERVER_URL="http://localhost:8080"
RESULT_FILE="results/extreme-load-test.csv"

echo "🔥 초고속 극한 부하 테스트 (500, 750, 1000명)"
echo "==============================================="

# 초고속 테스트 함수
run_ultra_quick_test() {
    local threads=$1
    local duration=20  # 20초로 단축
    
    echo "🧪 ${threads}명 테스트 시작..."
    
    local result_dir="results/extreme-${threads}"
    mkdir -p "$result_dir"
    
    # JMeter 실행
    timeout 30 jmeter -n -t coupon-stock-concurrency-test.jmx \
        -JSERVER_URL=${SERVER_URL} \
        -JTHREADS=${threads} \
        -JRAMP_UP=5 \
        -JDURATION=${duration} \
        -JLOOPS=10 \
        -l "${result_dir}/results.jtl" \
        > "${result_dir}/console.log" 2>&1
    
    # 결과 분석
    if [ -f "${result_dir}/results.jtl" ]; then
        local total=$(tail -n +2 "${result_dir}/results.jtl" | wc -l | tr -d ' ')
        local success=$(tail -n +2 "${result_dir}/results.jtl" | awk -F',' '$8=="true"' | wc -l | tr -d ' ')
        
        if [ $total -gt 0 ]; then
            local success_rate=$(echo "scale=1; $success * 100 / $total" | bc 2>/dev/null || echo "0")
            local tps=$(echo "scale=1; $success / $duration" | bc 2>/dev/null || echo "0")
            local avg_time=$(tail -n +2 "${result_dir}/results.jtl" | awk -F',' '{sum+=$2; count++} END {if(count>0) print int(sum/count); else print 0}')
            local max_time=$(tail -n +2 "${result_dir}/results.jtl" | awk -F',' 'BEGIN{max=0} {if($2>max) max=$2} END {print int(max)}')
            local p95_time=$(tail -n +2 "${result_dir}/results.jtl" | awk -F',' '{print $2}' | sort -n | awk 'BEGIN{count=0} {values[count++]=$1} END {p95_index=int(count*0.95); print int(values[p95_index])}')
            local errors=$((total - success))
            
            local system_status="정상"
            if [ $(echo "$success_rate < 95" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
                system_status="한계도달"
            elif [ $(echo "$success_rate < 99" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
                system_status="성능저하"
            fi
            
            # 결과 저장
            local current_time=$(date '+%H:%M:%S')
            echo "${current_time},${threads},${tps},${avg_time},${success_rate},${errors},${p95_time},${max_time},${system_status}" >> $RESULT_FILE
            
            echo "✅ ${threads}명: TPS ${tps}, 성공률 ${success_rate}%, 평균 ${avg_time}ms, 상태: ${system_status}"
        else
            echo "❌ ${threads}명: 테스트 실패"
            echo "$(date '+%H:%M:%S'),${threads},0,0,0,0,0,0,테스트실패" >> $RESULT_FILE
        fi
    else
        echo "❌ ${threads}명: 결과 파일 없음"
        echo "$(date '+%H:%M:%S'),${threads},0,0,0,0,0,0,결과없음" >> $RESULT_FILE
    fi
    
    sleep 5
}

# 테스트 실행
for threads in 500 750 1000; do
    run_ultra_quick_test $threads
done

echo ""
echo "🎉 극한 부하 테스트 완료! 결과 요약:"
echo "===================================="

if [ -f "$RESULT_FILE" ]; then
    printf "┌──────────┬────────┬─────────┬─────────────┬─────────────┐\n"
    printf "│ 동시사용자│   TPS  │ 성공률  │ 평균응답시간│   시스템    │\n"
    printf "│   (명)   │        │   (%)   │    (ms)     │    상태     │\n"
    printf "├──────────┼────────┼─────────┼─────────────┼─────────────┤\n"
    
    tail -n +2 "$RESULT_FILE" | while IFS=',' read -r time threads tps avg_time success_rate errors p95 max_time status; do
        if [ $(echo "$success_rate >= 99.5" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
            grade="🥇"
        elif [ $(echo "$success_rate >= 99.0" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
            grade="🥈"  
        elif [ $(echo "$success_rate >= 95.0" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
            grade="🥉"
        else
            grade="⚠️"
        fi
        
        printf "│%8s  │%6s  │%7s %s│%11s  │%11s  │\n" \
            "$threads" "$tps" "$success_rate" "$grade" "$avg_time" "$status"
    done
    
    printf "└──────────┴────────┴─────────┴─────────────┴─────────────┘\n"
fi

echo ""
echo "📂 상세 결과: $RESULT_FILE"