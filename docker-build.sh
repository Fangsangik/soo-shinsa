#!/bin/bash

# 🐳 SooShinsa Docker 빌드 및 실행 스크립트

echo "🚀 SooShinsa Docker 환경 구축 시작!"
echo "=================================="

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 에러 처리
set -e
trap 'echo -e "${RED}❌ 빌드 중 오류가 발생했습니다!${NC}"; exit 1' ERR

# 1. 환경 변수 확인
echo -e "${BLUE}📋 1단계: 환경 변수 확인${NC}"
if [ ! -f .env ]; then
    echo -e "${RED}❌ .env 파일이 없습니다!${NC}"
    echo "   .env.example을 복사하여 .env 파일을 생성하세요."
    exit 1
fi

source .env
echo -e "${GREEN}✅ 환경 변수 로드 완료${NC}"

# 2. Docker 상태 확인
echo -e "${BLUE}📋 2단계: Docker 상태 확인${NC}"
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}❌ Docker가 실행되지 않았습니다!${NC}"
    echo "   Docker Desktop을 실행하세요."
    exit 1
fi
echo -e "${GREEN}✅ Docker 실행 중${NC}"

# 3. 기존 컨테이너 정리 (선택사항)
read -p "🤔 기존 컨테이너를 삭제하고 새로 시작하시겠습니까? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}🧹 기존 컨테이너 정리 중...${NC}"
    docker-compose down -v 2>/dev/null || true
    docker system prune -f
    echo -e "${GREEN}✅ 정리 완료${NC}"
fi

# 4. 애플리케이션 빌드
echo -e "${BLUE}📋 3단계: 애플리케이션 빌드${NC}"
echo "   Gradle 빌드 실행 중..."
./gradlew build -x test --no-daemon
echo -e "${GREEN}✅ 애플리케이션 빌드 완료${NC}"

# 5. Docker 이미지 빌드
echo -e "${BLUE}📋 4단계: Docker 이미지 빌드${NC}"
echo "   Docker 이미지 빌드 중... (시간이 소요될 수 있습니다)"
docker-compose build --no-cache app
echo -e "${GREEN}✅ Docker 이미지 빌드 완료${NC}"

# 6. 인프라 서비스 시작 (MySQL, Redis)
echo -e "${BLUE}📋 5단계: 인프라 서비스 시작${NC}"
echo "   데이터베이스 및 캐시 서비스 시작 중..."
docker-compose up -d mysql redis
echo "   서비스 준비 대기 중..."

# MySQL 준비 대기
echo -n "   MySQL 준비 중"
for i in {1..30}; do
    if docker-compose exec -T mysql mysqladmin ping -h localhost --silent 2>/dev/null; then
        echo -e "\n${GREEN}✅ MySQL 준비 완료${NC}"
        break
    fi
    echo -n "."
    sleep 2
done

# Redis 준비 대기
echo -n "   Redis 준비 중"
for i in {1..15}; do
    if docker-compose exec -T redis redis-cli ping 2>/dev/null | grep -q PONG; then
        echo -e "\n${GREEN}✅ Redis 준비 완료${NC}"
        break
    fi
    echo -n "."
    sleep 1
done

# 7. 애플리케이션 시작
echo -e "${BLUE}📋 6단계: 애플리케이션 시작${NC}"
echo "   Spring Boot 애플리케이션 시작 중..."
docker-compose up -d app

# 8. 애플리케이션 준비 대기
echo -n "   애플리케이션 준비 중"
for i in {1..60}; do
    if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo -e "\n${GREEN}✅ 애플리케이션 준비 완료${NC}"
        break
    fi
    echo -n "."
    sleep 2
done

# 9. 모니터링 서비스 시작
echo -e "${BLUE}📋 7단계: 모니터링 서비스 시작${NC}"
docker-compose up -d prometheus grafana nginx

# 10. 최종 상태 확인
echo -e "${BLUE}📋 8단계: 서비스 상태 확인${NC}"
sleep 10

echo ""
echo -e "${GREEN}🎉 SooShinsa Docker 환경 구축 완료!${NC}"
echo "=================================="
echo ""
echo "📊 서비스 접속 정보:"
echo "  🌐 애플리케이션: http://localhost:8080"
echo "  📈 Grafana: http://localhost:3000 (admin/admin123)"
echo "  📊 Prometheus: http://localhost:9090"
echo "  🔍 헬스체크: http://localhost:8080/actuator/health"
echo ""
echo "🐳 Docker 명령어:"
echo "  📋 로그 확인: docker-compose logs -f app"
echo "  🔍 상태 확인: docker-compose ps"
echo "  🛑 종료: docker-compose down"
echo ""
echo "💡 트러블슈팅:"
echo "  🔧 재시작: docker-compose restart app"
echo "  🧹 전체 정리: docker-compose down -v && docker system prune -f"