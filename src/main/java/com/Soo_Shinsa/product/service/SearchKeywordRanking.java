package com.Soo_Shinsa.product.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

/**
 * 인기 검색어 집계.
 *
 * 검색어를 Redis Sorted Set 에 카운팅한다. 날짜별 키를 쓰므로 오래된 집계는 TTL 로 사라진다.
 * 집계 실패가 검색 자체를 막으면 안 되므로 모든 오류는 삼킨다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchKeywordRanking {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Duration RETENTION = Duration.ofDays(3);
    private static final int MAX_KEYWORD_LENGTH = 50;

    private final StringRedisTemplate redis;
    private final Clock clock = Clock.systemDefaultZone();

    @Value("${app.search.ranking-enabled:true}")
    private boolean enabled = true;

    private String todayKey() {
        return "search:popular:" + LocalDate.now(clock).format(DAY);
    }

    /** 검색어 1회 집계. 너무 길거나 빈 값은 버린다. */
    public void record(String keyword) {
        if (!enabled || keyword == null) {
            return;
        }
        String term = keyword.trim();
        if (term.isEmpty() || term.length() > MAX_KEYWORD_LENGTH) {
            return;
        }
        try {
            String key = todayKey();
            redis.opsForZSet().incrementScore(key, term, 1);
            // 키가 새로 생겼을 때만 TTL 이 없다. 매번 걸어도 같은 값이라 무해하다.
            redis.expire(key, RETENTION);
        } catch (Exception e) {
            log.debug("인기 검색어 집계 실패: {}", e.getMessage());
        }
    }

    /** 오늘 많이 찾은 검색어. 조회 실패 시 빈 목록. */
    public List<String> popularKeywords(int limit) {
        if (!enabled) {
            return List.of();
        }
        int capped = Math.min(Math.max(limit, 1), 20);
        try {
            Set<ZSetOperations.TypedTuple<String>> top =
                    redis.opsForZSet().reverseRangeWithScores(todayKey(), 0, capped - 1L);
            if (top == null) {
                return List.of();
            }
            return top.stream().map(ZSetOperations.TypedTuple::getValue).toList();
        } catch (Exception e) {
            log.debug("인기 검색어 조회 실패: {}", e.getMessage());
            return List.of();
        }
    }
}
