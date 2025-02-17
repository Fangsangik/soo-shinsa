package com.Soo_Shinsa.coupon.aop;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class CouponLockAspect {
    private final RedissonClient redissonClient;

    @Around("@annotation(couponLock)")
    public Object lock(ProceedingJoinPoint joinPoint, CouponLock couponLock) throws Throwable {
        String lockKey = couponLock.key();
        RLock lock = redissonClient.getLock(lockKey);

        boolean acquired = false;
        try {
            acquired = lock.tryLock(couponLock.waitTime(), couponLock.leaseTime(), TimeUnit.SECONDS);
            if (!acquired) {
                throw new IllegalStateException("현재 쿠폰 발급 요청이 많아 잠시 후 다시 시도해주세요.");
            }
            return joinPoint.proceed();
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
