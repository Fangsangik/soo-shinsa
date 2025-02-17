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

    public void deleteAccessToken(String accessToken) {
        redisTemplate.delete(ACCESS_TOKEN_PREFIX + accessToken);
    }
}
