#!/bin/bash

# ⏰ SooShinsa 백업 스케줄러
# 정기적인 백업 작업을 관리하고 실행

echo "⏰ SooShinsa 백업 스케줄러"
echo "========================"

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 변수 설정
ACTION=${1:-status}  # install, uninstall, status, run
BACKUP_SCRIPT_DIR="/Users/hwangsang-ik/IdeaProjects/sparta/soo-shinsa/scripts"
LOG_DIR="/var/log/sooshinsa"
CRON_JOB_LABEL="# SooShinsa Backup Jobs"

install_cron_jobs() {
    echo -e "${BLUE}📅 백업 스케줄 설치${NC}"
    
    # 로그 디렉토리 생성
    sudo mkdir -p "$LOG_DIR"
    sudo chmod 755 "$LOG_DIR"
    
    # 기존 SooShinsa 백업 작업 제거
    crontab -l 2>/dev/null | grep -v "$CRON_JOB_LABEL" | grep -v "sooshinsa.*backup" > /tmp/crontab_new
    
    # 새로운 백업 스케줄 추가
    cat >> /tmp/crontab_new << EOF

$CRON_JOB_LABEL
# 매일 새벽 2시 - 전체 백업
0 2 * * * $BACKUP_SCRIPT_DIR/backup.sh full >> $LOG_DIR/backup.log 2>&1

# 매 6시간마다 - 데이터베이스 백업
0 */6 * * * $BACKUP_SCRIPT_DIR/backup.sh db >> $LOG_DIR/backup-db.log 2>&1

# 매주 일요일 새벽 1시 - 백업 디렉토리 정리
0 1 * * 0 find /backup/sooshinsa -type d -name "20*_*" -mtime +30 -exec rm -rf {} + >> $LOG_DIR/cleanup.log 2>&1

# 매시간 - 헬스체크 (문제 발생 시 백업 트리거)
0 * * * * $BACKUP_SCRIPT_DIR/health-check.sh || $BACKUP_SCRIPT_DIR/backup.sh db >> $LOG_DIR/emergency-backup.log 2>&1

EOF
    
    # 새로운 crontab 적용
    crontab /tmp/crontab_new
    rm -f /tmp/crontab_new
    
    echo "   ✅ 백업 스케줄이 설치되었습니다:"
    echo "      - 매일 02:00: 전체 백업"
    echo "      - 매 6시간: 데이터베이스 백업"
    echo "      - 매주 일요일 01:00: 정리 작업"
    echo "      - 매시간: 헬스체크 + 응급 백업"
    echo ""
    echo "   📋 로그 위치: $LOG_DIR/"
    echo -e "${GREEN}✅ 백업 스케줄 설치 완료${NC}"
}

uninstall_cron_jobs() {
    echo -e "${BLUE}🗑️ 백업 스케줄 제거${NC}"
    
    # SooShinsa 백업 작업 제거
    crontab -l 2>/dev/null | grep -v "$CRON_JOB_LABEL" | grep -v "sooshinsa.*backup" > /tmp/crontab_new
    crontab /tmp/crontab_new
    rm -f /tmp/crontab_new
    
    echo -e "${GREEN}✅ 백업 스케줄 제거 완료${NC}"
}

show_status() {
    echo -e "${BLUE}📊 백업 스케줄 상태${NC}"
    
    # 현재 cron 작업 확인
    echo "📅 현재 설정된 백업 스케줄:"
    if crontab -l 2>/dev/null | grep -q "sooshinsa.*backup"; then
        crontab -l | grep -A 10 "$CRON_JOB_LABEL" | grep -E "(backup|health-check)" | while read line; do
            echo "   $line"
        done
    else
        echo "   ❌ 설정된 백업 스케줄이 없습니다"
    fi
    echo ""
    
    # 최근 백업 상태 확인
    echo "📋 최근 백업 기록:"
    if [ -d "/backup/sooshinsa" ]; then
        local recent_backups=($(ls -1t /backup/sooshinsa | head -5))
        for backup in "${recent_backups[@]}"; do
            local backup_size=$(du -sh "/backup/sooshinsa/$backup" 2>/dev/null | cut -f1)
            echo "   📁 $backup ($backup_size)"
        done
    else
        echo "   ❌ 백업 디렉토리가 없습니다"
    fi
    echo ""
    
    # 로그 파일 상태 확인
    echo "📋 백업 로그 상태:"
    local log_files=("backup.log" "backup-db.log" "cleanup.log" "emergency-backup.log")
    for log_file in "${log_files[@]}"; do
        if [ -f "$LOG_DIR/$log_file" ]; then
            local log_size=$(du -h "$LOG_DIR/$log_file" | cut -f1)
            local last_modified=$(stat -f "%Sm" -t "%Y-%m-%d %H:%M" "$LOG_DIR/$log_file" 2>/dev/null || echo "unknown")
            echo "   📄 $log_file ($log_size, $last_modified)"
        else
            echo "   ❌ $log_file (없음)"
        fi
    done
    echo ""
    
    # 디스크 사용량 확인
    echo "💾 디스크 사용량:"
    if [ -d "/backup" ]; then
        echo "   백업 디렉토리: $(du -sh /backup 2>/dev/null | cut -f1)"
    fi
    if [ -d "$LOG_DIR" ]; then
        echo "   로그 디렉토리: $(du -sh $LOG_DIR 2>/dev/null | cut -f1)"
    fi
    echo "   전체 디스크: $(df -h / | tail -1 | awk '{print $3"/"$2" ("$5")"}')"
}

run_backup() {
    local backup_type=${2:-full}
    
    echo -e "${BLUE}🚀 백업 실행${NC}"
    echo "백업 유형: $backup_type"
    echo "실행 시간: $(date '+%Y-%m-%d %H:%M:%S')"
    echo ""
    
    # 백업 실행
    if "$BACKUP_SCRIPT_DIR/backup.sh" "$backup_type"; then
        echo -e "${GREEN}✅ 백업 성공${NC}"
        
        # 성공 알림 (선택사항)
        if command -v mail &> /dev/null && [ -n "$BACKUP_ADMIN_EMAIL" ]; then
            echo "백업이 성공적으로 완료되었습니다. ($(date))" | \
            mail -s "SooShinsa 백업 성공" "$BACKUP_ADMIN_EMAIL"
        fi
    else
        echo -e "${RED}❌ 백업 실패${NC}"
        
        # 실패 알림 (선택사항)
        if command -v mail &> /dev/null && [ -n "$BACKUP_ADMIN_EMAIL" ]; then
            echo "백업이 실패했습니다. 시스템을 확인해주세요. ($(date))" | \
            mail -s "SooShinsa 백업 실패" "$BACKUP_ADMIN_EMAIL"
        fi
        
        return 1
    fi
}

show_logs() {
    local log_type=${2:-all}
    local lines=${3:-50}
    
    echo -e "${BLUE}📋 백업 로그 보기${NC}"
    
    case $log_type in
        "backup"|"full")
            if [ -f "$LOG_DIR/backup.log" ]; then
                echo "📄 전체 백업 로그 (최근 $lines줄):"
                tail -n "$lines" "$LOG_DIR/backup.log"
            else
                echo "❌ 전체 백업 로그 파일이 없습니다"
            fi
            ;;
        "db")
            if [ -f "$LOG_DIR/backup-db.log" ]; then
                echo "📄 데이터베이스 백업 로그 (최근 $lines줄):"
                tail -n "$lines" "$LOG_DIR/backup-db.log"
            else
                echo "❌ 데이터베이스 백업 로그 파일이 없습니다"
            fi
            ;;
        "emergency")
            if [ -f "$LOG_DIR/emergency-backup.log" ]; then
                echo "📄 응급 백업 로그 (최근 $lines줄):"
                tail -n "$lines" "$LOG_DIR/emergency-backup.log"
            else
                echo "❌ 응급 백업 로그 파일이 없습니다"
            fi
            ;;
        "cleanup")
            if [ -f "$LOG_DIR/cleanup.log" ]; then
                echo "📄 정리 작업 로그 (최근 $lines줄):"
                tail -n "$lines" "$LOG_DIR/cleanup.log"
            else
                echo "❌ 정리 작업 로그 파일이 없습니다"
            fi
            ;;
        "all")
            for log_file in backup.log backup-db.log emergency-backup.log cleanup.log; do
                if [ -f "$LOG_DIR/$log_file" ]; then
                    echo ""
                    echo "📄 $log_file (최근 10줄):"
                    tail -n 10 "$LOG_DIR/$log_file"
                fi
            done
            ;;
        *)
            echo "❌ 지원하지 않는 로그 유형: $log_type"
            echo "사용 가능한 유형: backup, db, emergency, cleanup, all"
            ;;
    esac
}

test_backup() {
    echo -e "${BLUE}🧪 백업 테스트${NC}"
    
    echo "   백업 스크립트 존재 확인..."
    if [ -f "$BACKUP_SCRIPT_DIR/backup.sh" ]; then
        echo "   ✅ backup.sh"
    else
        echo "   ❌ backup.sh 없음"
        return 1
    fi
    
    echo "   백업 디렉토리 권한 확인..."
    if sudo mkdir -p "/backup/sooshinsa/test" 2>/dev/null; then
        sudo rmdir "/backup/sooshinsa/test"
        echo "   ✅ 백업 디렉토리 쓰기 가능"
    else
        echo "   ❌ 백업 디렉토리 쓰기 불가"
        return 1
    fi
    
    echo "   Docker 서비스 상태 확인..."
    if docker-compose ps | grep -q "Up"; then
        echo "   ✅ Docker 서비스 실행 중"
    else
        echo "   ❌ Docker 서비스 중지됨"
        return 1
    fi
    
    echo "   간단한 백업 테스트 실행..."
    if "$BACKUP_SCRIPT_DIR/backup.sh" config > /tmp/backup_test.log 2>&1; then
        echo "   ✅ 백업 테스트 성공"
        rm -f /tmp/backup_test.log
    else
        echo "   ❌ 백업 테스트 실패"
        echo "   로그: /tmp/backup_test.log"
        return 1
    fi
    
    echo -e "${GREEN}✅ 모든 백업 테스트 통과${NC}"
}

# 사용법 출력
if [ "$1" = "--help" ] || [ "$1" = "-h" ]; then
    echo "사용법: $0 <action> [options]"
    echo ""
    echo "액션:"
    echo "  install   - 백업 스케줄 설치"
    echo "  uninstall - 백업 스케줄 제거"
    echo "  status    - 백업 상태 확인 (기본값)"
    echo "  run       - 수동 백업 실행"
    echo "  logs      - 백업 로그 보기"
    echo "  test      - 백업 시스템 테스트"
    echo ""
    echo "예시:"
    echo "  $0 install               # 백업 스케줄 설치"
    echo "  $0 status                # 상태 확인"
    echo "  $0 run full              # 전체 백업 실행"
    echo "  $0 run db                # DB 백업 실행"
    echo "  $0 logs backup 100       # 백업 로그 100줄 보기"
    echo "  $0 test                  # 백업 시스템 테스트"
    echo ""
    echo "환경 변수:"
    echo "  BACKUP_ADMIN_EMAIL - 백업 결과 알림 이메일"
    echo ""
    exit 0
fi

# 메인 실행
case $ACTION in
    "install")
        install_cron_jobs
        ;;
    "uninstall")
        uninstall_cron_jobs
        ;;
    "status")
        show_status
        ;;
    "run")
        run_backup "$@"
        ;;
    "logs")
        show_logs "$@"
        ;;
    "test")
        test_backup
        ;;
    *)
        echo -e "${RED}❌ 지원하지 않는 액션: $ACTION${NC}"
        echo "사용 가능한 액션: install, uninstall, status, run, logs, test"
        echo "도움말: $0 --help"
        exit 1
        ;;
esac