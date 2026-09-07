#!/bin/bash

# 🔄 롤백 스크립트
# 이전 버전으로 빠르게 롤백

echo "🔄 긴급 롤백 시작!"
echo "=================="

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 에러 처리
set -e
trap 'echo -e "${RED}❌ 롤백 중 오류가 발생했습니다!${NC}"; exit 1' ERR

# 변수 설정
ROLLBACK_IMAGE=${1}
HEALTH_CHECK_URL="http://localhost:8080/actuator/health"

if [ -z "$ROLLBACK_IMAGE" ]; then
    echo -e "${RED}❌ 롤백할 이미지를 지정해주세요!${NC}"
    echo "사용법: $0 <rollback_image>"
    echo "예시: $0 ghcr.io/sooshinsa:v1.0.0"
    exit 1
fi

wait_for_health() {
    local max_attempts=30
    
    echo -n "   서비스 헬스체크 중"
    for i in $(seq 1 $max_attempts); do
        if curl -f $HEALTH_CHECK_URL > /dev/null 2>&1; then
            echo -e "\n${GREEN}✅ 서비스 준비 완료${NC}"
            return 0
        fi
        echo -n "."
        sleep 5
    done
    
    echo -e "\n${RED}❌ 서비스 헬스체크 실패${NC}"
    return 1
}

echo -e "${BLUE}📋 1단계: 현재 상태 확인${NC}"
echo "   롤백할 이미지: $ROLLBACK_IMAGE"

# 현재 실행 중인 이미지 확인
CURRENT_IMAGE=$(docker-compose images app | tail -1 | awk '{print $4":"$5}')
echo "   현재 실행 중인 이미지: $CURRENT_IMAGE"

if [ "$CURRENT_IMAGE" = "$ROLLBACK_IMAGE" ]; then
    echo -e "${YELLOW}⚠️  이미 해당 이미지로 실행 중입니다${NC}"
    exit 0
fi

echo -e "${BLUE}📋 2단계: 긴급 롤백 실행${NC}"
echo "   서비스 중지 중..."

# 현재 서비스 중지
docker-compose stop app

echo "   이미지 교체 중..."
# Docker Compose 파일의 이미지 태그 임시 변경
cat > docker-compose.rollback.yml << EOF
version: '3.8'
services:
  app:
    image: $ROLLBACK_IMAGE
    container_name: sooshinsa-app
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: docker,prod
    depends_on:
      - mysql
      - redis
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s
    networks:
      - sooshinsa-network

networks:
  sooshinsa-network:
    external: true
EOF

echo "   이전 버전 시작 중..."
docker-compose -f docker-compose.yml -f docker-compose.rollback.yml up -d app

echo -e "${BLUE}📋 3단계: 롤백 검증${NC}"
if ! wait_for_health; then
    echo -e "${RED}❌ 롤백 실패!${NC}"
    echo "   수동으로 확인이 필요합니다."
    exit 1
fi

# 간단한 API 테스트
echo "   API 응답 테스트 중..."
if curl -f $HEALTH_CHECK_URL > /dev/null 2>&1; then
    echo -e "${GREEN}✅ API 응답 정상${NC}"
else
    echo -e "${RED}❌ API 응답 실패${NC}"
    exit 1
fi

echo -e "${BLUE}📋 4단계: 정리${NC}"
rm -f docker-compose.rollback.yml

echo ""
echo -e "${GREEN}🎉 롤백 완료!${NC}"
echo "=================="
echo ""
echo "📊 롤백 정보:"
echo "  🔄 롤백된 이미지: $ROLLBACK_IMAGE"
echo "  📊 이전 이미지: $CURRENT_IMAGE"
echo "  🌐 서비스 URL: http://localhost:8080"
echo "  🔍 헬스체크: $HEALTH_CHECK_URL"
echo ""
echo "💡 다음 단계:"
echo "  📋 로그 확인: docker-compose logs -f app"
echo "  🔍 모니터링: http://localhost:3000 (Grafana)"
echo "  📊 메트릭 확인: http://localhost:9090 (Prometheus)"
echo ""