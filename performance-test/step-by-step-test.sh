#!/bin/bash

# 🚀 단계적 부하 테스트 - 300명부터 차근차근
clear

SERVER_URL="http://localhost:8080"
RESULT_FILE="results/step-by-step-test.csv"
mkdir -p results

echo "🔥 SooShinsa 단계적 부하 테스트"
echo "=============================="
echo "목표: 진짜 한계점 찾기 (300 → 350 → 400 → 450 → 500)"
echo ""

# CSV 헤더
echo "시간,스레드수,TPS,평균응답시간ms,성공률,오류수,P95응답시간ms,최대응답시간ms" > $RESULT_FILE

# 단계적 테스트 함수
run_step_test() {
    local threads=$1
    local duration=30  # 짧게 30초
    
    echo "🧪 ${threads}명 테스트 시작... (${duration}초)"
    
    local result_dir="results/step-${threads}"
    mkdir -p "$result_dir"
    
    # JMeter 실행 (더 간단한 설정)
    timeout 60 jmeter -n -t simple-test.jmx \
        -JSERVER_URL=${SERVER_URL} \
        -JTHREADS=${threads} \
        -JRAMP_UP=10 \
        -JDURATION=${duration} \
        -JLOOPS=10 \
        -l "${result_dir}/results.jtl" \
        > "${result_dir}/console.log" 2>&1
    
    local jmeter_exit_code=$?
    
    # 결과 분석
    if [ $jmeter_exit_code -eq 0 ] && [ -f "${result_dir}/results.jtl" ]; then
        local total=$(tail -n +2 "${result_dir}/results.jtl" | wc -l | tr -d ' ')
        local success=$(tail -n +2 "${result_dir}/results.jtl" | awk -F',' '$8=="true"' | wc -l | tr -d ' ')
        local errors=$((total - success))
        
        if [ $total -gt 0 ]; then
            local success_rate=$(echo "scale=1; $success * 100 / $total" | bc 2>/dev/null || echo "0")
            local tps=$(echo "scale=1; $success / $duration" | bc 2>/dev/null || echo "0")
            local avg_time=$(tail -n +2 "${result_dir}/results.jtl" | awk -F',' '{sum+=$2; count++} END {if(count>0) print int(sum/count); else print 0}')
            local max_time=$(tail -n +2 "${result_dir}/results.jtl" | awk -F',' 'BEGIN{max=0} {if($2>max) max=$2} END {print int(max)}')
            local p95_time=$(tail -n +2 "${result_dir}/results.jtl" | awk -F',' '{print $2}' | sort -n | awk 'BEGIN{count=0} {values[count++]=$1} END {p95_index=int(count*0.95); print int(values[p95_index])}')
            
            # 결과 저장
            local current_time=$(date '+%H:%M:%S')
            echo "${current_time},${threads},${tps},${avg_time},${success_rate},${errors},${p95_time},${max_time}" >> $RESULT_FILE
            
            echo "✅ ${threads}명: TPS ${tps}, 성공률 ${success_rate}%, 평균 ${avg_time}ms"
            
            # 성공률 체크
            if [ $(echo "$success_rate < 95" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
                echo "🚨 한계점 감지! 성공률 ${success_rate}%"
                return 1  # 한계점 도달
            fi
        else
            echo "❌ ${threads}명: 유효 결과 없음"
            echo "$(date '+%H:%M:%S'),${threads},0,0,0,${total},0,0" >> $RESULT_FILE
            return 1
        fi
    else
        echo "❌ ${threads}명: 테스트 실패 (종료코드: ${jmeter_exit_code})"
        echo "$(date '+%H:%M:%S'),${threads},0,0,0,0,0,0" >> $RESULT_FILE
        return 1
    fi
    
    sleep 10  # 짧은 대기
    return 0
}

# 애플리케이션 상태 확인
echo "🔍 애플리케이션 상태 확인..."
if ! curl -f ${SERVER_URL}/actuator/health > /dev/null 2>&1; then
    echo "❌ 애플리케이션이 실행되지 않습니다"
    exit 1
fi
echo "✅ 애플리케이션 정상!"
echo ""

# 단계적 테스트 실행
THREAD_COUNTS=(300 350 400 450 500 550 600)

for threads in "${THREAD_COUNTS[@]}"; do
    if ! run_step_test $threads; then
        echo ""
        echo "🛑 ${threads}명에서 한계점 도달! 테스트 중단"
        break
    fi
done

# 최종 결과
echo ""
echo "🎉 단계적 부하 테스트 완료!"
echo "=========================="

if [ -f "$RESULT_FILE" ]; then
    echo ""
    echo "📊 단계별 결과:"
    printf "%-8s %-8s %-8s %-12s %-8s\n" "스레드" "TPS" "성공률%" "응답시간ms" "오류수"
    printf "%-8s %-8s %-8s %-12s %-8s\n" "----" "---" "-----" "--------" "----"
    
    tail -n +2 "$RESULT_FILE" | while IFS=',' read -r time threads tps avg_time success_rate errors p95 max_time; do
        if [ $(echo "$success_rate >= 99" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
            status="✅"
        elif [ $(echo "$success_rate >= 95" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
            status="⚠️"
        else
            status="❌"
        fi
        printf "%-8s %-8s %-8s %-12s %-8s %s\n" "$threads" "$tps" "$success_rate" "$avg_time" "$errors" "$status"
    done
fi

echo ""
echo "📂 상세 결과: $RESULT_FILE"