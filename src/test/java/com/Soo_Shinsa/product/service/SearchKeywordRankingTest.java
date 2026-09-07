package com.Soo_Shinsa.product.service;

import com.Soo_Shinsa.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 인기 검색어 집계. Redis Sorted Set 동작이라 실제 Redis 위에서 확인한다. */
@SpringBootTest
class SearchKeywordRankingTest extends IntegrationTestSupport {

    @Autowired private SearchKeywordRanking ranking;
    @Autowired private StringRedisTemplate redis;

    @BeforeEach
    void setUp() {
        redis.keys("search:popular:*").forEach(redis::delete);
    }

    @Test
    void 많이_찾은_순서로_반환된다() {
        for (int i = 0; i < 5; i++) ranking.record("셔츠");
        for (int i = 0; i < 3; i++) ranking.record("자켓");
        ranking.record("팬츠");

        assertEquals(List.of("셔츠", "자켓", "팬츠"), ranking.popularKeywords(10));
    }

    @Test
    void limit_만큼만_반환된다() {
        for (String kw : List.of("a1", "b2", "c3", "d4")) ranking.record(kw);
        assertEquals(2, ranking.popularKeywords(2).size());
    }

    @Test
    void 빈값과_지나치게_긴_검색어는_집계하지_않는다() {
        ranking.record(null);
        ranking.record("   ");
        ranking.record("가".repeat(51));
        assertTrue(ranking.popularKeywords(10).isEmpty());
    }

    @Test
    void 검색어는_트림해서_같은_항목으로_센다() {
        ranking.record("  셔츠  ");
        ranking.record("셔츠");
        assertEquals(List.of("셔츠"), ranking.popularKeywords(10));
    }

    @Test
    void 집계_키에_만료가_걸려_오래된_집계는_사라진다() {
        ranking.record("셔츠");
        String key = redis.keys("search:popular:*").iterator().next();
        Long ttl = redis.getExpire(key);
        assertTrue(ttl != null && ttl > 0, "TTL 이 없으면 집계가 영원히 남는다: " + ttl);
    }

    @Test
    void 집계가_없으면_빈_목록이다() {
        assertFalse(ranking.popularKeywords(10).iterator().hasNext());
    }
}
