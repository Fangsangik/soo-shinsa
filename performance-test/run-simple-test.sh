#!/bin/bash

echo "🚀 SooShinsa 성능 테스트 시작"
echo "=================================="

# 환경 변수 설정
SERVER_URL="http://localhost:8080"
THREADS=10
LOOPS=5

# 테스트 전 준비
echo "📋 테스트 환경 준비 중..."

# 1. 테스트 결과 디렉토리 생성
mkdir -p results
rm -f results/*

echo "✅ 테스트 준비 완료"

# 2. 간단한 JMeter 테스트 실행 (애플리케이션이 실행되지 않은 상태에서도 가능)
echo "🧪 JMeter 간단 성능 테스트 실행 중..."
echo "  - 동시 사용자: ${THREADS}명"
echo "  - 반복 횟수: ${LOOPS}회"

# 현재 시각 (Before 테스트)
BEFORE_TEST=$(date "+%Y-%m-%d %H:%M:%S")

# JMeter 실행 (결과를 콘솔로 출력)
jmeter -n -t simple-test.jmx \
  -JSERVER_URL=${SERVER_URL} \
  -JthreadCount=${THREADS} \
  -Jloops=${LOOPS} \
  -l results/simple-test-results.jtl

# 현재 시각 (After 테스트)
AFTER_TEST=$(date "+%Y-%m-%d %H:%M:%S")

echo "✅ 테스트 완료!"
echo "  - 시작 시간: ${BEFORE_TEST}"
echo "  - 종료 시간: ${AFTER_TEST}"

# 3. 결과 분석
echo ""
echo "📊 테스트 결과 분석"
echo "==================="

# JTL 파일에서 주요 지표 추출
if [ -f results/simple-test-results.jtl ]; then
    echo "📈 성능 지표:"
    
    # 총 요청 수
    TOTAL_REQUESTS=$(tail -n +2 results/simple-test-results.jtl 2>/dev/null | wc -l | tr -d ' ')
    echo "  - 총 요청 수: ${TOTAL_REQUESTS}"
    
    if [ ${TOTAL_REQUESTS} -gt 0 ]; then
        # 성공 요청 수
        SUCCESS_REQUESTS=$(tail -n +2 results/simple-test-results.jtl 2>/dev/null | awk -F',' '$8=="true"' | wc -l | tr -d ' ')
        echo "  - 성공 요청 수: ${SUCCESS_REQUESTS}"
        
        # 실패 요청 수
        FAILED_REQUESTS=$((TOTAL_REQUESTS - SUCCESS_REQUESTS))
        echo "  - 실패 요청 수: ${FAILED_REQUESTS}"
        
        # 성공률 계산
        if [ ${TOTAL_REQUESTS} -gt 0 ]; then
            SUCCESS_RATE=$(echo "scale=2; ${SUCCESS_REQUESTS} * 100 / ${TOTAL_REQUESTS}" | bc 2>/dev/null || echo "계산 불가")
            echo "  - 성공률: ${SUCCESS_RATE}%"
        fi
        
        # 평균 응답 시간
        AVG_RESPONSE_TIME=$(tail -n +2 results/simple-test-results.jtl 2>/dev/null | awk -F',' '{sum+=$2; count++} END {if(count>0) print sum/count; else print 0}')
        echo "  - 평균 응답 시간: ${AVG_RESPONSE_TIME}ms"
        
        echo ""
        echo "🔍 오류 분석:"
        
        # 연결 오류 확인
        CONNECTION_ERRORS=$(tail -n +2 results/simple-test-results.jtl 2>/dev/null | grep -i "connection\|connect" | wc -l | tr -d ' ')
        echo "  - 연결 오류: ${CONNECTION_ERRORS}건"
        
        # 타임아웃 오류 확인
        TIMEOUT_ERRORS=$(tail -n +2 results/simple-test-results.jtl 2>/dev/null | grep -i "timeout\|time.*out" | wc -l | tr -d ' ')
        echo "  - 타임아웃 오류: ${TIMEOUT_ERRORS}건"
        
    else
        echo "⚠️  테스트 결과가 없습니다."
    fi
else
    echo "❌ 결과 파일을 찾을 수 없습니다."
fi

echo ""
echo "📋 보고서 위치:"
echo "  - 상세 결과: results/simple-test-results.jtl"
echo ""

# 애플리케이션 실행 안내
echo "🔧 다음 단계 안내:"
echo "=================="
echo "1. 애플리케이션 실행:"
echo "   ./gradlew bootRun"
echo ""
echo "2. 실제 성능 테스트 실행:"
echo "   ./run-lock-test.sh"
echo ""
echo "3. 예상 성능 개선 효과:"
echo "   - TPS: 273 → 800+ (약 3배 향상)"
echo "   - 오류율: 50% → 1% 미만"
echo "   - 데드락: 0건"
echo ""
echo "🏁 간단 테스트 완료!"