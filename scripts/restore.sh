#!/bin/bash

# 🔄 SooShinsa 복원 스크립트
# 백업된 데이터를 안전하게 복원

echo "🔄 SooShinsa 복원 시작!"
echo "======================"

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 에러 처리
set -e
trap 'echo -e "${RED}❌ 복원 중 오류가 발생했습니다!${NC}"; cleanup; exit 1' ERR

# 변수 설정
BACKUP_DATE=${1}
RESTORE_TYPE=${2:-full}  # full, db, files, config
BACKUP_BASE_DIR="/backup/sooshinsa"
BACKUP_DIR="$BACKUP_BASE_DIR/$BACKUP_DATE"

# S3 설정 (선택사항)
S3_BUCKET=${S3_BACKUP_BUCKET:-""}
AWS_REGION=${AWS_REGION:-"ap-northeast-2"}

cleanup() {
    echo -e "${YELLOW}🧹 임시 파일 정리 중...${NC}"
    rm -rf /tmp/sooshinsa_restore_* 2>/dev/null || true
}

validate_backup() {
    echo -e "${BLUE}🔍 백업 파일 검증${NC}"
    
    if [ -z "$BACKUP_DATE" ]; then
        echo -e "${RED}❌ 백업 날짜를 지정해주세요!${NC}"
        echo "사용법: $0 <backup_date> [restore_type]"
        echo "예시: $0 20241201_143000 full"
        exit 1
    fi
    
    # 로컬 백업 확인
    if [ ! -d "$BACKUP_DIR" ]; then
        echo -e "${YELLOW}⚠️  로컬 백업을 찾을 수 없습니다: $BACKUP_DIR${NC}"
        
        # S3에서 다운로드 시도
        if [ -n "$S3_BUCKET" ] && command -v aws &> /dev/null; then
            echo "   S3에서 백업 다운로드 시도 중..."
            download_from_s3
        else
            echo -e "${RED}❌ 복원할 백업을 찾을 수 없습니다${NC}"
            exit 1
        fi
    fi
    
    # 매니페스트 파일 확인
    if [ ! -f "$BACKUP_DIR/backup_manifest.json" ]; then
        echo -e "${RED}❌ 백업 매니페스트 파일이 없습니다${NC}"
        exit 1
    fi
    
    echo "   백업 정보 로드 중..."
    local backup_type=$(cat "$BACKUP_DIR/backup_manifest.json" | grep '"type"' | cut -d'"' -f4)
    local backup_timestamp=$(cat "$BACKUP_DIR/backup_manifest.json" | grep '"timestamp"' | cut -d'"' -f4)
    
    echo "   백업 날짜: $backup_timestamp"
    echo "   백업 유형: $backup_type"
    echo -e "${GREEN}✅ 백업 파일 검증 완료${NC}"
}

download_from_s3() {
    if [ -n "$S3_BUCKET" ] && command -v aws &> /dev/null; then
        echo -e "${BLUE}☁️ S3에서 백업 다운로드${NC}"
        
        echo "   S3에서 백업 다운로드 중: s3://$S3_BUCKET/sooshinsa-backups/$BACKUP_DATE"
        
        # 백업 디렉토리 생성
        sudo mkdir -p "$BACKUP_DIR"
        
        # S3에서 백업 다운로드
        aws s3 sync "s3://$S3_BUCKET/sooshinsa-backups/$BACKUP_DATE" "$BACKUP_DIR" \
            --region "$AWS_REGION"
        
        echo -e "${GREEN}✅ S3 다운로드 완료${NC}"
    else
        echo -e "${RED}❌ S3 설정이 없거나 AWS CLI가 설치되지 않았습니다${NC}"
        exit 1
    fi
}

create_pre_restore_backup() {
    echo -e "${BLUE}💾 복원 전 현재 상태 백업${NC}"
    
    local pre_restore_backup_date=$(date '+%Y%m%d_%H%M%S')
    local pre_restore_dir="$BACKUP_BASE_DIR/pre_restore_$pre_restore_backup_date"
    
    echo "   현재 상태를 백업 중..."
    
    # 현재 상태 백업 실행
    if ./scripts/backup.sh db > /dev/null 2>&1; then
        echo "   복원 전 백업 완료: pre_restore_$pre_restore_backup_date"
        echo -e "${GREEN}✅ 복원 전 백업 완료${NC}"
    else
        echo -e "${YELLOW}⚠️  복원 전 백업 실패 (계속 진행)${NC}"
    fi
}

stop_services() {
    echo -e "${BLUE}🛑 서비스 중지${NC}"
    
    echo "   애플리케이션 서비스 중지 중..."
    docker-compose stop app nginx 2>/dev/null || true
    
    echo -e "${GREEN}✅ 서비스 중지 완료${NC}"
}

restore_database() {
    echo -e "${BLUE}🗄️ 데이터베이스 복원${NC}"
    
    local mysql_backup_file="$BACKUP_DIR/database/mysql_backup_$BACKUP_DATE.sql.gz"
    local redis_backup_file="$BACKUP_DIR/database/redis_backup_$BACKUP_DATE.rdb.gz"
    
    if [ ! -f "$mysql_backup_file" ]; then
        echo -e "${RED}❌ MySQL 백업 파일을 찾을 수 없습니다: $mysql_backup_file${NC}"
        return 1
    fi
    
    # MySQL 복원
    echo "   MySQL 데이터베이스 복원 중..."
    
    # MySQL 서비스 실행 확인
    if ! docker-compose ps mysql | grep -q "Up"; then
        echo "   MySQL 서비스 시작 중..."
        docker-compose up -d mysql
        
        # MySQL 준비 대기
        echo -n "   MySQL 준비 대기 중"
        for i in {1..30}; do
            if docker-compose exec -T mysql mysqladmin ping -h localhost --silent 2>/dev/null; then
                echo -e "\n   MySQL 준비 완료"
                break
            fi
            echo -n "."
            sleep 2
        done
    fi
    
    # 기존 데이터베이스 백업 (안전장치)
    echo "   기존 데이터베이스 임시 백업 중..."
    docker-compose exec -T mysql mysqldump \
        --single-transaction \
        --databases soo_shinsa \
        -u root -p${DB_PASSWORD} > "/tmp/pre_restore_db_$(date '+%H%M%S').sql" 2>/dev/null || true
    
    # 데이터베이스 복원
    echo "   데이터베이스 복원 실행 중..."
    gunzip -c "$mysql_backup_file" | docker-compose exec -T mysql mysql -u root -p${DB_PASSWORD}
    
    echo -e "${GREEN}✅ MySQL 복원 완료${NC}"
    
    # Redis 복원
    if [ -f "$redis_backup_file" ]; then
        echo "   Redis 데이터 복원 중..."
        
        # Redis 서비스 중지
        docker-compose stop redis
        
        # Redis 데이터 디렉토리 정리
        docker-compose run --rm redis rm -f /data/dump.rdb 2>/dev/null || true
        
        # 백업된 RDB 파일 복원
        gunzip -c "$redis_backup_file" > /tmp/restore_dump.rdb
        docker cp /tmp/restore_dump.rdb $(docker-compose ps -q redis):/data/dump.rdb 2>/dev/null || true
        rm -f /tmp/restore_dump.rdb
        
        # Redis 서비스 재시작
        docker-compose up -d redis
        
        echo -e "${GREEN}✅ Redis 복원 완료${NC}"
    else
        echo -e "${YELLOW}⚠️  Redis 백업 파일을 찾을 수 없습니다${NC}"
    fi
}

restore_application_files() {
    echo -e "${BLUE}📂 애플리케이션 파일 복원${NC}"
    
    local files_backup_file="$BACKUP_DIR/files/application_files_$BACKUP_DATE.tar.gz"
    
    if [ ! -f "$files_backup_file" ]; then
        echo -e "${RED}❌ 애플리케이션 파일 백업을 찾을 수 없습니다: $files_backup_file${NC}"
        return 1
    fi
    
    echo "   현재 애플리케이션 파일 백업 중..."
    tar -czf "/tmp/pre_restore_app_$(date '+%H%M%S').tar.gz" \
        -C /Users/hwangsang-ik/IdeaProjects/sparta/soo-shinsa \
        --exclude='target' --exclude='build' --exclude='.git' \
        . 2>/dev/null || true
    
    echo "   애플리케이션 파일 복원 중..."
    tar -xzf "$files_backup_file" -C /Users/hwangsang-ik/IdeaProjects/sparta/soo-shinsa
    
    echo -e "${GREEN}✅ 애플리케이션 파일 복원 완료${NC}"
}

restore_configuration() {
    echo -e "${BLUE}⚙️ 설정 파일 복원${NC}"
    
    local config_backup_file="$BACKUP_DIR/config/configuration_$BACKUP_DATE.tar.gz"
    
    if [ ! -f "$config_backup_file" ]; then
        echo -e "${RED}❌ 설정 파일 백업을 찾을 수 없습니다: $config_backup_file${NC}"
        return 1
    fi
    
    echo "   현재 설정 파일 백업 중..."
    tar -czf "/tmp/pre_restore_config_$(date '+%H%M%S').tar.gz" \
        -C /Users/hwangsang-ik/IdeaProjects/sparta/soo-shinsa \
        .env docker-compose.yml Dockerfile nginx/ scripts/ .github/ 2>/dev/null || true
    
    echo "   설정 파일 복원 중..."
    tar -xzf "$config_backup_file" -C /Users/hwangsang-ik/IdeaProjects/sparta/soo-shinsa
    
    echo -e "${GREEN}✅ 설정 파일 복원 완료${NC}"
}

start_services() {
    echo -e "${BLUE}🚀 서비스 시작${NC}"
    
    echo "   인프라 서비스 시작 중..."
    docker-compose up -d mysql redis
    
    # 서비스 준비 대기
    echo -n "   MySQL 준비 대기 중"
    for i in {1..30}; do
        if docker-compose exec -T mysql mysqladmin ping -h localhost --silent 2>/dev/null; then
            echo -e "\n   MySQL 준비 완료"
            break
        fi
        echo -n "."
        sleep 2
    done
    
    echo -n "   Redis 준비 대기 중"
    for i in {1..15}; do
        if docker-compose exec -T redis redis-cli ping 2>/dev/null | grep -q PONG; then
            echo -e "\n   Redis 준비 완료"
            break
        fi
        echo -n "."
        sleep 1
    done
    
    echo "   애플리케이션 서비스 시작 중..."
    docker-compose up -d app nginx
    
    echo -e "${GREEN}✅ 서비스 시작 완료${NC}"
}

verify_restoration() {
    echo -e "${BLUE}🔍 복원 검증${NC}"
    
    # 헬스체크
    echo -n "   애플리케이션 헬스체크 중"
    for i in {1..60}; do
        if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
            echo -e "\n   ✅ 애플리케이션 정상 응답"
            break
        fi
        echo -n "."
        sleep 2
    done
    
    # 데이터베이스 연결 확인
    echo -n "   데이터베이스 연결 확인 중"
    if docker-compose exec -T mysql mysqladmin ping -h localhost --silent 2>/dev/null; then
        echo -e " ✅"
    else
        echo -e " ❌"
        return 1
    fi
    
    # Redis 연결 확인
    echo -n "   Redis 연결 확인 중"
    if docker-compose exec -T redis redis-cli ping 2>/dev/null | grep -q PONG; then
        echo -e " ✅"
    else
        echo -e " ❌"
        return 1
    fi
    
    echo -e "${GREEN}✅ 복원 검증 완료${NC}"
}

show_restore_summary() {
    echo ""
    echo "🔄 복원 완료 리포트"
    echo "=================="
    echo "복원 시간: $(date '+%Y-%m-%d %H:%M:%S')"
    echo "복원 유형: $RESTORE_TYPE"
    echo "백업 날짜: $BACKUP_DATE"
    echo ""
    echo "📊 서비스 상태:"
    docker-compose ps
    echo ""
    echo "🔍 접속 정보:"
    echo "   🌐 애플리케이션: http://localhost:8080"
    echo "   📈 Grafana: http://localhost:3000"
    echo "   📊 Prometheus: http://localhost:9090"
    echo ""
    echo "💡 문제 발생 시:"
    echo "   📋 로그 확인: docker-compose logs -f app"
    echo "   🔄 서비스 재시작: docker-compose restart"
    echo "   🧪 헬스체크: ./scripts/health-check.sh"
    echo ""
}

# 사용법 출력
if [ "$1" = "--help" ] || [ "$1" = "-h" ]; then
    echo "사용법: $0 <backup_date> [restore_type]"
    echo ""
    echo "복원 유형:"
    echo "  full   - 전체 복원 (기본값)"
    echo "  db     - 데이터베이스만 복원"
    echo "  files  - 파일만 복원"
    echo "  config - 설정만 복원"
    echo ""
    echo "예시:"
    echo "  $0 20241201_143000 full"
    echo "  $0 20241201_143000 db"
    echo ""
    echo "사용 가능한 백업 목록:"
    if [ -d "$BACKUP_BASE_DIR" ]; then
        ls -1 "$BACKUP_BASE_DIR" | grep "^20" | head -10
    else
        echo "  백업 디렉토리가 없습니다: $BACKUP_BASE_DIR"
    fi
    echo ""
    exit 0
fi

# 복원 확인
echo -e "${YELLOW}⚠️  복원을 실행하면 현재 데이터가 백업 시점으로 되돌려집니다.${NC}"
echo "복원할 백업: $BACKUP_DATE"
echo "복원 유형: $RESTORE_TYPE"
echo ""
read -p "정말로 복원하시겠습니까? (y/N): " -n 1 -r
echo

if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}⏸️  복원이 취소되었습니다${NC}"
    exit 0
fi

# 메인 복원 실행
echo "복원 시작 시간: $(date '+%Y-%m-%d %H:%M:%S')"
echo ""

validate_backup
create_pre_restore_backup
stop_services

case $RESTORE_TYPE in
    "full")
        restore_database
        restore_application_files
        restore_configuration
        ;;
    "db")
        restore_database
        ;;
    "files")
        restore_application_files
        ;;
    "config")
        restore_configuration
        ;;
    *)
        echo -e "${RED}❌ 지원하지 않는 복원 유형: $RESTORE_TYPE${NC}"
        echo "사용 가능한 유형: full, db, files, config"
        exit 1
        ;;
esac

start_services

if verify_restoration; then
    show_restore_summary
    echo -e "${GREEN}🎉 복원 성공!${NC}"
else
    echo -e "${RED}❌ 복원 검증 실패!${NC}"
    echo "로그를 확인하고 필요시 수동으로 문제를 해결하세요."
    exit 1
fi