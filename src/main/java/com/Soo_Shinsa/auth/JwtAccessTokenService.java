package com.Soo_Shinsa.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class JwtAccessTokenService {

    private final String ACCESS_TOKEN_PREFIX = "access_Token";
    private final RedisTemplate<String, String> redisTemplate;

    public void saveAccessToken(String accessToken, String username, long expiration) {
        String redisKey = ACCESS_TOKEN_PREFIX + accessToken;
        redisTemplate.opsForValue().set(redisKey, username, expiration, TimeUnit.MILLISECONDS);
    }

    public String getAccessToken (String accessToken) {
        return redisTemplate.opsForValue().get(ACCESS_TOKEN_PREFIX + accessToken);
    }

    /** 활동이 있을 때마다 유휴 만료 시간을 다시 늘린다 (슬라이딩 세션) */
    public void touch(String accessToken, long idleMillis) {
        redisTemplate.expire(ACCESS_TOKEN_PREFIX + accessToken, idleMillis, TimeUnit.MILLISECONDS);
    }

    public void deleteAccessToken(String accessToken) {
        redisTemplate.delete(ACCESS_TOKEN_PREFIX + accessToken);
    }
}
