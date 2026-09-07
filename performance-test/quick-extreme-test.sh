#!/bin/bash

# 🚀 빠른 극한 부하 테스트 - 300~1000명
clear

SERVER_URL="http://localhost:8080"
RESULT_FILE="results/extreme-load-test.csv"
mkdir -p results

echo "🔥 SooShinsa 빠른 극한 부하 테스트"
echo "=================================="
echo "목표: 300-1000 동시 사용자 한계점 찾기"
echo ""

# CSV 헤더
echo "시간,스레드수,TPS,평균응답시간ms,성공률,오류수,P95응답시간ms,최대응답시간ms,시스템상태" > $RESULT_FILE

# 빠른 극한 테스트 함수
run_quick_extreme_test() {
    local threads=$1
    local duration=45  # 45초로 단축
    
    echo "🧪 ${threads}명 극한 부하 테스트 시작! (${duration}초)"
    echo "========================================"
    
    local result_dir="results/extreme-${threads}"
    mkdir -p "$result_dir"
    
    # JMeter 실행
    jmeter -n -t coupon-stock-concurrency-test.jmx \
        -JSERVER_URL=${SERVER_URL} \
        -JTHREADS=${threads} \
        -JRAMP_UP=10 \
        -JDURATION=${duration} \
        -JLOOPS=20 \
        -l "${result_dir}/results.jtl" \
        > "${result_dir}/console.log" 2>&1 &
    
    local jmeter_pid=$!
    local start_time=$(date +%s)
    
    # 진행 상황 모니터링
    while kill -0 $jmeter_pid 2>/dev/null; do
        local elapsed=$(($(date +%s) - start_time))
        local remaining=$((duration - elapsed))
        
        if [ $remaining -gt 0 ]; then
            echo -ne "\r📊 진행 중... ${elapsed}초 경과, ${remaining}초 남음     "
        else
            echo -ne "\r📊 분석 중... 결과 처리 중     "
        fi
        
        sleep 2
    done
    
    wait $jmeter_pid 2>/dev/null
    echo ""
    
    # 상세 결과 분석
    if [ -f "${result_dir}/results.jtl" ]; then
        local total=$(tail -n +2 "${result_dir}/results.jtl" | wc -l | tr -d ' ')
        local success=$(tail -n +2 "${result_dir}/results.jtl" | awk -F',' '$8=="true"' | wc -l | tr -d ' ')
        local errors=$((total - success))
        
        if [ $total -gt 0 ]; then
            local success_rate=$(echo "scale=1; $success * 100 / $total" | bc 2>/dev/null || echo "0")
            local tps=$(echo "scale=1; $success / $duration" | bc 2>/dev/null || echo "0")
            local avg_time=$(tail -n +2 "${result_dir}/results.jtl" | awk -F',' '{sum+=$2; count++} END {if(count>0) print int(sum/count); else print 0}')
            local max_time=$(tail -n +2 "${result_dir}/results.jtl" | awk -F',' 'BEGIN{max=0} {if($2>max) max=$2} END {print int(max)}')
            local p95_time=$(tail -n +2 "${result_dir}/results.jtl" | awk -F',' '{print $2}' | sort -n | awk 'BEGIN{count=0} {values[count++]=$1} END {p95_index=int(count*0.95); print int(values[p95_index])}')
            
            # 시스템 상태 판정
            local system_status="정상"
            if [ $(echo "$success_rate < 95" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
                system_status="한계도달"
            elif [ $(echo "$success_rate < 99" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
                system_status="성능저하"
            fi
            
            # 결과 저장
            local current_time=$(date '+%H:%M:%S')
            echo "${current_time},${threads},${tps},${avg_time},${success_rate},${errors},${p95_time},${max_time},${system_status}" >> $RESULT_FILE
            
            # 결과 출력
            echo "🎯 ${threads}명 결과: TPS ${tps}, 성공률 ${success_rate}%, 평균 ${avg_time}ms"
            
            # 성능 등급
            if [ $(echo "$success_rate >= 99.5" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
                echo "🏆 성능: 🥇 S급 (완벽)"
            elif [ $(echo "$success_rate >= 99.0" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
                echo "🏆 성능: 🥈 A급 (우수)"
            elif [ $(echo "$success_rate >= 95.0" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
                echo "🏆 성능: 🥉 B급 (양호)"
            else
                echo "🏆 성능: ⚠️ C급 (개선필요)"
                echo "🚨 단일 서버 한계점 도달!"
            fi
        else
            echo "❌ 유효한 결과 없음 - 시스템 과부하"
            echo "$(date '+%H:%M:%S'),${threads},0,0,0,${total},0,0,시스템과부하" >> $RESULT_FILE
        fi
    else
        echo "❌ 테스트 실패"
        echo "$(date '+%H:%M:%S'),${threads},0,0,0,0,0,0,테스트실패" >> $RESULT_FILE
    fi
    
    echo ""
    echo "⏳ 시스템 안정화 대기 (10초)..."
    sleep 10
}

# 애플리케이션 상태 확인
echo "🔍 애플리케이션 상태 확인..."
if ! curl -f ${SERVER_URL}/actuator/health > /dev/null 2>&1; then
    echo "❌ 애플리케이션이 실행되지 않습니다"
    exit 1
fi
echo "✅ 애플리케이션 정상!"
echo ""

# 극한 테스트 실행
THREAD_COUNTS=(300 500 750 1000)

for threads in "${THREAD_COUNTS[@]}"; do
    run_quick_extreme_test $threads
done

# 최종 결과 표시
echo ""
echo "🎉 극한 부하 테스트 완료!"
echo "========================="
echo ""

echo "📊 최종 결과 요약:"
echo "=================="

printf "┌──────────┬────────┬─────────────┬─────────┬────────┬─────────────┬─────────────┐\n"
printf "│ 동시사용자│   TPS  │ 평균응답시간│ 성공률  │ 오류수 │ P95응답시간 │   시스템    │\n"
printf "│   (명)   │        │    (ms)     │   (%)   │  (건)  │    (ms)     │    상태     │\n"
printf "├──────────┼────────┼─────────────┼─────────┼────────┼─────────────┼─────────────┤\n"

if [ -f "$RESULT_FILE" ]; then
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
        
        printf "│%8s  │%6s  │%11s  │%7s %s│%6s  │%11s  │%11s  │\n" \
            "$threads" "$tps" "$avg_time" "$success_rate" "$grade" "$errors" "$p95" "$status"
    done
fi

printf "└──────────┴────────┴─────────────┴─────────┴────────┴─────────────┴─────────────┘\n"

echo ""
echo "🎯 단일 서버 한계점 분석:"
echo "========================"

if [ -f "$RESULT_FILE" ]; then
    BEST_TPS=0
    BEST_THREADS=0
    BREAKING_POINT=""
    
    while IFS=',' read -r time threads tps avg_time success_rate errors p95 max_time status; do
        if [ $(echo "$tps > $BEST_TPS" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
            BEST_TPS=$tps
            BEST_THREADS=$threads
        fi
        
        if [ $(echo "$success_rate < 95" | bc -l 2>/dev/null || echo "0") -eq 1 ] && [ -z "$BREAKING_POINT" ]; then
            BREAKING_POINT=$threads
        fi
    done < <(tail -n +2 "$RESULT_FILE")
    
    echo "🏆 최고 성능: TPS ${BEST_TPS} (${BEST_THREADS}명)"
    
    if [ -n "$BREAKING_POINT" ]; then
        echo "⚠️ 성능 한계점: ${BREAKING_POINT}명부터 성공률 95% 미만"
        echo "💡 권장 운영 한계: $((BREAKING_POINT - 50))명"
    else
        echo "✅ 1000명까지 안정적! 단일 서버 성능 우수"
    fi
fi

echo ""
echo "📂 상세 결과: $RESULT_FILE"
echo "💡 다음 단계: 분산 환경 구축 검토"