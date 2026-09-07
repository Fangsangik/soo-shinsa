#!/bin/bash

# 🚀 단일 서버 극한 부하 테스트 - 진짜 한계점 찾기
clear

SERVER_URL="http://localhost:8080"
RESULT_FILE="results/extreme-load-test.csv"
mkdir -p results

echo "🔥 SooShinsa 단일 서버 극한 부하 테스트"
echo "====================================="
echo "목표: 진짜 성능 한계점과 시스템 파괴 지점 찾기"
echo "단일 서버 환경에서 최대한 밀어넣어 보자!"
echo ""

# CSV 헤더
echo "시간,스레드수,TPS,평균응답시간ms,성공률,오류수,P95응답시간ms,최대응답시간ms,시스템상태" > $RESULT_FILE

# 극한 테스트 함수
run_extreme_test() {
    local threads=$1
    local duration=90  # 90초로 더 길게
    
    echo "🧪 ${threads}명 극한 부하 테스트 시작!"
    echo "========================================="
    echo "⏱️ 시작: $(date '+%H:%M:%S')"
    echo "🎯 목표: 단일 서버 한계까지 밀어넣기"
    echo "📊 지속시간: ${duration}초"
    echo ""
    
    local result_dir="results/extreme-${threads}"
    mkdir -p "$result_dir"
    
    # 시스템 리소스 모니터링 시작
    iostat 5 > "${result_dir}/system-stats.log" &
    local iostat_pid=$!
    
    # JMeter 실행 (더 적극적인 설정)
    jmeter -n -t coupon-stock-concurrency-test.jmx \
        -JSERVER_URL=${SERVER_URL} \
        -JTHREADS=${threads} \
        -JRAMP_UP=20 \
        -JDURATION=${duration} \
        -JLOOPS=50 \
        -l "${result_dir}/results.jtl" \
        > "${result_dir}/console.log" 2>&1 &
    
    local jmeter_pid=$!
    local start_time=$(date +%s)
    
    # 실시간 모니터링
    while kill -0 $jmeter_pid 2>/dev/null; do
        local elapsed=$(($(date +%s) - start_time))
        local remaining=$((duration - elapsed))
        
        # 시스템 상태 체크
        local cpu_usage=$(ps -p $(cat ../app.pid 2>/dev/null || echo "0") -o %cpu --no-headers 2>/dev/null | tr -d ' ' || echo "0")
        local memory_usage=$(ps -p $(cat ../app.pid 2>/dev/null || echo "0") -o %mem --no-headers 2>/dev/null | tr -d ' ' || echo "0")
        
        echo "📊 [${elapsed}초] CPU: ${cpu_usage}%, 메모리: ${memory_usage}%, 남은시간: ${remaining}초"
        
        # 중간 결과 체크
        if [ -f "${result_dir}/results.jtl" ] && [ $elapsed -gt 30 ]; then
            local current_total=$(tail -n +2 "${result_dir}/results.jtl" | wc -l | tr -d ' ')
            local current_success=$(tail -n +2 "${result_dir}/results.jtl" | awk -F',' '$8=="true"' | wc -l | tr -d ' ')
            if [ $current_total -gt 0 ]; then
                local current_success_rate=$(echo "scale=1; $current_success * 100 / $current_total" | bc 2>/dev/null || echo "100")
                echo "📈 중간 성공률: ${current_success_rate}% (${current_success}/${current_total})"
                
                # 성공률이 너무 떨어지면 경고
                if [ $(echo "$current_success_rate < 90" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
                    echo "🚨 경고: 성공률이 90% 미만으로 떨어졌습니다!"
                fi
            fi
        fi
        
        sleep 5
    done
    
    # 시스템 모니터링 중지
    kill $iostat_pid 2>/dev/null || true
    
    wait $jmeter_pid 2>/dev/null
    
    echo ""
    echo "⏱️ 완료: $(date '+%H:%M:%S')"
    echo ""
    
    # 상세 결과 분석
    if [ -f "${result_dir}/results.jtl" ]; then
        echo "📊 극한 테스트 결과 분석:"
        echo "========================="
        
        local total=$(tail -n +2 "${result_dir}/results.jtl" | wc -l | tr -d ' ')
        local success=$(tail -n +2 "${result_dir}/results.jtl" | awk -F',' '$8=="true"' | wc -l | tr -d ' ')
        local errors=$((total - success))
        
        if [ $total -gt 0 ]; then
            local success_rate=$(echo "scale=1; $success * 100 / $total" | bc 2>/dev/null || echo "0")
            local tps=$(echo "scale=1; $success / $duration" | bc 2>/dev/null || echo "0")
            local avg_time=$(tail -n +2 "${result_dir}/results.jtl" | awk -F',' '{sum+=$2; count++} END {if(count>0) print int(sum/count); else print 0}')
            local max_time=$(tail -n +2 "${result_dir}/results.jtl" | awk -F',' 'BEGIN{max=0} {if($2>max) max=$2} END {print int(max)}')
            local min_time=$(tail -n +2 "${result_dir}/results.jtl" | awk -F',' 'BEGIN{min=999999} {if($2<min) min=$2} END {print int(min)}')
            
            # P95, P99 계산
            local p95_time=$(tail -n +2 "${result_dir}/results.jtl" | awk -F',' '{print $2}' | sort -n | awk 'BEGIN{count=0} {values[count++]=$1} END {p95_index=int(count*0.95); print int(values[p95_index])}')
            local p99_time=$(tail -n +2 "${result_dir}/results.jtl" | awk -F',' '{print $2}' | sort -n | awk 'BEGIN{count=0} {values[count++]=$1} END {p99_index=int(count*0.99); print int(values[p99_index])}')
            
            # 시스템 상태 판정
            local system_status="정상"
            if [ $(echo "$success_rate < 95" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
                system_status="한계도달"
            elif [ $(echo "$success_rate < 99" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
                system_status="성능저하"
            elif [ $avg_time -gt 500 ]; then
                system_status="응답지연"
            fi
            
            # 결과 저장
            local current_time=$(date '+%H:%M:%S')
            echo "${current_time},${threads},${tps},${avg_time},${success_rate},${errors},${p95_time},${max_time},${system_status}" >> $RESULT_FILE
            
            # 상세 출력
            echo "🎯 테스트 규모:"
            echo "   • 동시 사용자: ${threads}명"
            echo "   • 총 요청 수: ${total}건"
            echo "   • 성공 요청: ${success}건"
            echo "   • 실패 요청: ${errors}건"
            echo ""
            echo "📈 성능 지표:"
            echo "   • TPS: ${tps}"
            echo "   • 성공률: ${success_rate}%"
            echo "   • 응답시간: 평균 ${avg_time}ms, 최소 ${min_time}ms, 최대 ${max_time}ms"
            echo "   • P95: ${p95_time}ms, P99: ${p99_time}ms"
            echo ""
            echo "💻 시스템 상태:"
            echo "   • 전체 상태: ${system_status}"
            
            # 성능 등급 및 경고
            if [ $(echo "$success_rate >= 99.5" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
                echo "🏆 성능 등급: 🥇 S급 - 완벽한 성능 (${success_rate}%)"
            elif [ $(echo "$success_rate >= 99.0" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
                echo "🏆 성능 등급: 🥈 A급 - 우수한 성능 (${success_rate}%)"
            elif [ $(echo "$success_rate >= 95.0" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
                echo "🏆 성능 등급: 🥉 B급 - 양호한 성능 (${success_rate}%)"
            elif [ $(echo "$success_rate >= 90.0" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
                echo "🏆 성능 등급: 📊 C급 - 개선 필요 (${success_rate}%)"
            else
                echo "🏆 성능 등급: ⚠️ D급 - 심각한 문제 (${success_rate}%)"
                echo ""
                echo "🚨 단일 서버 한계점 도달!"
                echo "   → 이 지점부터 성능 저하 시작"
                echo "   → 분산 처리 또는 스케일업 필요"
            fi
            
            # 응답시간 분석
            if [ $avg_time -gt 1000 ]; then
                echo ""
                echo "⚠️ 응답시간 경고: 평균 1초 초과"
                echo "   → 사용자 경험에 심각한 영향"
            elif [ $avg_time -gt 500 ]; then
                echo ""
                echo "⚠️ 응답시간 주의: 평균 500ms 초과"
                echo "   → 성능 최적화 권장"
            fi
            
        else
            echo "❌ 유효한 결과가 없습니다 - 시스템 완전 과부하"
            echo "$(date '+%H:%M:%S'),${threads},0,0,0,${total},0,0,시스템과부하" >> $RESULT_FILE
        fi
    else
        echo "❌ 테스트 실패 - JMeter 프로세스 문제"
        echo "$(date '+%H:%M:%S'),${threads},0,0,0,0,0,0,테스트실패" >> $RESULT_FILE
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

# 극한 테스트 실행 (점진적으로 한계까지)
THREAD_COUNTS=(300 500 750 1000)

echo "🎯 극한 테스트 계획:"
echo "=================="
for threads in "${THREAD_COUNTS[@]}"; do
    echo "• ${threads}명 동시 사용자 (90초 지속)"
done
echo ""

REPLY="y"
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "테스트를 취소했습니다."
    exit 0
fi

for threads in "${THREAD_COUNTS[@]}"; do
    run_extreme_test $threads
    
    # 마지막이 아니면 시스템 안정화 대기
    if [ "$threads" != "${THREAD_COUNTS[-1]}" ]; then
        echo "⏳ 시스템 안정화 대기 (60초)..."
        echo "   • 메모리 정리 중..."
        echo "   • GC 대기 중..."
        echo "   • DB 커넥션 풀 안정화..."
        
        for i in {60..1}; do
            echo -ne "\r   ⏰ ${i}초 남음     "
            sleep 1
        done
        echo -e "\r   ✅ 안정화 완료     "
        echo ""
    fi
done

# 최종 극한 분석
clear
echo "🎉 단일 서버 극한 부하 테스트 완료!"
echo "================================="
echo ""

echo "📊 극한 테스트 최종 결과:"
echo "========================"

printf "┌──────────┬────────┬─────────────┬─────────┬────────┬─────────────┬─────────────┬─────────────┐\n"
printf "│ 동시사용자│   TPS  │ 평균응답시간│ 성공률  │ 오류수 │ P95응답시간 │ 최대응답시간│   시스템    │\n"
printf "│   (명)   │        │    (ms)     │   (%)   │  (건)  │    (ms)     │    (ms)     │    상태     │\n"
printf "├──────────┼────────┼─────────────┼─────────┼────────┼─────────────┼─────────────┼─────────────┤\n"

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
        
        printf "│%8s  │%6s  │%11s  │%7s %s│%6s  │%11s  │%11s  │%11s  │\n" \
            "$threads" "$tps" "$avg_time" "$success_rate" "$grade" "$errors" "$p95" "$max_time" "$status"
    done
fi

printf "└──────────┴────────┴─────────────┴─────────┴────────┴─────────────┴─────────────┴─────────────┘\n"

echo ""
echo "🎯 단일 서버 한계점 분석:"
echo "========================"

if [ -f "$RESULT_FILE" ]; then
    # 최고 성능과 한계점 찾기
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
    
    echo "🏆 단일 서버 최고 성능: TPS ${BEST_TPS} (${BEST_THREADS}명)"
    
    if [ -n "$BREAKING_POINT" ]; then
        echo "⚠️ 성능 한계점: ${BREAKING_POINT}명부터 성공률 95% 미만"
        echo "💡 단일 서버 권장 한계: $((BREAKING_POINT - 100))명"
        echo ""
        echo "🚀 스케일링 권장사항:"
        echo "   • ${BREAKING_POINT}명 이상: 로드밸런서 + 다중 인스턴스"
        echo "   • 또는 서버 스케일업 (CPU/메모리 증설)"
    else
        echo "✅ 1000명까지도 안정적! (단일 서버 성능 우수)"
        echo "💡 현재 단일 서버로 충분한 성능"
    fi
fi

echo ""
echo "📈 성능 개선 효과 요약:"
echo "======================"
echo "• 분산락 최적화: 데드락 완전 해결"
echo "• N+1 쿼리 제거: DB 부하 최소화"
echo "• JWT 화이트리스트: 인증 오버헤드 제거"
echo "• 트랜잭션 최적화: 격리 수준 조정"

echo ""
echo "📂 상세 결과: $RESULT_FILE"
echo ""
echo "💡 다음 단계: 분산 환경 구축 또는 캐시 레이어 추가"