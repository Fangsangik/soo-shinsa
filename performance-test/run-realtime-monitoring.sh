#!/bin/bash

echo "🔥 실시간 Grafana 모니터링 부하 테스트 시작!"
echo "============================================="

# 서비스 상태 확인
echo "🔍 서비스 상태 확인..."
if ! curl -f http://localhost:3000/api/health > /dev/null 2>&1; then
    echo "❌ Grafana가 실행되지 않음"
    exit 1
fi

if ! curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "❌ Spring Boot 앱이 실행되지 않음"
    exit 1
fi

echo "✅ 모든 서비스 정상!"
echo ""

# Grafana 대시보드 URL 안내
DASHBOARD_URL="http://localhost:3000/d/b7c3b226-27d5-4cc0-b34d-4763e6dc6a64"

echo "📊 실시간 모니터링 준비 완료!"
echo "============================"
echo "🌐 Grafana 대시보드: $DASHBOARD_URL"
echo "👤 사용자: admin"
echo "🔐 비밀번호: admin123"
echo ""
echo "💡 위 링크를 브라우저에서 열어서 실시간 TPS를 확인하세요!"
echo ""

# 부하 테스트 시작
echo "🚀 실시간 모니터링 부하 테스트 시작!"
echo ""
echo "📈 다음 시나리오로 진행됩니다:"
echo "   1. 100명 → 2분"
echo "   2. 200명 → 2분"  
echo "   3. 300명 → 2분"
echo "   4. 500명 → 2분 (극한 테스트)"
echo ""

read -p "🎯 실시간 모니터링 테스트를 시작하시겠습니까? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "테스트를 취소했습니다."
    echo "📊 Grafana 대시보드는 계속 사용할 수 있습니다: $DASHBOARD_URL"
    exit 0
fi

# 실시간 모니터링 테스트 실행
SCENARIOS=(100 200 300 500)

for threads in "${SCENARIOS[@]}"; do
    echo ""
    echo "🧪 ${threads}명 실시간 모니터링 테스트 시작!"
    echo "=========================================="
    echo "📊 Grafana에서 실시간 TPS 변화를 확인하세요!"
    echo "🌐 $DASHBOARD_URL"
    echo ""
    
    # JMeter 백그라운드 실행
    jmeter -n -t coupon-stock-concurrency-test.jmx \
        -JSERVER_URL=http://localhost:8080 \
        -JTHREADS=${threads} \
        -JRAMP_UP=30 \
        -JDURATION=120 \
        -JLOOPS=30 \
        -l "results/realtime-${threads}.jtl" \
        > "results/realtime-${threads}.log" 2>&1 &
    
    local jmeter_pid=$!
    
    echo "⏳ 2분 30초 진행 중... (Grafana에서 실시간 확인!)"
    
    # 30초 간격으로 진행 상황 표시
    for i in {1..5}; do
        sleep 30
        echo "   📊 ${i}번째 30초 경과... (총 ${threads}명 부하 중)"
    done
    
    # JMeter 완료 대기
    wait $jmeter_pid 2>/dev/null
    
    echo "   ✅ ${threads}명 테스트 완료!"
    
    # 간단한 결과 분석
    if [ -f "results/realtime-${threads}.jtl" ]; then
        local total=$(tail -n +2 "results/realtime-${threads}.jtl" | wc -l | tr -d ' ')
        local success=$(tail -n +2 "results/realtime-${threads}.jtl" | awk -F',' '$8=="true"' | wc -l | tr -d ' ')
        
        if [ $total -gt 0 ]; then
            local success_rate=$(echo "scale=1; $success * 100 / $total" | bc 2>/dev/null || echo "100")
            local tps=$(echo "scale=1; $success / 120" | bc 2>/dev/null || echo "0")
            
            echo "   📈 결과: TPS ${tps}, 성공률 ${success_rate}%"
            
            if [ $(echo "$success_rate >= 99.5" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
                echo "   🏆 성능: 🥇 S급 (완벽한 성능)"
            elif [ $(echo "$success_rate >= 99.0" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
                echo "   🏆 성능: 🥈 A급 (우수한 성능)"
            elif [ $(echo "$success_rate >= 95.0" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
                echo "   🏆 성능: 🥉 B급 (양호한 성능)"
            else
                echo "   🏆 성능: ⚠️ C급 (성능 한계 도달)"
            fi
        fi
    fi
    
    # 다음 테스트까지 대기 (마지막 제외)
    if [ "$threads" != "${SCENARIOS[-1]}" ]; then
        echo ""
        echo "⏳ 다음 테스트까지 30초 대기 (시스템 안정화)..."
        sleep 30
    fi
done

echo ""
echo "🎉 실시간 모니터링 부하 테스트 완료!"
echo "===================================="
echo ""
echo "📊 Grafana 대시보드에서 전체 결과를 확인하세요:"
echo "   🌐 $DASHBOARD_URL"
echo ""
echo "📈 상세 결과 파일:"
for threads in "${SCENARIOS[@]}"; do
    echo "   • ${threads}명: results/realtime-${threads}.jtl"
done
echo ""
echo "💡 Grafana는 계속 실행 중이므로 언제든지 모니터링할 수 있습니다!"
echo "   중지: docker-compose down"