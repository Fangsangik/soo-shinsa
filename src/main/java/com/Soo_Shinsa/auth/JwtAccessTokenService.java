package com.Soo_Shinsa.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtAccessTokenService {

    private final String ACCESS_TOKEN_PREFIX = "access_Token";
    private final RedisTemplate<String, String> redisTemplate;

    public void saveAccessToken(String accessToken, String email, long expiration) {
        String redisKey = ACCESS_TOKEN_PREFIX + accessToken;
        redisTemplate.opsForValue().set(redisKey, email, expiration, TimeUnit.MILLISECONDS);
    }

    public String getAccessToken (String accessToken) {
        return redisTemplate.opsForValue().get(ACCESS_TOKEN_PREFIX + accessToken);
    }

    public void deleteAllAccessTokens() {
        Set<String> keys = redisTemplate.keys("access_Token*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("✅ Redis에서 모든 AccessToken 삭제 완료: {}", keys.size());
        } else {
            log.warn("⚠️ Redis에서 삭제할 AccessToken이 없습니다.");
        }
    }

}
