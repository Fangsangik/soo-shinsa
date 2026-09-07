package com.Soo_Shinsa.coupon.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 선착순 쿠폰 잔여 수량 선차단.
 *
 * 정확성은 DB 의 increaseIssuedCount 가 이미 보장한다. 이 클래스의 목적은 부하 차단이다.
 * 정원 10개에 요청 10,000건이 몰리면 9,990건은 DB 커넥션을 잡지 않고 여기서 끝난다.
 *
 * 그래서 Redis 가 죽거나 값이 틀려도 초과 발급은 일어나지 않는다.
 * 통과시킨 요청은 어차피 DB WHERE 조건을 다시 통과해야 한다. 장애 시에는 그냥 통과시킨다(fail-open).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponStockGuard {

    private final StringRedisTemplate redis;

    /** 문제가 생기면 끌 수 있게 둔다. 꺼도 정확성은 DB 가 지킨다. */
    @Value("${app.coupon.prefilter-enabled:true}")
    private boolean enabled = true;

    private static String key(Long couponId) {
        return "coupon:" + couponId + ":remaining";
    }

    /**
     * 잔여 수량을 하나 선점한다.
     *
     * @param remainingInDb 카운터가 아직 없을 때 초기값으로 쓸 DB 기준 잔여 수량
     * @return 선점 성공 여부. false 면 정원 소진이므로 DB 까지 갈 필요가 없다.
     */
    public boolean tryAcquire(Long couponId, int remainingInDb) {
        if (!enabled) {
            return true;
        }
        try {
            String k = key(couponId);
            // 없을 때만 DB 잔여 수량으로 초기화 (SETNX)
            redis.opsForValue().setIfAbsent(k, String.valueOf(Math.max(remainingInDb, 0)));

            Long left = redis.opsForValue().decrement(k);
            if (left == null) {
                return true;
            }
            if (left < 0) {
                redis.opsForValue().increment(k); // 음수로 계속 내려가지 않게 되돌린다
                return false;
            }
            return true;
        } catch (Exception e) {
            // Redis 장애가 발급 자체를 막으면 안 된다. 정확성은 DB 가 지킨다.
            log.warn("쿠폰 선차단 건너뜀 (Redis 오류): {}", e.getMessage());
            return true;
        }
    }

    /** 선점했지만 DB 단계에서 실패한 경우 되돌린다. */
    public void release(Long couponId) {
        if (!enabled) {
            return;
        }
        try {
            redis.opsForValue().increment(key(couponId));
        } catch (Exception e) {
            // 되돌리기 실패는 카운터를 실제보다 작게 만들 뿐이라 초과 발급으로 이어지지 않는다.
            log.warn("쿠폰 선차단 복구 실패: {}", e.getMessage());
        }
    }

    /** 정원이 바뀌었을 때 카운터를 버린다. 다음 요청이 DB 값으로 다시 초기화한다. */
    public void reset(Long couponId) {
        try {
            redis.delete(key(couponId));
        } catch (Exception e) {
            log.warn("쿠폰 선차단 초기화 실패: {}", e.getMessage());
        }
    }
}
