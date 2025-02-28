package com.Soo_Shinsa.global.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class JwtBlackListService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String BLACKLIST_PREFIX = "blackList";

    /**
     * 블랙 리스트 추가
     * 토큰 만료 시간과 동일한 TTL 설정 -> 메모리 낭비 줄임
     *
     * @param token
     * @param expiration
     */
    public void addBlackList(String token, long expiration) {
        redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, "BLACKLISTED", expiration, TimeUnit.MILLISECONDS);
    }

    public boolean isBlackListed(String token) {
        return redisTemplate.hasKey(BLACKLIST_PREFIX + token);
    }
}
