#!/bin/bash

# 🔄 Blue-Green 무중단 배포 스크립트
# 새로운 버전을 Blue 환경에 배포하고, 검증 후 Green으로 전환

echo "🚀 Blue-Green 무중단 배포 시작!"
echo "================================"

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 에러 처리
set -e
trap 'echo -e "${RED}❌ 배포 중 오류가 발생했습니다!${NC}"; cleanup; exit 1' ERR

# 변수 설정
IMAGE_TAG=${1:-latest}
DOCKER_REGISTRY=${DOCKER_REGISTRY:-ghcr.io}
APP_NAME="sooshinsa"
HEALTH_CHECK_URL="http://localhost:8080/actuator/health"
ROLLBACK_IMAGE=""

# 헬프 함수
cleanup() {
    echo -e "${YELLOW}🧹 정리 중...${NC}"
    # Blue 환경 정리 (실패 시)
    docker-compose -f docker-compose.yml -f docker-compose.blue.yml down --remove-orphans 2>/dev/null || true
}

rollback() {
    if [ -n "$ROLLBACK_IMAGE" ]; then
        echo -e "${YELLOW}🔄 롤백 시작...${NC}"
        echo "이전 버전으로 복원 중: $ROLLBACK_IMAGE"
        
        # Green 환경을 이전 이미지로 복원
        docker tag $ROLLBACK_IMAGE $DOCKER_REGISTRY/$APP_NAME:green
        docker-compose -f docker-compose.yml -f docker-compose.green.yml up -d app
        
        # 헬스체크
        wait_for_health "Green" $HEALTH_CHECK_URL
        
        echo -e "${GREEN}✅ 롤백 완료${NC}"
    else
        echo -e "${RED}❌ 롤백할 이미지를 찾을 수 없습니다${NC}"
    fi
}

wait_for_health() {
    local env_name=$1
    local url=$2
    local max_attempts=30
    
    echo -n "   $env_name 환경 헬스체크 중"
    for i in $(seq 1 $max_attempts); do
        if curl -f $url > /dev/null 2>&1; then
            echo -e "\n${GREEN}✅ $env_name 환경 준비 완료${NC}"
            return 0
        fi
        echo -n "."
        sleep 5
    done
    
    echo -e "\n${RED}❌ $env_name 환경 헬스체크 실패${NC}"
    return 1
}

perform_smoke_tests() {
    local base_url=$1
    
    echo -e "${BLUE}🧪 Smoke Test 실행${NC}"
    
    # 기본 헬스체크
    if ! curl -f $base_url/actuator/health > /dev/null 2>&1; then
        echo -e "${RED}❌ 헬스체크 실패${NC}"
        return 1
    fi
    
    # API 엔드포인트 테스트
    local endpoints=(
        "/actuator/info"
        "/api/health"
    )
    
    for endpoint in "${endpoints[@]}"; do
        echo "   테스트 중: $endpoint"
        if ! curl -f $base_url$endpoint > /dev/null 2>&1; then
            echo -e "${RED}❌ API 테스트 실패: $endpoint${NC}"
            return 1
        fi
    done
    
    echo -e "${GREEN}✅ 모든 Smoke Test 통과${NC}"
    return 0
}

# 1. 환경 변수 확인
echo -e "${BLUE}📋 1단계: 환경 설정 확인${NC}"
if [ ! -f .env ]; then
    echo -e "${RED}❌ .env 파일이 없습니다!${NC}"
    exit 1
fi

source .env
echo -e "${GREEN}✅ 환경 설정 로드 완료${NC}"
echo "   배포할 이미지: $DOCKER_REGISTRY/$APP_NAME:$IMAGE_TAG"

# 2. 현재 실행 중인 이미지 백업
echo -e "${BLUE}📋 2단계: 현재 실행 중인 환경 확인${NC}"
if docker-compose ps | grep -q "app.*Up"; then
    ROLLBACK_IMAGE=$(docker-compose images app | tail -1 | awk '{print $4":"$5}')
    echo "   현재 실행 중인 이미지: $ROLLBACK_IMAGE"
    echo -e "${GREEN}✅ 롤백 이미지 백업 완료${NC}"
else
    echo -e "${YELLOW}⚠️  현재 실행 중인 서비스가 없습니다${NC}"
fi

# 3. Blue 환경에 새 버전 배포
echo -e "${BLUE}📋 3단계: Blue 환경에 새 버전 배포${NC}"

# Blue 환경 설정 파일 생성
cat > docker-compose.blue.yml << EOF
version: '3.8'
services:
  app:
    image: $DOCKER_REGISTRY/$APP_NAME:$IMAGE_TAG
    container_name: sooshinsa-blue
    ports:
      - "8081:8080"  # Blue 환경은 8081 포트 사용
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
EOF

echo "   Blue 환경 컨테이너 시작 중..."
docker-compose -f docker-compose.yml -f docker-compose.blue.yml up -d app

# 4. Blue 환경 헬스체크
echo -e "${BLUE}📋 4단계: Blue 환경 검증${NC}"
if ! wait_for_health "Blue" "http://localhost:8081/actuator/health"; then
    echo -e "${RED}❌ Blue 환경 배포 실패${NC}"
    cleanup
    exit 1
fi

# 5. Smoke Test 실행
echo -e "${BLUE}📋 5단계: Smoke Test 실행${NC}"
if ! perform_smoke_tests "http://localhost:8081"; then
    echo -e "${RED}❌ Smoke Test 실패${NC}"
    cleanup
    exit 1
fi

# 6. 부하 테스트 (선택적)
echo -e "${BLUE}📋 6단계: 간단한 부하 테스트${NC}"
echo "   Blue 환경에 간단한 부하 테스트 실행 중..."

# 간단한 부하 테스트 (10초간 10개 동시 요청)
for i in {1..10}; do
    curl -s http://localhost:8081/actuator/health > /dev/null &
done
wait

echo -e "${GREEN}✅ 부하 테스트 완료${NC}"

# 7. 사용자 승인 (프로덕션에서는 자동 승인 가능)
echo -e "${YELLOW}🤔 Blue 환경 검증이 완료되었습니다.${NC}"
read -p "Green 환경으로 전환하시겠습니까? (y/N): " -n 1 -r
echo

if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}⏸️  배포가 취소되었습니다${NC}"
    cleanup
    exit 0
fi

# 8. Green 환경으로 전환
echo -e "${BLUE}📋 7단계: Green 환경으로 전환${NC}"

# Nginx 설정을 Blue에서 Green으로 전환하는 설정 파일 생성
cat > nginx/upstream.conf << EOF
upstream app_backend {
    server sooshinsa-blue:8080;  # 트래픽을 Blue 환경으로 전환
}
EOF

# Nginx 리로드
echo "   Nginx 설정 리로드 중..."
docker-compose exec nginx nginx -s reload

echo -e "${GREEN}✅ 트래픽이 새 버전(Blue)으로 전환되었습니다${NC}"

# 9. 기존 Green 환경 정리
echo -e "${BLUE}📋 8단계: 기존 환경 정리${NC}"
sleep 10  # 트래픽 안정화 대기

# 기존 Green 컨테이너 중지
if docker ps | grep -q "sooshinsa-app"; then
    echo "   기존 Green 환경 중지 중..."
    docker-compose stop app 2>/dev/null || true
    docker-compose rm -f app 2>/dev/null || true
fi

# Blue를 새로운 Green으로 변경
echo "   Blue 환경을 Green으로 재명명 중..."
docker rename sooshinsa-blue sooshinsa-app 2>/dev/null || true

# 포트 변경 (8081 -> 8080)
docker-compose -f docker-compose.yml up -d app

# Nginx 설정을 원래대로 복원
cat > nginx/upstream.conf << EOF
upstream app_backend {
    server app:8080;  # 원래 설정으로 복원
}
EOF

docker-compose exec nginx nginx -s reload

# 10. 최종 검증
echo -e "${BLUE}📋 9단계: 최종 검증${NC}"
if ! wait_for_health "Green" $HEALTH_CHECK_URL; then
    echo -e "${RED}❌ 최종 검증 실패, 롤백 시작${NC}"
    rollback
    exit 1
fi

# 11. 정리
echo -e "${BLUE}📋 10단계: 정리${NC}"
rm -f docker-compose.blue.yml

echo ""
echo -e "${GREEN}🎉 Blue-Green 무중단 배포 완료!${NC}"
echo "================================"
echo ""
echo "📊 배포 정보:"
echo "  🏷️  배포된 이미지: $DOCKER_REGISTRY/$APP_NAME:$IMAGE_TAG"
echo "  🔄 롤백 가능 이미지: $ROLLBACK_IMAGE"
echo "  🌐 서비스 URL: http://localhost:8080"
echo "  🔍 헬스체크: $HEALTH_CHECK_URL"
echo ""
echo "💡 유용한 명령어:"
echo "  📋 로그 확인: docker-compose logs -f app"
echo "  🔍 상태 확인: docker-compose ps"
echo "  🔄 롤백: ./scripts/rollback.sh $ROLLBACK_IMAGE"
echo ""