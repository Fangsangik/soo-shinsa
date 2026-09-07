#!/bin/bash

# 🚀 SooShinsa 빠른 TPS vs 스레드 수 분석
echo "🔥 SooShinsa TPS vs 스레드 수 분석 시작"
echo "====================================="

SERVER_URL="http://localhost:8080"
TEST_DURATION=60  # 각 단계별 1분 테스트

# 점진적 스레드 수 (빠른 테스트)
THREAD_COUNTS=(50 100 200 500 1000)

# 결과 저장용 CSV 파일
RESULT_CSV="results/quick-tps-analysis.csv"
mkdir -p results

# CSV 헤더 생성
echo "스레드수,TPS,평균응답시간ms,성공률,오류수,최대응답시간ms" > $RESULT_CSV

echo "📊 TPS vs 스레드 수 분석 진행:"
echo "============================"

# 각 스레드 수별 테스트 실행
for THREADS in "${THREAD_COUNTS[@]}"; do
    echo ""
    echo "🧪 테스트: ${THREADS}명 동시 사용자"
    echo "  - 지속시간: ${TEST_DURATION}초"
    echo "  - 시작시간: $(date "+%H:%M:%S")"
    
    RESULT_DIR="results/tps-${THREADS}-threads"
    mkdir -p "$RESULT_DIR"
    
    # JMeter 테스트 실행
    echo "  ⏳ JMeter 실행 중..."
    jmeter -n -t coupon-stock-concurrency-test.jmx \
        -JSERVER_URL=${SERVER_URL} \
        -JTHREADS=${THREADS} \
        -JRAMP_UP=10 \
        -JDURATION=${TEST_DURATION} \
        -JLOOPS=20 \
        -l "${RESULT_DIR}/results.jtl" \
        > "${RESULT_DIR}/console.log" 2>&1
    
    # 결과 분석
    if [ -f "${RESULT_DIR}/results.jtl" ]; then
        echo "  📊 결과 분석 중..."
        
        TOTAL=$(tail -n +2 "${RESULT_DIR}/results.jtl" | wc -l | tr -d ' ')
        SUCCESS=$(tail -n +2 "${RESULT_DIR}/results.jtl" | awk -F',' '$8=="true"' | wc -l | tr -d ' ')
        ERRORS=$((TOTAL - SUCCESS))
        
        if [ $TOTAL -gt 0 ]; then
            SUCCESS_RATE=$(echo "scale=2; $SUCCESS * 100 / $TOTAL" | bc 2>/dev/null || echo "0")
            TPS=$(echo "scale=2; $SUCCESS / $TEST_DURATION" | bc 2>/dev/null || echo "0")
            AVG_TIME=$(tail -n +2 "${RESULT_DIR}/results.jtl" | awk -F',' '{sum+=$2; count++} END {if(count>0) print int(sum/count); else print 0}')
            MAX_TIME=$(tail -n +2 "${RESULT_DIR}/results.jtl" | awk -F',' 'BEGIN{max=0} {if($2>max) max=$2} END {print int(max)}')
            
            # CSV에 결과 저장
            echo "${THREADS},${TPS},${AVG_TIME},${SUCCESS_RATE},${ERRORS},${MAX_TIME}" >> $RESULT_CSV
            
            # 콘솔 출력
            echo "  ✅ 결과:"
            echo "    - TPS: ${TPS}"
            echo "    - 평균 응답시간: ${AVG_TIME}ms"
            echo "    - 최대 응답시간: ${MAX_TIME}ms"
            echo "    - 성공률: ${SUCCESS_RATE}%"
            echo "    - 오류 수: ${ERRORS}건"
            
            # 성능 경고
            if [ $(echo "${SUCCESS_RATE} < 95" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
                echo "    ⚠️ 경고: 성공률 95% 미만!"
            fi
            
            if [ $AVG_TIME -gt 500 ]; then
                echo "    ⚠️ 경고: 평균 응답시간 500ms 초과!"
            fi
            
        else
            echo "  ❌ 유효한 결과 없음"
            echo "${THREADS},0,0,0,$TOTAL,0" >> $RESULT_CSV
        fi
    else
        echo "  ❌ 테스트 실패"
        echo "${THREADS},0,0,0,0,0" >> $RESULT_CSV
    fi
    
    # 다음 테스트 전 쿨다운
    if [ "$THREADS" != "${THREAD_COUNTS[-1]}" ]; then
        echo "  ⏳ 시스템 쿨다운 (15초)..."
        sleep 15
    fi
done

echo ""
echo "🏁 TPS 분석 완료!"
echo "=================="

# 결과 분석 및 시각화
echo ""
echo "📈 TPS vs 스레드 수 결과:"
echo "========================"

# 최고 성능 찾기
BEST_TPS=0
BEST_THREADS=0
BREAKING_POINT=""

printf "%-10s %-10s %-15s %-10s %-10s\n" "스레드수" "TPS" "평균응답시간ms" "성공률%" "오류수"
printf "%-10s %-10s %-15s %-10s %-10s\n" "------" "---" "----------" "-----" "----"

while IFS=',' read -r threads tps avg_time success_rate errors max_time; do
    if [ "$threads" != "스레드수" ]; then  # 헤더 제외
        printf "%-10s %-10s %-15s %-10s %-10s\n" "$threads" "$tps" "$avg_time" "$success_rate" "$errors"
        
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
    RECOMMENDED=$((BREAKING_POINT - 50))
    [ $RECOMMENDED -lt 50 ] && RECOMMENDED=50
    echo "💡 권장 최대 동시 사용자: ${RECOMMENDED}명"
else
    echo "✅ 테스트 범위 내에서 성능 저하 없음"
    echo "💡 권장 최대 동시 사용자: ${BEST_THREADS}명"
fi

echo ""
echo "📊 성능 트렌드 분석:"
echo "==================="

# TPS 증가율 계산
PREV_TPS=0
PREV_THREADS=0

while IFS=',' read -r threads tps avg_time success_rate errors max_time; do
    if [ "$threads" != "스레드수" ]; then
        if [ $PREV_TPS -gt 0 ]; then
            GROWTH=$(echo "scale=1; ($tps - $PREV_TPS) / $PREV_TPS * 100" | bc 2>/dev/null || echo "0")
            THREAD_INCREASE=$((threads - PREV_THREADS))
            EFFICIENCY=$(echo "scale=2; ($tps - $PREV_TPS) / $THREAD_INCREASE" | bc 2>/dev/null || echo "0")
            
            if [ $(echo "$GROWTH < 0" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
                echo "📉 스레드 ${PREV_THREADS} → ${threads}: TPS 감소 ${GROWTH}% (효율성: ${EFFICIENCY})"
            elif [ $(echo "$GROWTH < 10" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
                echo "📊 스레드 ${PREV_THREADS} → ${threads}: TPS 증가 ${GROWTH}% (효율성: ${EFFICIENCY}) - 포화상태"
            else
                echo "📈 스레드 ${PREV_THREADS} → ${threads}: TPS 증가 ${GROWTH}% (효율성: ${EFFICIENCY}) - 선형 증가"
            fi
        fi
        PREV_TPS=$tps
        PREV_THREADS=$threads
    fi
done < $RESULT_CSV

echo ""
echo "💡 운영 권장사항:"
echo "================"
echo "1. 최적 스레드 수: ${BEST_THREADS}명"
echo "2. 오토스케일링 기준: TPS < $(echo "scale=0; $BEST_TPS * 0.8" | bc)"
echo "3. 알림 설정: 평균 응답시간 > 200ms"
echo "4. 모니터링 주기: 5초"

echo ""
echo "📂 결과 파일:"
echo "============"
echo "- CSV 데이터: $RESULT_CSV"
echo "- 상세 결과: results/tps-*-threads/"
echo ""
echo "🎯 다음 단계: 최적 스레드 수 ${BEST_THREADS}명으로 장시간 안정성 테스트"