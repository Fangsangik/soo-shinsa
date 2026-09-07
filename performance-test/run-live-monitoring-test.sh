#!/bin/bash

echo "🔥 실시간 Grafana 모니터링과 함께 부하 테스트"
echo "============================================="

# 모니터링 상태 확인
echo "🔍 모니터링 서비스 상태 확인..."
if ! curl -f http://localhost:3000/api/health > /dev/null 2>&1; then
    echo "❌ Grafana가 실행되지 않음. 먼저 모니터링을 시작하세요:"
    echo "   ./start-monitoring.sh"
    exit 1
fi

if ! curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "❌ Spring Boot 앱이 실행되지 않음"
    exit 1
fi

echo "✅ 모니터링 서비스 정상!"
echo ""

# 사용자에게 Grafana 대시보드 열기 안내
echo "📊 Grafana 대시보드를 열어주세요:"
echo "   🌐 http://localhost:3000"
echo "   👤 사용자: admin"
echo "   🔐 비밀번호: admin123"
echo ""
echo "💡 대시보드에서 실시간 TPS를 확인할 수 있습니다!"
echo ""

# 부하 테스트 시나리오 선택
echo "🎯 부하 테스트 시나리오 선택:"
echo "1. 점진적 부하 테스트 (50→100→200→300명)"
echo "2. 고정 부하 테스트 (300명 5분간)"
echo "3. 극한 부하 테스트 (300→500명)"
echo ""
read -p "선택하세요 (1-3): " choice

case $choice in
    1)
        echo "🚀 점진적 부하 테스트 시작..."
        for threads in 50 100 200 300; do
            echo ""
            echo "🧪 ${threads}명 부하 테스트 시작 (2분간)"
            echo "   📊 Grafana에서 실시간 TPS 확인하세요!"
            
            jmeter -n -t coupon-stock-concurrency-test.jmx \
                -JSERVER_URL=http://localhost:8080 \
                -JTHREADS=${threads} \
                -JRAMP_UP=30 \
                -JDURATION=120 \
                -JLOOPS=20 \
                -l "results/live-${threads}.jtl" \
                > "results/live-${threads}.log" 2>&1 &
            
            echo "   ⏳ 2분 30초 대기 중..."
            sleep 150
            
            echo "   ✅ ${threads}명 테스트 완료"
        done
        ;;
    2)
        echo "🚀 고정 부하 테스트 시작 (300명 5분간)"
        echo "   📊 Grafana에서 실시간 TPS 확인하세요!"
        
        jmeter -n -t coupon-stock-concurrency-test.jmx \
            -JSERVER_URL=http://localhost:8080 \
            -JTHREADS=300 \
            -JRAMP_UP=60 \
            -JDURATION=300 \
            -JLOOPS=50 \
            -l "results/live-300-5min.jtl" \
            > "results/live-300-5min.log" 2>&1 &
        
        echo "   ⏳ 6분 대기 중..."
        sleep 360
        echo "   ✅ 고정 부하 테스트 완료"
        ;;
    3)
        echo "🚀 극한 부하 테스트 시작 (300→500명)"
        echo "   📊 Grafana에서 실시간 TPS 확인하세요!"
        
        for threads in 300 500; do
            echo ""
            echo "🧪 ${threads}명 극한 부하 테스트 시작 (3분간)"
            
            jmeter -n -t coupon-stock-concurrency-test.jmx \
                -JSERVER_URL=http://localhost:8080 \
                -JTHREADS=${threads} \
                -JRAMP_UP=30 \
                -JDURATION=180 \
                -JLOOPS=30 \
                -l "results/live-extreme-${threads}.jtl" \
                > "results/live-extreme-${threads}.log" 2>&1 &
            
            echo "   ⏳ 3분 30초 대기 중..."
            sleep 210
            
            echo "   ✅ ${threads}명 테스트 완료"
        done
        ;;
    *)
        echo "❌ 잘못된 선택입니다."
        exit 1
        ;;
esac

echo ""
echo "🎉 실시간 모니터링 부하 테스트 완료!"
echo "=================================="
echo "📊 Grafana 대시보드에서 결과를 확인하세요:"
echo "   🌐 http://localhost:3000"
echo ""
echo "📈 상세 결과:"
echo "   • JTL 파일: results/live-*.jtl"
echo "   • 로그 파일: results/live-*.log"