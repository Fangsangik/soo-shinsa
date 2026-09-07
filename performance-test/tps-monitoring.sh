#!/bin/bash

# 🚀 실시간 TPS 모니터링
echo "📊 실시간 TPS 모니터링"
echo "===================="

CSV_FILE="results/quick-tps-analysis.csv"

while true; do
    clear
    echo "🔥 SooShinsa TPS vs 스레드 수 - 실시간 결과"
    echo "=========================================="
    echo "$(date '+%Y-%m-%d %H:%M:%S') 기준"
    echo ""
    
    if [ -f "$CSV_FILE" ]; then
        printf "%-10s %-10s %-15s %-10s %-10s\n" "스레드수" "TPS" "평균응답시간ms" "성공률%" "오류수"
        printf "%-10s %-10s %-15s %-10s %-10s\n" "------" "---" "----------" "-----" "----"
        
        tail -n +2 "$CSV_FILE" | while IFS=',' read -r threads tps avg_time success_rate errors max_time; do
            if [ -n "$threads" ]; then
                printf "%-10s %-10s %-15s %-10s %-10s\n" "$threads" "$tps" "$avg_time" "$success_rate" "$errors"
            fi
        done
        
        echo ""
        echo "📈 현재 진행 상황:"
        CURRENT_COUNT=$(tail -n +2 "$CSV_FILE" | grep -c ".")
        TOTAL_TESTS=5
        PROGRESS=$((CURRENT_COUNT * 100 / TOTAL_TESTS))
        echo "완료: ${CURRENT_COUNT}/${TOTAL_TESTS} 테스트 (${PROGRESS}%)"
        
        # 최고 TPS 계산
        BEST_TPS=$(tail -n +2 "$CSV_FILE" | awk -F',' 'BEGIN{max=0} {if($2>max) max=$2} END {print max}')
        echo "현재 최고 TPS: ${BEST_TPS}"
        
    else
        echo "⏳ 테스트 결과 파일을 기다리는 중..."
    fi
    
    echo ""
    echo "⏳ 5초 후 새로고침... (Ctrl+C로 종료)"
    sleep 5
done