package com.Soo_Shinsa.global.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 🚀 SooShinsa 캐시 설정
 * - Redis 기반 분산 캐시
 * - 조회 성능 최적화를 위한 캐시 전략
 */
@Configuration
@EnableCaching
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
public class CacheConfig {

    private final RedisConnectionFactory redisConnectionFactory;

    /**
     * Redis 캐시 매니저 설정
     */
    @Bean
    public CacheManager cacheManager() {
        RedisCacheConfiguration defaultConfig = createCacheConfiguration(Duration.ofMinutes(10));
        
        // 캐시별 개별 TTL 설정
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // 주문 요약 정보 - 10분 캐시
        cacheConfigurations.put("orderSummaries", createCacheConfiguration(Duration.ofMinutes(10)));
        cacheConfigurations.put("orderSummariesWithDate", createCacheConfiguration(Duration.ofMinutes(10)));
        
        // 주문 취소 통계 - 30분 캐시 (변경 빈도가 낮음)
        cacheConfigurations.put("orderCancelStats", createCacheConfiguration(Duration.ofMinutes(30)));
        
        // 사용자 주문 통계 - 1시간 캐시
        cacheConfigurations.put("userOrderStats", createCacheConfiguration(Duration.ofHours(1)));
        
        // 상품 정보 - 6시간 캐시 (변경 빈도가 매우 낮음)
        cacheConfigurations.put("products", createCacheConfiguration(Duration.ofHours(6)));
        
        // 사용자 정보 - 30분 캐시
        cacheConfigurations.put("users", createCacheConfiguration(Duration.ofMinutes(30)));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware() // 트랜잭션과 연동
                .build();
    }

    /**
     * Redis Template 설정
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        
        // 직렬화 설정
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper());
        
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(jsonSerializer);
        
        template.setDefaultSerializer(jsonSerializer);
        template.afterPropertiesSet();
        
        return template;
    }

    /**
     * 캐시별 설정 생성
     */
    private RedisCacheConfiguration createCacheConfiguration(Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues() // null 값 캐싱 비활성화
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper())))
                .computePrefixWith(cacheName -> "sooshinsa:cache:" + cacheName + ":");
    }

    /**
     * JSON 직렬화를 위한 ObjectMapper 설정
     */
    private ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // Java 8 시간 API 지원
        mapper.registerModule(new JavaTimeModule());
        
        // 모든 필드 접근 허용
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        
        // 타입 정보 포함 (역직렬화 시 클래스 정보 보존)
        // 역직렬화 가젯 방지: 애플리케이션 DTO 패키지만 허용
        mapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType("com.Soo_Shinsa.")
                        .allowIfSubType("java.util.")
                        .allowIfSubType("java.time.")
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL
        );
        
        return mapper;
    }

    /**
     * 캐시 키 생성기 (선택사항)
     */
    @Bean
    public org.springframework.cache.interceptor.KeyGenerator customKeyGenerator() {
        return (target, method, params) -> {
            StringBuilder sb = new StringBuilder();
            sb.append(target.getClass().getSimpleName()).append(":");
            sb.append(method.getName()).append(":");
            
            for (Object param : params) {
                if (param != null) {
                    sb.append(param.toString()).append(":");
                }
            }
            
            return sb.toString();
        };
    }
}