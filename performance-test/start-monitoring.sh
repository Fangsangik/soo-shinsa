#!/bin/bash

echo "🔥 SooShinsa 실시간 성능 모니터링 시작"
echo "====================================="

# Docker Compose 실행
echo "📊 Prometheus + Grafana 컨테이너 시작 중..."
docker-compose up -d

# 서비스 상태 확인
echo ""
echo "⏳ 서비스 시작 대기 중..."
sleep 10

# Prometheus 상태 확인
echo "🔍 Prometheus 상태 확인..."
if curl -f http://localhost:9090/-/healthy > /dev/null 2>&1; then
    echo "✅ Prometheus 실행 중: http://localhost:9090"
else
    echo "❌ Prometheus 실행 실패"
fi

# Grafana 상태 확인
echo "🔍 Grafana 상태 확인..."
if curl -f http://localhost:3000/api/health > /dev/null 2>&1; then
    echo "✅ Grafana 실행 중: http://localhost:3000"
    echo "   • 사용자: admin"
    echo "   • 비밀번호: admin123"
else
    echo "❌ Grafana 실행 실패"
fi

# Spring Boot 앱 상태 확인
echo "🔍 Spring Boot 앱 상태 확인..."
if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "✅ Spring Boot 앱 실행 중: http://localhost:8080"
    echo "   • Prometheus 메트릭: http://localhost:8080/actuator/prometheus"
else
    echo "❌ Spring Boot 앱이 실행되지 않음"
    echo "   → 먼저 Spring Boot 앱을 실행하세요"
fi

echo ""
echo "🚀 모니터링 준비 완료!"
echo "====================="
echo "📊 Grafana 대시보드: http://localhost:3000"
echo "📈 Prometheus: http://localhost:9090"
echo "🎯 Spring Boot 메트릭: http://localhost:8080/actuator/prometheus"
echo ""
echo "💡 이제 JMeter 부하 테스트를 실행하면"
echo "   Grafana에서 실시간 TPS를 확인할 수 있습니다!"
echo ""
echo "🔥 부하 테스트 실행 예시:"
echo "   ./run-live-monitoring-test.sh"