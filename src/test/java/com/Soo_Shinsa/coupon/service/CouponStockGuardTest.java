package com.Soo_Shinsa.coupon.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * 선착순 선차단 검증.
 * 정확성은 DB 가 보장하므로 여기서 보는 것은 "정원 초과 요청이 DB 까지 가지 않는가" 이다.
 */
@ExtendWith(MockitoExtension.class)
class CouponStockGuardTest {

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ValueOperations<String, String> ops;

    @InjectMocks
    private CouponStockGuard guard;

    /** DECR/INCR/SETNX 를 흉내내는 인메모리 카운터 */
    private final AtomicLong counter = new AtomicLong();
    private final AtomicInteger initCount = new AtomicInteger();

    @BeforeEach
    void setUp() {
        lenient().when(redis.opsForValue()).thenReturn(ops);
        lenient().when(ops.setIfAbsent(anyString(), anyString())).thenAnswer(inv -> {
            if (initCount.getAndIncrement() == 0) {
                counter.set(Long.parseLong(inv.getArgument(1)));
                return true;
            }
            return false;
        });
        lenient().when(ops.decrement(anyString())).thenAnswer(inv -> counter.decrementAndGet());
        lenient().when(ops.increment(anyString())).thenAnswer(inv -> counter.incrementAndGet());
    }

    @Test
    void 정원만큼만_통과시킨다() {
        int passed = 0;
        for (int i = 0; i < 50; i++) {
            if (guard.tryAcquire(1L, 10)) {
                passed++;
            }
        }
        assertEquals(10, passed, "정원을 넘어선 요청은 DB 까지 가면 안 된다");
    }

    @Test
    void 동시_요청에서도_정원을_넘지_않는다() throws Exception {
        int threads = 100;
        ExecutorService pool = Executors.newFixedThreadPool(16);
        AtomicInteger passed = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                if (guard.tryAcquire(1L, 10)) {
                    passed.incrementAndGet();
                }
            });
        }
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));

        assertEquals(10, passed.get());
    }

    @Test
    void 마감_후_되돌리면_다시_한_건_통과한다() {
        for (int i = 0; i < 10; i++) {
            guard.tryAcquire(1L, 10);
        }
        assertFalse(guard.tryAcquire(1L, 10), "마감 상태");

        guard.release(1L); // DB 단계 실패로 슬롯 반납
        assertTrue(guard.tryAcquire(1L, 10), "반납한 슬롯은 다시 쓸 수 있어야 한다");
    }

    @Test
    void Redis_장애면_통과시킨다() {
        when(redis.opsForValue()).thenThrow(new RuntimeException("redis down"));
        assertTrue(guard.tryAcquire(1L, 10), "선차단은 부하 대책일 뿐, 장애가 발급을 막으면 안 된다");
    }
}
