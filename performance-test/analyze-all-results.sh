#!/bin/bash

# 🔍 모든 부하 테스트 결과 종합 분석
echo "🔍 SooShinsa 전체 부하 테스트 결과 종합 분석"
echo "============================================="

analyze_jtl_detailed() {
    local file=$1
    local test_name=$2
    local expected_threads=$3
    
    if [ ! -f "$file" ]; then
        echo "❌ $test_name: 결과 파일 없음"
        return
    fi
    
    local total=$(tail -n +2 "$file" | wc -l | tr -d ' ')
    local success=$(tail -n +2 "$file" | awk -F',' '$8=="true"' | wc -l | tr -d ' ')
    local errors=$((total - success))
    
    if [ $total -gt 0 ]; then
        local success_rate=$(echo "scale=1; $success * 100 / $total" | bc 2>/dev/null || echo "0")
        local avg_time=$(tail -n +2 "$file" | awk -F',' '{sum+=$2; count++} END {if(count>0) print int(sum/count); else print 0}')
        local max_time=$(tail -n +2 "$file" | awk -F',' 'BEGIN{max=0} {if($2>max) max=$2} END {print int(max)}')
        local min_time=$(tail -n +2 "$file" | awk -F',' 'BEGIN{min=999999} {if($2<min) min=$2} END {print int(min)}')
        
        # 시간 범위 계산
        local start_time=$(tail -n +2 "$file" | head -1 | cut -d',' -f1)
        local end_time=$(tail -1 "$file" | cut -d',' -f1)
        local duration_ms=$((end_time - start_time))
        local duration_sec=$(echo "scale=1; $duration_ms / 1000" | bc 2>/dev/null || echo "1")
        
        # TPS 계산
        local tps=$(echo "scale=1; $success / $duration_sec" | bc 2>/dev/null || echo "0")
        
        # P95 계산
        local p95_time=$(tail -n +2 "$file" | awk -F',' '{print $2}' | sort -n | awk 'BEGIN{count=0} {values[count++]=$1} END {p95_index=int(count*0.95); if(count>0) print int(values[p95_index]); else print 0}')
        
        echo "$expected_threads,$tps,$success_rate,$avg_time,$max_time,$p95_time,$errors,$total,$duration_sec,$test_name"
    else
        echo "$expected_threads,0,0,0,0,0,$total,0,0,$test_name"
    fi
}

# 결과 수집
echo "스레드수,TPS,성공률%,평균응답시간ms,최대응답시간ms,P95ms,오류수,총요청수,지속시간초,테스트명" > all_results.csv

cd results

# 기존 테스트 결과들 분석
analyze_jtl_detailed "demo-50/results.jtl" "Demo-50명" 50 >> all_results.csv
analyze_jtl_detailed "demo-100/results.jtl" "Demo-100명" 100 >> all_results.csv
analyze_jtl_detailed "tps-50-threads/results.jtl" "TPS-50명" 50 >> all_results.csv
analyze_jtl_detailed "tps-100-threads/results.jtl" "TPS-100명" 100 >> all_results.csv
analyze_jtl_detailed "high-load-100/results.jtl" "High-Load-100명" 100 >> all_results.csv
analyze_jtl_detailed "high-load-200/results.jtl" "High-Load-200명" 200 >> all_results.csv
analyze_jtl_detailed "load-test-200/results.jtl" "Load-Test-200명" 200 >> all_results.csv
analyze_jtl_detailed "load-test-300/results.jtl" "Load-Test-300명" 300 >> all_results.csv
analyze_jtl_detailed "extreme-300/results.jtl" "Extreme-300명" 300 >> all_results.csv

# 최신 실제 테스트들
analyze_jtl_detailed "test-350.jtl" "실제-350명" 350 >> all_results.csv
analyze_jtl_detailed "test-400.jtl" "실제-400명(헬스체크)" 400 >> all_results.csv
analyze_jtl_detailed "real-test-400.jtl" "실제-400명(비즈니스)" 400 >> all_results.csv
analyze_jtl_detailed "test-500-retry.jtl" "실제-500명(헬스체크)" 500 >> all_results.csv
analyze_jtl_detailed "real-test-600.jtl" "실제-600명(비즈니스)" 600 >> all_results.csv
analyze_jtl_detailed "test-750.jtl" "실제-750명(헬스체크)" 750 >> all_results.csv
analyze_jtl_detailed "test-1000.jtl" "실제-1000명(헬스체크)" 1000 >> all_results.csv
analyze_jtl_detailed "real-test-1000.jtl" "실제-1000명(비즈니스)" 1000 >> all_results.csv
analyze_jtl_detailed "extreme-1000.jtl" "극한-1000명(장시간)" 1000 >> all_results.csv

echo ""
echo "📊 전체 테스트 결과 요약"
echo "======================"

# 헤더 출력
printf "%-25s %-8s %-8s %-8s %-8s %-8s %-8s %-8s %-10s\n" \
    "테스트명" "스레드" "TPS" "성공률%" "평균ms" "최대ms" "P95ms" "오류수" "지속시간"
printf "%-25s %-8s %-8s %-8s %-8s %-8s %-8s %-8s %-10s\n" \
    "------------------------" "------" "-----" "-----" "-----" "-----" "-----" "-----" "--------"

# 데이터 출력 (성공률과 TPS 기준으로 정렬)
tail -n +2 all_results.csv | sort -t',' -k2,2n | while IFS=',' read -r threads tps success_rate avg_time max_time p95 errors total duration test_name; do
    # 성능 등급 표시
    if [ $(echo "$success_rate >= 99.5" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
        grade="🥇"
    elif [ $(echo "$success_rate >= 99.0" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
        grade="🥈"
    elif [ $(echo "$success_rate >= 95.0" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
        grade="🥉"
    elif [ $(echo "$success_rate > 0" | bc -l 2>/dev/null || echo "0") -eq 1 ]; then
        grade="📊"
    else
        grade="❌"
    fi
    
    printf "%-25s %-8s %-8s %-7s%s %-8s %-8s %-8s %-8s %-10s\n" \
        "$test_name" "$threads" "$tps" "$success_rate" "$grade" "$avg_time" "$max_time" "$p95" "$errors" "$duration"
done

echo ""
echo "🎯 성능 구간별 분석"
echo "=================="

echo ""
echo "✅ 완벽한 성능 (99.5%+ 성공률):"
tail -n +2 all_results.csv | awk -F',' '$3 >= 99.5 {print "   • " $10 ": " $1 "명, TPS " $2 ", " $3 "% 성공률"}' | sort -t':' -k2,2n

echo ""
echo "🥈 우수한 성능 (99.0%+ 성공률):"
tail -n +2 all_results.csv | awk -F',' '$3 >= 99.0 && $3 < 99.5 {print "   • " $10 ": " $1 "명, TPS " $2 ", " $3 "% 성공률"}' | sort -t':' -k2,2n

echo ""
echo "🥉 양호한 성능 (95.0%+ 성공률):"
tail -n +2 all_results.csv | awk -F',' '$3 >= 95.0 && $3 < 99.0 {print "   • " $10 ": " $1 "명, TPS " $2 ", " $3 "% 성공률"}' | sort -t':' -k2,2n

echo ""
echo "❌ 성능 문제 (95% 미만):"
tail -n +2 all_results.csv | awk -F',' '$3 < 95.0 {print "   • " $10 ": " $1 "명, TPS " $2 ", " $3 "% 성공률"}' | sort -t':' -k2,2n

echo ""
echo "🏆 최고 성능 기록:"
best_tps=$(tail -n +2 all_results.csv | awk -F',' 'BEGIN{max=0} {if($2>max && $3>=99) {max=$2; name=$10; threads=$1; rate=$3}} END {print threads "명(" name "), TPS " max ", " rate "% 성공률"}')
echo "   • $best_tps"

echo ""
echo "📂 상세 결과: all_results.csv"