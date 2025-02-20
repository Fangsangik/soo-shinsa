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
        log.info("🟢 saveAccessToken() 호출됨 - Key: {}, 호출 스택: {}", ACCESS_TOKEN_PREFIX + accessToken, Thread.currentThread().getStackTrace());
        String redisKey = ACCESS_TOKEN_PREFIX + email;
        log.info("🟢 saveAccessToken() 실행 - Key: {}, Email: {}, Expiration: {}", redisKey, email, expiration);

        // Redis에 같은 키가 존재하는지 확인
        // 저장 전에 Redis에 같은 키가 있는지 확인
        String existingToken = redisTemplate.opsForValue().get(redisKey);
        if (existingToken != null) {
            log.warn("⚠️ 기존에 저장된 AccessToken이 있음! 기존 값: {}", existingToken);
        }

        redisTemplate.opsForValue().set(redisKey, email, expiration, TimeUnit.MILLISECONDS);
        log.info("✅ Redis에 AccessToken 저장 완료! Key: {}, Email: {}, Expiration: {}", redisKey, email, expiration);
    }


    public String getAccessToken(String email) {
        String redisKey = ACCESS_TOKEN_PREFIX + email;
        log.info("🔍 Redis에서 AccessToken 조회 - Key: {}", redisKey); // 저장된 키 확인

        String storedValue = redisTemplate.opsForValue().get(redisKey);

        if (storedValue == null) {
            log.warn("⚠️ Redis에서 AccessToken을 찾을 수 없음! 조회하려는 Key: {}", redisKey);
        } else {
            log.info("🔍 Redis에서 AccessToken 조회 성공! Key: {}, Email: {}", redisKey, storedValue);
        }

        return storedValue;
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
