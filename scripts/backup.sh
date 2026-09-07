#!/bin/bash

# 💾 SooShinsa 백업 스크립트
# 데이터베이스, 파일, 설정 등을 안전하게 백업

echo "💾 SooShinsa 백업 시작!"
echo "======================"

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 에러 처리
set -e
trap 'echo -e "${RED}❌ 백업 중 오류가 발생했습니다!${NC}"; cleanup; exit 1' ERR

# 변수 설정
BACKUP_TYPE=${1:-full}  # full, db, files
BACKUP_BASE_DIR="/backup/sooshinsa"
BACKUP_DATE=$(date '+%Y%m%d_%H%M%S')
BACKUP_DIR="$BACKUP_BASE_DIR/$BACKUP_DATE"
RETENTION_DAYS=7  # 백업 보관 기간

# S3 설정 (선택사항)
S3_BUCKET=${S3_BACKUP_BUCKET:-""}
AWS_REGION=${AWS_REGION:-"ap-northeast-2"}

cleanup() {
    echo -e "${YELLOW}🧹 임시 파일 정리 중...${NC}"
    rm -rf /tmp/sooshinsa_backup_* 2>/dev/null || true
}

create_backup_dir() {
    echo -e "${BLUE}📁 백업 디렉토리 생성${NC}"
    
    # 로컬 백업 디렉토리 생성
    sudo mkdir -p "$BACKUP_DIR"/{database,files,config,logs}
    
    echo "   백업 위치: $BACKUP_DIR"
    echo -e "${GREEN}✅ 백업 디렉토리 준비 완료${NC}"
}

backup_database() {
    echo -e "${BLUE}🗄️ 데이터베이스 백업${NC}"
    
    local db_backup_file="$BACKUP_DIR/database/mysql_backup_$BACKUP_DATE.sql"
    local redis_backup_file="$BACKUP_DIR/database/redis_backup_$BACKUP_DATE.rdb"
    
    # MySQL 백업
    echo "   MySQL 데이터베이스 백업 중..."
    docker-compose exec -T mysql mysqldump \
        --single-transaction \
        --routines \
        --triggers \
        --events \
        --add-drop-database \
        --databases soo_shinsa \
        -u root -p${DB_PASSWORD} > "$db_backup_file"
    
    # 백업 파일 압축
    gzip "$db_backup_file"
    echo "   MySQL 백업 완료: ${db_backup_file}.gz"
    
    # Redis 백업
    echo "   Redis 데이터 백업 중..."
    docker-compose exec -T redis redis-cli BGSAVE
    
    # Redis RDB 파일 복사
    docker cp $(docker-compose ps -q redis):/data/dump.rdb "$redis_backup_file"
    gzip "$redis_backup_file"
    echo "   Redis 백업 완료: ${redis_backup_file}.gz"
    
    echo -e "${GREEN}✅ 데이터베이스 백업 완료${NC}"
}

backup_application_files() {
    echo -e "${BLUE}📂 애플리케이션 파일 백업${NC}"
    
    local files_backup_file="$BACKUP_DIR/files/application_files_$BACKUP_DATE.tar.gz"
    
    # 중요한 애플리케이션 파일들 백업
    echo "   애플리케이션 파일 압축 중..."
    tar -czf "$files_backup_file" \
        --exclude='target' \
        --exclude='build' \
        --exclude='node_modules' \
        --exclude='.git' \
        --exclude='logs' \
        --exclude='*.log' \
        -C /Users/hwangsang-ik/IdeaProjects/sparta/soo-shinsa \
        .
    
    echo "   애플리케이션 파일 백업 완료: $files_backup_file"
    echo -e "${GREEN}✅ 애플리케이션 파일 백업 완료${NC}"
}

backup_configuration() {
    echo -e "${BLUE}⚙️ 설정 파일 백업${NC}"
    
    local config_backup_file="$BACKUP_DIR/config/configuration_$BACKUP_DATE.tar.gz"
    
    # 설정 파일들 백업
    echo "   설정 파일 백업 중..."
    tar -czf "$config_backup_file" \
        -C /Users/hwangsang-ik/IdeaProjects/sparta/soo-shinsa \
        .env \
        docker-compose.yml \
        docker-compose.prod.yml \
        Dockerfile \
        nginx/ \
        scripts/ \
        .github/ 2>/dev/null || true
    
    echo "   설정 파일 백업 완료: $config_backup_file"
    echo -e "${GREEN}✅ 설정 파일 백업 완료${NC}"
}

backup_logs() {
    echo -e "${BLUE}📋 로그 파일 백업${NC}"
    
    local logs_backup_file="$BACKUP_DIR/logs/application_logs_$BACKUP_DATE.tar.gz"
    
    # Docker 컨테이너 로그 백업
    echo "   Docker 로그 백업 중..."
    mkdir -p /tmp/sooshinsa_logs_$BACKUP_DATE
    
    # 각 컨테이너별 로그 수집
    local containers=("app" "mysql" "redis" "prometheus" "grafana" "nginx")
    for container in "${containers[@]}"; do
        if docker-compose ps | grep -q "$container.*Up"; then
            docker-compose logs --no-color "$container" > "/tmp/sooshinsa_logs_$BACKUP_DATE/${container}.log" 2>/dev/null || true
        fi
    done
    
    # 로그 파일 압축
    tar -czf "$logs_backup_file" -C /tmp "sooshinsa_logs_$BACKUP_DATE"
    rm -rf "/tmp/sooshinsa_logs_$BACKUP_DATE"
    
    echo "   로그 파일 백업 완료: $logs_backup_file"
    echo -e "${GREEN}✅ 로그 파일 백업 완료${NC}"
}

create_backup_manifest() {
    echo -e "${BLUE}📋 백업 매니페스트 생성${NC}"
    
    local manifest_file="$BACKUP_DIR/backup_manifest.json"
    
    cat > "$manifest_file" << EOF
{
  "backup_info": {
    "timestamp": "$BACKUP_DATE",
    "type": "$BACKUP_TYPE",
    "version": "1.0",
    "hostname": "$(hostname)",
    "git_commit": "$(cd /Users/hwangsang-ik/IdeaProjects/sparta/soo-shinsa && git rev-parse HEAD 2>/dev/null || echo 'unknown')"
  },
  "services": {
    "mysql": {
      "version": "$(docker-compose exec -T mysql mysql --version 2>/dev/null | head -1 || echo 'unknown')",
      "backup_file": "database/mysql_backup_$BACKUP_DATE.sql.gz"
    },
    "redis": {
      "version": "$(docker-compose exec -T redis redis-server --version 2>/dev/null | head -1 || echo 'unknown')",
      "backup_file": "database/redis_backup_$BACKUP_DATE.rdb.gz"
    },
    "application": {
      "backup_file": "files/application_files_$BACKUP_DATE.tar.gz"
    },
    "configuration": {
      "backup_file": "config/configuration_$BACKUP_DATE.tar.gz"
    },
    "logs": {
      "backup_file": "logs/application_logs_$BACKUP_DATE.tar.gz"
    }
  },
  "backup_size": "$(du -sh $BACKUP_DIR | cut -f1)",
  "file_count": $(find $BACKUP_DIR -type f | wc -l)
}
EOF

    echo "   백업 매니페스트 생성 완료: $manifest_file"
    echo -e "${GREEN}✅ 백업 매니페스트 생성 완료${NC}"
}

upload_to_s3() {
    if [ -n "$S3_BUCKET" ]; then
        echo -e "${BLUE}☁️ S3 업로드${NC}"
        
        # AWS CLI 설치 확인
        if ! command -v aws &> /dev/null; then
            echo -e "${YELLOW}⚠️  AWS CLI가 설치되지 않았습니다. S3 업로드를 건너뜁니다.${NC}"
            return 0
        fi
        
        echo "   S3 버킷으로 업로드 중: s3://$S3_BUCKET"
        
        # 백업 디렉토리 전체를 S3에 업로드
        aws s3 sync "$BACKUP_DIR" "s3://$S3_BUCKET/sooshinsa-backups/$BACKUP_DATE" \
            --region "$AWS_REGION" \
            --storage-class STANDARD_IA
        
        echo -e "${GREEN}✅ S3 업로드 완료${NC}"
    else
        echo -e "${YELLOW}⚠️  S3 설정이 없습니다. 로컬 백업만 유지됩니다.${NC}"
    fi
}

cleanup_old_backups() {
    echo -e "${BLUE}🧹 오래된 백업 정리${NC}"
    
    echo "   $RETENTION_DAYS일 이전 백업 정리 중..."
    
    # 로컬 백업 정리
    find "$BACKUP_BASE_DIR" -type d -name "20*_*" -mtime +$RETENTION_DAYS -exec rm -rf {} + 2>/dev/null || true
    
    # S3 백업 정리 (S3 Lifecycle 정책을 사용하는 것이 권장됨)
    if [ -n "$S3_BUCKET" ] && command -v aws &> /dev/null; then
        echo "   S3 오래된 백업 정리 중..."
        aws s3 ls "s3://$S3_BUCKET/sooshinsa-backups/" --recursive | \
        while read -r line; do
            create_date=$(echo $line | awk '{print $1" "$2}')
            create_date_seconds=$(date -d "$create_date" +%s 2>/dev/null || echo 0)
            current_date_seconds=$(date +%s)
            days_old=$(( (current_date_seconds - create_date_seconds) / 86400 ))
            
            if [ $days_old -gt $RETENTION_DAYS ]; then
                file_path=$(echo $line | awk '{print $4}')
                aws s3 rm "s3://$S3_BUCKET/$file_path" 2>/dev/null || true
            fi
        done
    fi
    
    echo -e "${GREEN}✅ 오래된 백업 정리 완료${NC}"
}

verify_backup() {
    echo -e "${BLUE}🔍 백업 검증${NC}"
    
    local verification_failed=false
    
    # 백업 파일들 존재 여부 확인
    local required_files=(
        "$BACKUP_DIR/database/mysql_backup_$BACKUP_DATE.sql.gz"
        "$BACKUP_DIR/database/redis_backup_$BACKUP_DATE.rdb.gz"
        "$BACKUP_DIR/files/application_files_$BACKUP_DATE.tar.gz"
        "$BACKUP_DIR/config/configuration_$BACKUP_DATE.tar.gz"
        "$BACKUP_DIR/logs/application_logs_$BACKUP_DATE.tar.gz"
        "$BACKUP_DIR/backup_manifest.json"
    )
    
    for file in "${required_files[@]}"; do
        if [ -f "$file" ]; then
            local file_size=$(du -h "$file" | cut -f1)
            echo "   ✅ $(basename "$file"): $file_size"
        else
            echo "   ❌ $(basename "$file"): 누락됨"
            verification_failed=true
        fi
    done
    
    # MySQL 백업 파일 무결성 검사
    echo "   MySQL 백업 파일 검증 중..."
    if gunzip -t "$BACKUP_DIR/database/mysql_backup_$BACKUP_DATE.sql.gz" 2>/dev/null; then
        echo "   ✅ MySQL 백업 파일 무결성 확인"
    else
        echo "   ❌ MySQL 백업 파일 손상됨"
        verification_failed=true
    fi
    
    if [ "$verification_failed" = true ]; then
        echo -e "${RED}❌ 백업 검증 실패${NC}"
        return 1
    else
        echo -e "${GREEN}✅ 백업 검증 완료${NC}"
        return 0
    fi
}

show_backup_summary() {
    echo ""
    echo "💾 백업 완료 리포트"
    echo "=================="
    echo "백업 시간: $(date '+%Y-%m-%d %H:%M:%S')"
    echo "백업 유형: $BACKUP_TYPE"
    echo "백업 위치: $BACKUP_DIR"
    echo "백업 크기: $(du -sh $BACKUP_DIR | cut -f1)"
    echo "파일 개수: $(find $BACKUP_DIR -type f | wc -l)개"
    echo ""
    
    if [ -n "$S3_BUCKET" ]; then
        echo "☁️ S3 백업: s3://$S3_BUCKET/sooshinsa-backups/$BACKUP_DATE"
    fi
    
    echo ""
    echo "💡 복원 방법:"
    echo "   전체 복원: ./scripts/restore.sh $BACKUP_DATE"
    echo "   DB만 복원: ./scripts/restore.sh $BACKUP_DATE db"
    echo ""
}

# 사용법 출력
if [ "$1" = "--help" ] || [ "$1" = "-h" ]; then
    echo "사용법: $0 [backup_type]"
    echo ""
    echo "백업 유형:"
    echo "  full   - 전체 백업 (기본값)"
    echo "  db     - 데이터베이스만 백업"
    echo "  files  - 파일만 백업"
    echo "  config - 설정만 백업"
    echo ""
    echo "환경 변수:"
    echo "  S3_BACKUP_BUCKET - S3 백업 버킷 이름"
    echo "  AWS_REGION       - AWS 리전 (기본: ap-northeast-2)"
    echo ""
    exit 0
fi

# 메인 백업 실행
echo "백업 유형: $BACKUP_TYPE"
echo "백업 시작 시간: $(date '+%Y-%m-%d %H:%M:%S')"
echo ""

create_backup_dir

case $BACKUP_TYPE in
    "full")
        backup_database
        backup_application_files
        backup_configuration
        backup_logs
        ;;
    "db")
        backup_database
        ;;
    "files")
        backup_application_files
        ;;
    "config")
        backup_configuration
        ;;
    *)
        echo -e "${RED}❌ 지원하지 않는 백업 유형: $BACKUP_TYPE${NC}"
        echo "사용 가능한 유형: full, db, files, config"
        exit 1
        ;;
esac

create_backup_manifest

if verify_backup; then
    upload_to_s3
    cleanup_old_backups
    show_backup_summary
    echo -e "${GREEN}🎉 백업 성공!${NC}"
else
    echo -e "${RED}❌ 백업 실패!${NC}"
    exit 1
fi