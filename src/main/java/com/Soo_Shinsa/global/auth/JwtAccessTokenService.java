package com.Soo_Shinsa.global.auth;

import com.Soo_Shinsa.global.security.SecurityLogger;
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
        log.info("🟢 saveAccessToken() 호출됨 - Key: {}", SecurityLogger.maskRedisKey(ACCESS_TOKEN_PREFIX + email));
        String redisKey = ACCESS_TOKEN_PREFIX + email;
        log.info("🟢 saveAccessToken() 실행 - Key: {}, Email: {}, Expiration: {}", ACCESS_TOKEN_PREFIX + "***", SecurityLogger.maskEmail(email), expiration);

        // Redis에 같은 키가 존재하는지 확인
        // 저장 전에 Redis에 같은 키가 있는지 확인
        String existingToken = redisTemplate.opsForValue().get(redisKey);
        if (existingToken != null) {
            log.warn("⚠️ 기존에 저장된 AccessToken이 있음! 기존 값: {}", SecurityLogger.maskToken(existingToken));
        }

        redisTemplate.opsForValue().set(redisKey, email, expiration, TimeUnit.MILLISECONDS);
        log.info("✅ Redis에 AccessToken 저장 완료! Key: {}, Email: {}, Expiration: {}", ACCESS_TOKEN_PREFIX + "***", SecurityLogger.maskEmail(email), expiration);
    }


    public String getAccessToken(String email) {
        String redisKey = ACCESS_TOKEN_PREFIX + email;
        log.info("🔍 Redis에서 AccessToken 조회 - Key: {}", ACCESS_TOKEN_PREFIX + "***"); // 저장된 키 확인

        String storedValue = redisTemplate.opsForValue().get(redisKey);

        if (storedValue == null) {
            log.warn("⚠️ Redis에서 AccessToken을 찾을 수 없음! 조회하려는 Key: {}", ACCESS_TOKEN_PREFIX + "***");
        } else {
            log.info("🔍 Redis에서 AccessToken 조회 성공! Key: {}, Email: {}", ACCESS_TOKEN_PREFIX + "***", SecurityLogger.maskEmail(storedValue));
        }

        return storedValue;
    }


    public void deleteAccessToken(String email) {
        String redisKey = ACCESS_TOKEN_PREFIX + email;
        Boolean deleted = redisTemplate.delete(redisKey);
        if (Boolean.TRUE.equals(deleted)) {
            log.info("✅ Redis에서 AccessToken 삭제 완료: {}", ACCESS_TOKEN_PREFIX + "***");
        } else {
            log.warn("⚠️ Redis에서 삭제할 AccessToken이 없습니다: {}", ACCESS_TOKEN_PREFIX + "***");
        }
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
