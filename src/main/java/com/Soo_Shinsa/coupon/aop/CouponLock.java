package com.Soo_Shinsa.coupon.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CouponLock {
    String key(); // 락 키
    long waitTime() default 10; // 대기 시간
    long leaseTime() default 10; // 락 유지 시간
 }
