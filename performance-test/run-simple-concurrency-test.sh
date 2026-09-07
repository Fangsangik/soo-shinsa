#!/bin/bash

# 🚀 SooShinsa 간단한 동시성 성능 테스트
echo "🔥 SooShinsa 동시성 성능 테스트 시작"
echo "=================================="

# 환경 변수 설정
SERVER_URL="http://localhost:8080"

# 1. 애플리케이션 상태 확인
echo "🔍 애플리케이션 상태 확인..."
curl -f ${SERVER_URL}/actuator/health > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "✅ 애플리케이션 상태 정상"
else
    echo "❌ 애플리케이션이 응답하지 않습니다."
    exit 1
fi

# 2. 결과 디렉토리 생성
mkdir -p results/simple-test

# 3. JMeter 테스트 실행
echo ""
echo "🧪 JMeter 동시성 테스트 실행 중..."
echo "  - 동시 사용자: 100명"
echo "  - 램프업 시간: 10초"
echo "  - 테스트 지속시간: 60초"
echo "  - 반복 횟수: 10회"
echo ""

# JMeter 실행
jmeter -n -t coupon-stock-concurrency-test.jmx \
    -JSERVER_URL=${SERVER_URL} \
    -JTHREADS=100 \
    -JRAMP_UP=10 \
    -JDURATION=60 \
    -JLOOPS=10 \
    -l results/simple-test/concurrency-test-results.jtl \
    -e -o results/simple-test/html-report \
    > results/simple-test/jmeter-console.log 2>&1

echo "✅ JMeter 테스트 완료!"

# 4. 결과 분석
echo ""
echo "📊 테스트 결과 분석:"
echo "===================="

RESULT_FILE="results/simple-test/concurrency-test-results.jtl"

if [ -f "$RESULT_FILE" ]; then
    # 기본 통계
    TOTAL_REQUESTS=$(tail -n +2 "$RESULT_FILE" 2>/dev/null | wc -l | tr -d ' ')
    SUCCESS_REQUESTS=$(tail -n +2 "$RESULT_FILE" 2>/dev/null | awk -F',' '$8=="true"' | wc -l | tr -d ' ')
    FAILED_REQUESTS=$((TOTAL_REQUESTS - SUCCESS_REQUESTS))
    
    echo "📈 기본 성능 지표:"
    echo "  - 총 요청 수: ${TOTAL_REQUESTS}"
    echo "  - 성공 요청 수: ${SUCCESS_REQUESTS}"
    echo "  - 실패 요청 수: ${FAILED_REQUESTS}"
    
    if [ ${TOTAL_REQUESTS} -gt 0 ]; then
        SUCCESS_RATE=$(echo "scale=2; ${SUCCESS_REQUESTS} * 100 / ${TOTAL_REQUESTS}" | bc 2>/dev/null || echo "0")
        echo "  - 성공률: ${SUCCESS_RATE}%"
        
        # 응답 시간 통계 (간단화)
        AVG_TIME=$(tail -n +2 "$RESULT_FILE" 2>/dev/null | awk -F',' '{sum+=$2; count++} END {if(count>0) print int(sum/count); else print 0}')
        echo "  - 평균 응답시간: ${AVG_TIME}ms"
        
        # TPS 계산
        TPS=$(echo "scale=2; ${SUCCESS_REQUESTS} / 60" | bc 2>/dev/null || echo "0")
        echo "  - TPS (성공 기준): ${TPS}"
        
        # 동시성 이슈 분석
        echo ""
        echo "🔍 동시성 이슈 분석:"
        COUPON_ERRORS=$(tail -n +2 "$RESULT_FILE" 2>/dev/null | grep -c -i "coupon\|쿠폰" || echo "0")
        STOCK_ERRORS=$(tail -n +2 "$RESULT_FILE" 2>/dev/null | grep -c -i "stock\|재고\|product" || echo "0")
        echo "  - 쿠폰 관련 오류: ${COUPON_ERRORS}건"
        echo "  - 재고 관련 오류: ${STOCK_ERRORS}건"
        
        # 성능 등급 판정
        echo ""
        echo "🏆 성능 등급 판정:"
        if [ $(echo "${SUCCESS_RATE} >= 99.5" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
            echo "  🥇 S급: 성공률 ${SUCCESS_RATE}% (99.5% 이상)"
        elif [ $(echo "${SUCCESS_RATE} >= 99.0" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
            echo "  🥈 A급: 성공률 ${SUCCESS_RATE}% (99.0-99.5%)"
        elif [ $(echo "${SUCCESS_RATE} >= 95.0" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
            echo "  🥉 B급: 성공률 ${SUCCESS_RATE}% (95.0-99.0%)"
        else
            echo "  📊 개선 필요: 성공률 ${SUCCESS_RATE}% (95.0% 미만)"
        fi
        
        # 목표 달성 여부
        echo ""
        echo "🎯 목표 달성 분석:"
        if [ $(echo "${SUCCESS_RATE} >= 99.0" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
            echo "  ✅ 0% 오류율 목표 달성 (성공률 ${SUCCESS_RATE}%)"
        else
            echo "  ⚠️ 0% 오류율 목표 미달성 (성공률 ${SUCCESS_RATE}%)"
        fi
        
        if [ $(echo "${TPS} >= 100" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
            echo "  ✅ 동시성 처리 성능 양호 (TPS ${TPS})"
        else
            echo "  ⚠️ 동시성 처리 성능 개선 필요 (TPS ${TPS})"
        fi
        
    else
        echo "⚠️ 유효한 결과가 없습니다."
    fi
else
    echo "❌ 결과 파일을 찾을 수 없습니다: $RESULT_FILE"
fi

echo ""
echo "📂 상세 결과 위치:"
echo "  - JTL 파일: results/simple-test/concurrency-test-results.jtl"
echo "  - HTML 리포트: results/simple-test/html-report/index.html"
echo "  - 콘솔 로그: results/simple-test/jmeter-console.log"
echo ""
echo "🏁 동시성 테스트 완료!"