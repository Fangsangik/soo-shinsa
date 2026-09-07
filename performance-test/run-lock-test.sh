#!/bin/bash

# JMeter 락 경합 성능 테스트 실행 스크립트

echo "🚀 분산락 + DB락 성능 테스트 시작"
echo "=================================="

# 환경 변수 설정
SERVER_URL="http://localhost:8080"
TEST_DURATION=60
THREADS=100
RAMP_UP=10

# 테스트 전 준비
echo "📋 테스트 환경 준비 중..."

# 1. 테스트 결과 디렉토리 생성
mkdir -p results
rm -f results/*

# 2. 애플리케이션 상태 확인
echo "🔍 애플리케이션 상태 확인..."
curl -f ${SERVER_URL}/actuator/health > /dev/null 2>&1
if [ $? -ne 0 ]; then
    echo "❌ 애플리케이션이 실행되지 않았습니다. 먼저 애플리케이션을 시작하세요."
    exit 1
fi

echo "✅ 애플리케이션 상태 정상"

# 3. 테스트 데이터 준비 (재고 및 쿠폰 초기화)
echo "📦 테스트 데이터 준비 중..."
curl -X POST ${SERVER_URL}/api/test/reset-stock \
  -H "Content-Type: application/json" \
  -d '{"productOptionId": 1, "quantity": 1000}' > /dev/null 2>&1

curl -X POST ${SERVER_URL}/api/test/reset-coupon \
  -H "Content-Type: application/json" \
  -d '{"couponId": 1, "maxCount": 100}' > /dev/null 2>&1

# 4. JMeter 테스트 실행
echo "🧪 JMeter 성능 테스트 실행 중..."
echo "  - 동시 사용자: ${THREADS}명"
echo "  - 램프업 시간: ${RAMP_UP}초"
echo "  - 테스트 지속시간: ${TEST_DURATION}초"

# 현재 시각 (Before 테스트)
BEFORE_TEST=$(date "+%Y-%m-%d %H:%M:%S")

# JMeter 실행
jmeter -n -t lock-contention-test.jmx \
  -JSERVER_URL=${SERVER_URL} \
  -JthreadCount=${THREADS} \
  -JrampUp=${RAMP_UP} \
  -Jduration=${TEST_DURATION} \
  -l results/lock-test-results.jtl \
  -e -o results/html-report

# 현재 시각 (After 테스트)
AFTER_TEST=$(date "+%Y-%m-%d %H:%M:%S")

echo "✅ 테스트 완료!"
echo "  - 시작 시간: ${BEFORE_TEST}"
echo "  - 종료 시간: ${AFTER_TEST}"

# 5. 결과 분석
echo ""
echo "📊 테스트 결과 분석"
echo "==================="

# JTL 파일에서 주요 지표 추출
if [ -f results/lock-test-results.jtl ]; then
    echo "📈 성능 지표:"
    
    # 총 요청 수
    TOTAL_REQUESTS=$(tail -n +2 results/lock-test-results.jtl | wc -l)
    echo "  - 총 요청 수: ${TOTAL_REQUESTS}"
    
    # 성공 요청 수
    SUCCESS_REQUESTS=$(tail -n +2 results/lock-test-results.jtl | awk -F',' '$8=="true"' | wc -l)
    echo "  - 성공 요청 수: ${SUCCESS_REQUESTS}"
    
    # 실패 요청 수
    FAILED_REQUESTS=$((TOTAL_REQUESTS - SUCCESS_REQUESTS))
    echo "  - 실패 요청 수: ${FAILED_REQUESTS}"
    
    # 성공률 계산
    if [ ${TOTAL_REQUESTS} -gt 0 ]; then
        SUCCESS_RATE=$(echo "scale=2; ${SUCCESS_REQUESTS} * 100 / ${TOTAL_REQUESTS}" | bc)
        echo "  - 성공률: ${SUCCESS_RATE}%"
    fi
    
    # 평균 응답 시간
    AVG_RESPONSE_TIME=$(tail -n +2 results/lock-test-results.jtl | awk -F',' '{sum+=$2; count++} END {if(count>0) print sum/count; else print 0}')
    echo "  - 평균 응답 시간: ${AVG_RESPONSE_TIME}ms"
    
    # TPS 계산
    if [ ${TEST_DURATION} -gt 0 ]; then
        TPS=$(echo "scale=2; ${SUCCESS_REQUESTS} / ${TEST_DURATION}" | bc)
        echo "  - TPS (성공 기준): ${TPS}"
    fi
    
    echo ""
    echo "🔍 데드락 및 타임아웃 오류 확인:"
    
    # 타임아웃 오류 확인
    TIMEOUT_ERRORS=$(tail -n +2 results/lock-test-results.jtl | grep -i "timeout\|time.*out" | wc -l)
    echo "  - 타임아웃 오류: ${TIMEOUT_ERRORS}건"
    
    # 락 관련 오류 확인
    LOCK_ERRORS=$(tail -n +2 results/lock-test-results.jtl | grep -i "lock\|deadlock\|blocked" | wc -l)
    echo "  - 락 관련 오류: ${LOCK_ERRORS}건"
    
    # 데이터베이스 관련 오류 확인
    DB_ERRORS=$(tail -n +2 results/lock-test-results.jtl | grep -i "sql\|database\|connection" | wc -l)
    echo "  - 데이터베이스 오류: ${DB_ERRORS}건"
    
else
    echo "❌ 결과 파일을 찾을 수 없습니다."
fi

# 6. 데이터 정합성 확인
echo ""
echo "🔍 데이터 정합성 확인"
echo "===================="

# 최종 재고 확인
FINAL_STOCK=$(curl -s ${SERVER_URL}/api/products/options/1/stock | jq -r '.quantity' 2>/dev/null || echo "확인 불가")
echo "  - 최종 재고: ${FINAL_STOCK}"

# 최종 쿠폰 수량 확인
FINAL_COUPON=$(curl -s ${SERVER_URL}/api/coupons/1/count | jq -r '.maxCount' 2>/dev/null || echo "확인 불가")
echo "  - 최종 쿠폰 수량: ${FINAL_COUPON}"

echo ""
echo "📋 보고서 위치:"
echo "  - 상세 결과: results/lock-test-results.jtl"
echo "  - HTML 리포트: results/html-report/index.html"
echo ""
echo "🏁 성능 테스트 완료!"

# 결과가 좋지 않은 경우 경고
if [ ${SUCCESS_RATE%.*} -lt 95 ]; then
    echo ""
    echo "⚠️  경고: 성공률이 95% 미만입니다. 락 경합 문제를 확인하세요!"
fi

if [ ${LOCK_ERRORS} -gt 0 ] || [ ${TIMEOUT_ERRORS} -gt 10 ]; then
    echo "⚠️  경고: 락 관련 오류가 발견되었습니다. 데드락 문제를 확인하세요!"
fi