#!/bin/bash

# 🚀 SooShinsa 고급 동시성 성능 테스트
# 쿠폰 발급 + 재고 차감 동시성 테스트 with 다양한 시나리오

echo "🔥 SooShinsa 고급 동시성 성능 테스트 시작"
echo "============================================"

# 환경 변수 설정
SERVER_URL="http://localhost:8080"
TEST_SCENARIOS=("light" "medium" "heavy" "extreme")

# 테스트 시나리오별 설정
declare -A SCENARIO_THREADS=(
    ["light"]=50
    ["medium"]=100
    ["heavy"]=200
    ["extreme"]=500
)

declare -A SCENARIO_DURATION=(
    ["light"]=30
    ["medium"]=60
    ["heavy"]=120
    ["extreme"]=300
)

declare -A SCENARIO_RAMP_UP=(
    ["light"]=5
    ["medium"]=10
    ["heavy"]=20
    ["extreme"]=30
)

declare -A SCENARIO_LOOPS=(
    ["light"]=5
    ["medium"]=10
    ["heavy"]=15
    ["extreme"]=20
)

# 테스트 전 준비
echo "📋 테스트 환경 준비 중..."

# 1. 결과 디렉토리 초기화
mkdir -p results/{light,medium,heavy,extreme}
rm -f results/*/*.jtl results/*/*.csv results/*/*.html

# 2. 애플리케이션 상태 확인
echo "🔍 애플리케이션 상태 확인..."
for i in {1..30}; do
    if curl -f ${SERVER_URL}/actuator/health > /dev/null 2>&1; then
        echo "✅ 애플리케이션 상태 정상 (${i}/30 시도)"
        break
    else
        echo "⏳ 애플리케이션 시작 대기 중... (${i}/30 시도)"
        sleep 2
    fi
    
    if [ $i -eq 30 ]; then
        echo "❌ 애플리케이션이 응답하지 않습니다. 테스트를 중단합니다."
        exit 1
    fi
done

# 3. 데이터베이스 초기 상태 확인
echo "📊 초기 데이터 상태 체크..."

# 테스트용 더미 데이터 생성 함수
setup_test_data() {
    echo "🔧 테스트 데이터 설정 중..."
    
    # 관리자 계정으로 테스트 데이터 생성 요청
    curl -s -X POST ${SERVER_URL}/api/test/setup-data \
        -H "Content-Type: application/json" \
        -d '{
            "productOption": {
                "id": 1,
                "initialStock": 1000,
                "name": "테스트 상품 옵션"
            },
            "coupon": {
                "id": 1,
                "maxCount": 500,
                "name": "동시성 테스트 쿠폰",
                "discountRate": 10
            },
            "users": {
                "count": 100,
                "namePrefix": "loadtest"
            }
        }' > /dev/null 2>&1 || echo "⚠️ 테스트 데이터 설정 실패 (계속 진행)"
}

# 테스트 시나리오 실행 함수
run_scenario() {
    local scenario=$1
    local threads=${SCENARIO_THREADS[$scenario]}
    local duration=${SCENARIO_DURATION[$scenario]}
    local ramp_up=${SCENARIO_RAMP_UP[$scenario]}
    local loops=${SCENARIO_LOOPS[$scenario]}
    
    echo ""
    echo "🧪 시나리오 [$scenario] 실행 중..."
    echo "  - 동시 사용자: ${threads}명"
    echo "  - 램프업 시간: ${ramp_up}초"
    echo "  - 테스트 지속시간: ${duration}초"
    echo "  - 반복 횟수: ${loops}회"
    echo "  - 예상 총 요청: $((threads * loops * 3))건"
    
    # 현재 시각
    local start_time=$(date "+%Y-%m-%d %H:%M:%S")
    
    # JMeter 실행
    jmeter -n -t coupon-stock-concurrency-test.jmx \
        -JSERVER_URL=${SERVER_URL} \
        -JTHREADS=${threads} \
        -JRAMP_UP=${ramp_up} \
        -JDURATION=${duration} \
        -JLOOPS=${loops} \
        -l results/${scenario}/concurrency-test-${scenario}.jtl \
        -e -o results/${scenario}/html-report \
        > results/${scenario}/jmeter-console-${scenario}.log 2>&1
    
    local end_time=$(date "+%Y-%m-%d %H:%M:%S")
    
    echo "✅ 시나리오 [$scenario] 완료!"
    echo "  - 시작: ${start_time}"
    echo "  - 종료: ${end_time}"
    
    # 결과 분석
    analyze_results $scenario
}

# 결과 분석 함수
analyze_results() {
    local scenario=$1
    local result_file="results/${scenario}/concurrency-test-${scenario}.jtl"
    
    if [ ! -f "$result_file" ]; then
        echo "❌ 결과 파일을 찾을 수 없습니다: $result_file"
        return 1
    fi
    
    echo ""
    echo "📊 시나리오 [$scenario] 결과 분석:"
    echo "================================="
    
    # 기본 통계
    local total_requests=$(tail -n +2 "$result_file" 2>/dev/null | wc -l | tr -d ' ')
    local success_requests=$(tail -n +2 "$result_file" 2>/dev/null | awk -F',' '$8=="true"' | wc -l | tr -d ' ')
    local failed_requests=$((total_requests - success_requests))
    
    echo "📈 기본 성능 지표:"
    echo "  - 총 요청 수: ${total_requests}"
    echo "  - 성공 요청 수: ${success_requests}"
    echo "  - 실패 요청 수: ${failed_requests}"
    
    if [ ${total_requests} -gt 0 ]; then
        local success_rate=$(echo "scale=2; ${success_requests} * 100 / ${total_requests}" | bc 2>/dev/null || echo "0")
        echo "  - 성공률: ${success_rate}%"
        
        # 응답 시간 통계
        local avg_response_time=$(tail -n +2 "$result_file" 2>/dev/null | awk -F',' '{sum+=$2; count++} END {if(count>0) print sum/count; else print 0}')
        local min_response_time=$(tail -n +2 "$result_file" 2>/dev/null | awk -F',' 'BEGIN{min=99999} {if($2<min) min=$2} END {print min}')
        local max_response_time=$(tail -n +2 "$result_file" 2>/dev/null | awk -F',' 'BEGIN{max=0} {if($2>max) max=$2} END {print max}')
        
        echo "  - 평균 응답시간: ${avg_response_time}ms"
        echo "  - 최소 응답시간: ${min_response_time}ms"
        echo "  - 최대 응답시간: ${max_response_time}ms"
        
        # TPS 계산
        local duration=${SCENARIO_DURATION[$scenario]}
        if [ ${duration} -gt 0 ]; then
            local tps=$(echo "scale=2; ${success_requests} / ${duration}" | bc 2>/dev/null || echo "0")
            echo "  - TPS (성공 기준): ${tps}"
        fi
        
        # 동시성 관련 오류 분석
        echo ""
        echo "🔍 동시성 이슈 분석:"
        
        # 쿠폰 관련 오류
        local coupon_errors=$(tail -n +2 "$result_file" 2>/dev/null | grep -i "coupon\|쿠폰" | wc -l | tr -d ' ')
        echo "  - 쿠폰 관련 오류: ${coupon_errors}건"
        
        # 재고 관련 오류
        local stock_errors=$(tail -n +2 "$result_file" 2>/dev/null | grep -i "stock\|재고\|product" | wc -l | tr -d ' ')
        echo "  - 재고 관련 오류: ${stock_errors}건"
        
        # 락 관련 오류
        local lock_errors=$(tail -n +2 "$result_file" 2>/dev/null | grep -i "lock\|timeout\|deadlock" | wc -l | tr -d ' ')
        echo "  - 락/타임아웃 오류: ${lock_errors}건"
        
        # 동시성 충돌 오류
        local concurrency_errors=$(tail -n +2 "$result_file" 2>/dev/null | grep -i "conflict\|concurrent\|duplicate" | wc -l | tr -d ' ')
        echo "  - 동시성 충돌 오류: ${concurrency_errors}건"
        
        # 성능 등급 판정
        echo ""
        echo "🏆 성능 등급 판정:"
        if (( $(echo "${success_rate} >= 99.5" | bc -l) )); then
            echo "  🥇 S급: 성공률 ${success_rate}% (99.5% 이상)"
        elif (( $(echo "${success_rate} >= 99.0" | bc -l) )); then
            echo "  🥈 A급: 성공률 ${success_rate}% (99.0-99.5%)"
        elif (( $(echo "${success_rate} >= 95.0" | bc -l) )); then
            echo "  🥉 B급: 성공률 ${success_rate}% (95.0-99.0%)"
        elif (( $(echo "${success_rate} >= 90.0" | bc -l) )); then
            echo "  📊 C급: 성공률 ${success_rate}% (90.0-95.0%)"
        else
            echo "  ⚠️ D급: 성공률 ${success_rate}% (90.0% 미만) - 개선 필요"
        fi
        
    else
        echo "⚠️ 유효한 결과가 없습니다."
    fi
    
    echo ""
    echo "📂 상세 결과 위치:"
    echo "  - JTL 파일: results/${scenario}/concurrency-test-${scenario}.jtl"
    echo "  - HTML 리포트: results/${scenario}/html-report/index.html"
    echo "  - 콘솔 로그: results/${scenario}/jmeter-console-${scenario}.log"
}

# 최종 종합 분석 함수
final_analysis() {
    echo ""
    echo "🎯 최종 종합 분석 리포트"
    echo "========================"
    
    local best_scenario=""
    local best_success_rate=0
    local best_tps=0
    
    for scenario in "${TEST_SCENARIOS[@]}"; do
        local result_file="results/${scenario}/concurrency-test-${scenario}.jtl"
        
        if [ -f "$result_file" ]; then
            local total=$(tail -n +2 "$result_file" 2>/dev/null | wc -l | tr -d ' ')
            local success=$(tail -n +2 "$result_file" 2>/dev/null | awk -F',' '$8=="true"' | wc -l | tr -d ' ')
            
            if [ ${total} -gt 0 ]; then
                local success_rate=$(echo "scale=2; ${success} * 100 / ${total}" | bc 2>/dev/null || echo "0")
                local duration=${SCENARIO_DURATION[$scenario]}
                local tps=$(echo "scale=2; ${success} / ${duration}" | bc 2>/dev/null || echo "0")
                
                echo "📊 ${scenario} 시나리오: 성공률 ${success_rate}%, TPS ${tps}"
                
                # 최고 성과 시나리오 찾기
                if (( $(echo "${success_rate} > ${best_success_rate}" | bc -l) )); then
                    best_scenario=$scenario
                    best_success_rate=$success_rate
                    best_tps=$tps
                fi
            fi
        fi
    done
    
    echo ""
    echo "🏆 최고 성과 시나리오: ${best_scenario}"
    echo "  - 성공률: ${best_success_rate}%"
    echo "  - TPS: ${best_tps}"
    
    # 개선 권장사항
    echo ""
    echo "🔧 성능 개선 권장사항:"
    echo "====================="
    
    if (( $(echo "${best_success_rate} < 95" | bc -l) )); then
        echo "⚠️ 성공률이 95% 미만입니다. 다음을 확인하세요:"
        echo "  1. 분산락 타임아웃 설정 증가"
        echo "  2. 데이터베이스 커넥션 풀 크기 증가"
        echo "  3. 애플리케이션 스레드 풀 튜닝"
        echo "  4. JVM 힙 메모리 증가"
    fi
    
    if (( $(echo "${best_tps} < 500" | bc -l) )); then
        echo "⚠️ TPS가 500 미만입니다. 다음을 고려하세요:"
        echo "  1. 쿼리 최적화 (인덱스 추가)"
        echo "  2. 캐시 도입 (Redis 캐시 전략)"
        echo "  3. 비동기 처리 도입"
        echo "  4. 로드 밸런싱 구성"
    fi
    
    echo ""
    echo "✅ 달성한 개선 효과:"
    echo "  - 기존: TPS 273, 오류율 50%"
    echo "  - 현재: TPS ${best_tps}, 성공률 ${best_success_rate}%"
    echo "  - 개선율: TPS $(echo "scale=1; ${best_tps} / 273 * 100" | bc)%, 성공률 개선 $(echo "scale=1; ${best_success_rate} - 50" | bc)%p"
}

# 메인 실행 로직
main() {
    # 테스트 데이터 설정
    setup_test_data
    
    # 각 시나리오별 테스트 실행
    for scenario in "${TEST_SCENARIOS[@]}"; do
        run_scenario $scenario
        
        # 시나리오 간 쿨다운 시간
        if [ "$scenario" != "extreme" ]; then
            echo "⏳ 다음 시나리오 준비를 위한 쿨다운 (10초)..."
            sleep 10
        fi
    done
    
    # 최종 분석
    final_analysis
    
    echo ""
    echo "🏁 모든 성능 테스트 완료!"
    echo "전체 결과는 results/ 디렉토리에서 확인할 수 있습니다."
}

# 사용자 확인
echo "이 테스트는 다음과 같이 진행됩니다:"
echo "1. Light 시나리오: 50명, 30초"
echo "2. Medium 시나리오: 100명, 60초" 
echo "3. Heavy 시나리오: 200명, 120초"
echo "4. Extreme 시나리오: 500명, 300초 (총 약 8분)"
echo ""
read -p "계속 진행하시겠습니까? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    main
else
    echo "테스트를 취소했습니다."
    exit 0
fi