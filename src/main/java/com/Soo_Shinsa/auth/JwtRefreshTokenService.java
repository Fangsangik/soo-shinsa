package com.Soo_Shinsa.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class JwtRefreshTokenService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String REFRESH_TOKEN_PREFIX = "refreshToken";

    public void saveRefreshToken(String username, String token, long expiration) {
        redisTemplate.opsForValue().set(REFRESH_TOKEN_PREFIX + username, token, expiration, TimeUnit.MILLISECONDS);
    }


    public String getRefreshToken(String username) {
        return redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + username);
    }

    public void deleteRefreshToken(String username) {
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + username);
    }
}
