#!/bin/bash

# 🔍 헬스체크 및 모니터링 스크립트
# 서비스 상태를 확인하고 문제 발생 시 알림

echo "🔍 SooShinsa 헬스체크 시작!"
echo "=========================="

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 변수 설정
APP_URL="http://localhost:8080"
GRAFANA_URL="http://localhost:3000"
PROMETHEUS_URL="http://localhost:9090"
ALERT_THRESHOLD_CPU=80
ALERT_THRESHOLD_MEMORY=80
ALERT_THRESHOLD_RESPONSE_TIME=3000

# 헬스체크 결과 저장
HEALTH_STATUS="healthy"
ISSUES=()

check_service() {
    local service_name=$1
    local url=$2
    local max_attempts=${3:-3}
    
    echo -n "   $service_name 확인 중"
    
    for i in $(seq 1 $max_attempts); do
        if curl -f $url > /dev/null 2>&1; then
            echo -e " ${GREEN}✅${NC}"
            return 0
        fi
        echo -n "."
        sleep 2
    done
    
    echo -e " ${RED}❌${NC}"
    HEALTH_STATUS="unhealthy"
    ISSUES+=("$service_name 서비스가 응답하지 않습니다")
    return 1
}

check_response_time() {
    local url=$1
    local threshold=$2
    
    echo -n "   응답시간 확인 중"
    
    local response_time=$(curl -o /dev/null -s -w '%{time_total}' $url 2>/dev/null || echo "999")
    local response_ms=$(echo "$response_time * 1000" | bc -l 2>/dev/null || echo "9999")
    
    if (( $(echo "$response_ms < $threshold" | bc -l 2>/dev/null || echo "0") )); then
        echo -e " ${GREEN}✅ ${response_ms%.*}ms${NC}"
        return 0
    else
        echo -e " ${RED}❌ ${response_ms%.*}ms (>${threshold}ms)${NC}"
        HEALTH_STATUS="unhealthy"
        ISSUES+=("응답시간이 임계값을 초과했습니다: ${response_ms%.*}ms")
        return 1
    fi
}

check_docker_containers() {
    echo -e "${BLUE}📋 Docker 컨테이너 상태 확인${NC}"
    
    local containers=("sooshinsa-app" "sooshinsa-mysql" "sooshinsa-redis" "sooshinsa-prometheus" "sooshinsa-grafana")
    
    for container in "${containers[@]}"; do
        echo -n "   $container 상태 확인 중"
        
        if docker ps --format "table {{.Names}}" | grep -q "^$container"; then
            local status=$(docker inspect --format='{{.State.Health.Status}}' $container 2>/dev/null || echo "unknown")
            case $status in
                "healthy")
                    echo -e " ${GREEN}✅ 정상${NC}"
                    ;;
                "unhealthy")
                    echo -e " ${RED}❌ 비정상${NC}"
                    HEALTH_STATUS="unhealthy"
                    ISSUES+=("$container 컨테이너가 비정상 상태입니다")
                    ;;
                "starting")
                    echo -e " ${YELLOW}⏳ 시작 중${NC}"
                    ;;
                *)
                    echo -e " ${GREEN}✅ 실행 중${NC}"
                    ;;
            esac
        else
            echo -e " ${RED}❌ 중지됨${NC}"
            HEALTH_STATUS="unhealthy"
            ISSUES+=("$container 컨테이너가 실행되지 않고 있습니다")
        fi
    done
}

check_resource_usage() {
    echo -e "${BLUE}📋 리소스 사용량 확인${NC}"
    
    # CPU 사용률 확인
    if command -v docker &> /dev/null; then
        local app_cpu=$(docker stats sooshinsa-app --no-stream --format "{{.CPUPerc}}" 2>/dev/null | sed 's/%//' || echo "0")
        echo -n "   애플리케이션 CPU 사용률"
        
        if (( $(echo "$app_cpu > $ALERT_THRESHOLD_CPU" | bc -l 2>/dev/null || echo "0") )); then
            echo -e " ${RED}❌ ${app_cpu}%${NC}"
            HEALTH_STATUS="unhealthy"
            ISSUES+=("CPU 사용률이 높습니다: ${app_cpu}%")
        else
            echo -e " ${GREEN}✅ ${app_cpu}%${NC}"
        fi
        
        # 메모리 사용률 확인
        local app_mem=$(docker stats sooshinsa-app --no-stream --format "{{.MemPerc}}" 2>/dev/null | sed 's/%//' || echo "0")
        echo -n "   애플리케이션 메모리 사용률"
        
        if (( $(echo "$app_mem > $ALERT_THRESHOLD_MEMORY" | bc -l 2>/dev/null || echo "0") )); then
            echo -e " ${RED}❌ ${app_mem}%${NC}"
            HEALTH_STATUS="unhealthy"
            ISSUES+=("메모리 사용률이 높습니다: ${app_mem}%")
        else
            echo -e " ${GREEN}✅ ${app_mem}%${NC}"
        fi
    fi
}

check_database_connection() {
    echo -e "${BLUE}📋 데이터베이스 연결 확인${NC}"
    
    echo -n "   MySQL 연결 확인 중"
    if docker-compose exec -T mysql mysqladmin ping -h localhost --silent 2>/dev/null; then
        echo -e " ${GREEN}✅${NC}"
    else
        echo -e " ${RED}❌${NC}"
        HEALTH_STATUS="unhealthy"
        ISSUES+=("MySQL 데이터베이스에 연결할 수 없습니다")
    fi
    
    echo -n "   Redis 연결 확인 중"
    if docker-compose exec -T redis redis-cli ping 2>/dev/null | grep -q PONG; then
        echo -e " ${GREEN}✅${NC}"
    else
        echo -e " ${RED}❌${NC}"
        HEALTH_STATUS="unhealthy"
        ISSUES+=("Redis 캐시에 연결할 수 없습니다")
    fi
}

check_api_endpoints() {
    echo -e "${BLUE}📋 API 엔드포인트 확인${NC}"
    
    local endpoints=(
        "$APP_URL/actuator/health:헬스체크"
        "$APP_URL/actuator/info:애플리케이션 정보"
        "$APP_URL/api/health:API 상태"
    )
    
    for endpoint_info in "${endpoints[@]}"; do
        local url="${endpoint_info%%:*}"
        local name="${endpoint_info##*:}"
        
        echo -n "   $name 확인 중"
        
        local http_code=$(curl -s -o /dev/null -w "%{http_code}" $url 2>/dev/null || echo "000")
        
        if [ "$http_code" = "200" ]; then
            echo -e " ${GREEN}✅${NC}"
        else
            echo -e " ${RED}❌ (HTTP $http_code)${NC}"
            HEALTH_STATUS="unhealthy"
            ISSUES+=("$name API가 정상적으로 응답하지 않습니다 (HTTP $http_code)")
        fi
    done
}

generate_report() {
    echo ""
    echo "🔍 헬스체크 리포트"
    echo "=================="
    echo "검사 시간: $(date '+%Y-%m-%d %H:%M:%S')"
    echo ""
    
    if [ "$HEALTH_STATUS" = "healthy" ]; then
        echo -e "${GREEN}🎉 전체 시스템 상태: 정상${NC}"
        echo ""
        echo "✅ 모든 서비스가 정상적으로 작동 중입니다"
    else
        echo -e "${RED}⚠️  전체 시스템 상태: 문제 발견${NC}"
        echo ""
        echo "🚨 발견된 문제:"
        for issue in "${ISSUES[@]}"; do
            echo "   • $issue"
        done
        echo ""
        echo "💡 권장 조치:"
        echo "   1. 로그 확인: docker-compose logs -f app"
        echo "   2. 컨테이너 재시작: docker-compose restart"
        echo "   3. 시스템 리소스 확인: docker stats"
        echo "   4. 필요시 긴급 롤백: ./scripts/rollback.sh <이전_이미지>"
    fi
    
    echo ""
    echo "📊 접속 정보:"
    echo "   🌐 애플리케이션: $APP_URL"
    echo "   📈 Grafana: $GRAFANA_URL"
    echo "   📊 Prometheus: $PROMETHEUS_URL"
    echo ""
}

# 메인 헬스체크 실행
echo -e "${BLUE}📋 서비스 응답 확인${NC}"
check_service "애플리케이션" "$APP_URL/actuator/health"
check_service "Grafana" "$GRAFANA_URL/api/health"
check_service "Prometheus" "$PROMETHEUS_URL/-/ready"

echo -e "${BLUE}📋 응답시간 확인${NC}"
check_response_time "$APP_URL/actuator/health" $ALERT_THRESHOLD_RESPONSE_TIME

check_docker_containers
check_resource_usage
check_database_connection
check_api_endpoints

# 리포트 생성
generate_report

# 종료 코드 설정
if [ "$HEALTH_STATUS" = "healthy" ]; then
    exit 0
else
    exit 1
fi