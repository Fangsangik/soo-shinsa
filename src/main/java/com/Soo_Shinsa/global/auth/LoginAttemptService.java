package com.Soo_Shinsa.global.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 로그인 브루트포스 제한. 이메일당 5회 실패하면 5분간 막는다.
 * Redis 가 죽어 있으면 로그인 자체는 막지 않는다(fail-open).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginAttemptService {

    private static final String KEY_PREFIX = "login:fail:";
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;

    public boolean isBlocked(String email) {
        try {
            String count = redisTemplate.opsForValue().get(KEY_PREFIX + email);
            return count != null && Integer.parseInt(count) >= MAX_ATTEMPTS;
        } catch (Exception e) {
            log.warn("로그인 시도 제한 확인 실패(무시): {}", e.getMessage());
            return false;
        }
    }

    public void recordFailure(String email) {
        try {
            String key = KEY_PREFIX + email;
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, WINDOW);
            }
        } catch (Exception e) {
            log.warn("로그인 실패 기록 실패(무시): {}", e.getMessage());
        }
    }

    public void clear(String email) {
        try {
            redisTemplate.delete(KEY_PREFIX + email);
        } catch (Exception e) {
            log.warn("로그인 실패 기록 초기화 실패(무시): {}", e.getMessage());
        }
    }
}
