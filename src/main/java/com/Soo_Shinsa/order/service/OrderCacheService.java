package com.Soo_Shinsa.order.service;

import com.Soo_Shinsa.order.dto.OrderSummaryDto;
import com.Soo_Shinsa.order.model.Orders;
import com.Soo_Shinsa.order.repository.OrdersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 주문 관련 캐싱 서비스
 * - 자주 조회되는 주문 통계 정보를 캐싱
 * - 부분 취소 시 관련 캐시 무효화
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCacheService {
    
    private final OrdersRepository ordersRepository;

    /**
     * 사용자별 주문 요약 정보 캐싱
     * 키: "orderSummary:userId:page:size"
     * TTL: 10분
     */
    @Cacheable(
        value = "orderSummaries", 
        key = "#userId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize",
        unless = "#result.isEmpty()"
    )
    @Transactional(readOnly = true)
    public Page<OrderSummaryDto> getCachedOrderSummariesByUserId(Long userId, Pageable pageable) {
        log.info("🔄 주문 요약 캐시 미스 - 데이터베이스에서 조회: userId={}", userId);
        
        Page<Orders> ordersPage = ordersRepository.findOrderSummariesByUserId(userId, pageable);
        return ordersPage.map(this::convertToOrderSummaryDto);
    }

    /**
     * 날짜 범위 포함 주문 요약 정보 캐싱
     */
    @Cacheable(
        value = "orderSummariesWithDate",
        key = "#userId + ':' + #startDate + ':' + #endDate + ':' + #pageable.pageNumber + ':' + #pageable.pageSize",
        unless = "#result.isEmpty()"
    )
    @Transactional(readOnly = true)
    public Page<OrderSummaryDto> getCachedOrderSummariesByUserIdAndDate(
            Long userId, 
            LocalDateTime startDate, 
            LocalDateTime endDate, 
            Pageable pageable) {
        
        log.info("🔄 날짜별 주문 요약 캐시 미스 - 데이터베이스에서 조회: userId={}, startDate={}, endDate={}", 
                userId, startDate, endDate);
        
        Page<Orders> ordersPage = ordersRepository.findOrderSummariesByUserIdAndDate(userId, startDate, endDate, pageable);
        return ordersPage.map(this::convertToOrderSummaryDto);
    }

    /**
     * 주문별 취소 통계 캐싱
     */
    @Cacheable(
        value = "orderCancelStats",
        key = "#orderId",
        unless = "#result == null"
    )
    @Transactional(readOnly = true)
    public Map<String, Object> getCachedOrderCancelStats(Long orderId) {
        log.info("🔄 주문 취소 통계 캐시 미스 - 계산 중: orderId={}", orderId);
        
        // 실제 통계 계산 로직
        return calculateOrderCancelStats(orderId);
    }

    /**
     * 사용자별 전체 주문 통계 캐싱
     */
    @Cacheable(
        value = "userOrderStats",
        key = "#userId",
        unless = "#result == null"
    )
    @Transactional(readOnly = true)
    public Map<String, Object> getCachedUserOrderStats(Long userId) {
        log.info("🔄 사용자 주문 통계 캐시 미스 - 계산 중: userId={}", userId);
        
        return calculateUserOrderStats(userId);
    }

    /**
     * 부분 취소 시 관련 캐시 무효화
     */
    @Caching(evict = {
        @CacheEvict(value = "orderSummaries", key = "#userId + ':*'", allEntries = true),
        @CacheEvict(value = "orderSummariesWithDate", key = "#userId + ':*'", allEntries = true),
        @CacheEvict(value = "orderCancelStats", key = "#orderId"),
        @CacheEvict(value = "userOrderStats", key = "#userId")
    })
    public void evictOrderCaches(Long orderId, Long userId) {
        log.info("🗑️ 주문 관련 캐시 무효화 완료 - orderId: {}, userId: {}", orderId, userId);
    }

    /**
     * 특정 사용자의 주문 캐시만 무효화
     */
    @Caching(evict = {
        @CacheEvict(value = "orderSummaries", allEntries = true),
        @CacheEvict(value = "orderSummariesWithDate", allEntries = true),
        @CacheEvict(value = "userOrderStats", key = "#userId")
    })
    public void evictUserOrderCaches(Long userId) {
        log.info("🗑️ 사용자 주문 캐시 무효화 완료 - userId: {}", userId);
    }

    /**
     * 전체 주문 캐시 무효화 (관리자용)
     */
    @Caching(evict = {
        @CacheEvict(value = "orderSummaries", allEntries = true),
        @CacheEvict(value = "orderSummariesWithDate", allEntries = true),
        @CacheEvict(value = "orderCancelStats", allEntries = true),
        @CacheEvict(value = "userOrderStats", allEntries = true)
    })
    public void evictAllOrderCaches() {
        log.info("🗑️ 모든 주문 캐시 무효화 완료");
    }

    /**
     * 주문별 취소 통계 계산
     */
    private Map<String, Object> calculateOrderCancelStats(Long orderId) {
        // 실제 구현에서는 복잡한 집계 쿼리 실행
        return Map.of(
            "orderId", orderId,
            "totalItems", 0L,
            "cancelledItems", 0L,
            "activeItems", 0L,
            "cancelledAmount", 0.0,
            "activeAmount", 0.0,
            "cancelStatus", "ACTIVE"
        );
    }

    /**
     * 사용자별 전체 주문 통계 계산
     */
    private Map<String, Object> calculateUserOrderStats(Long userId) {
        // 실제 구현에서는 사용자의 전체 주문 통계 계산
        return Map.of(
            "userId", userId,
            "totalOrders", 0L,
            "totalSpent", 0.0,
            "cancelledOrders", 0L,
            "cancelledAmount", 0.0,
            "averageOrderAmount", 0.0,
            "lastOrderDate", LocalDateTime.now()
        );
    }
    
    /**
     * Orders를 OrderSummaryDto로 변환
     */
    private OrderSummaryDto convertToOrderSummaryDto(Orders order) {
        // 간단한 변환 로직
        return OrderSummaryDto.builder()
            .orderId(order.getId())
            .orderNumber(order.getOrderId())
            .totalPrice(order.getTotalPrice())
            .status(order.getStatus())
            .createdAt(order.getCreatedAt().toLocalDateTime())
            .totalItems(0L) // 기본값
            .activeItems(0L) // 기본값
            .cancelledItems(0L) // 기본값
            .activeTotalAmount(order.getTotalPrice()) // 기본값
            .cancelledTotalAmount(java.math.BigDecimal.ZERO) // 기본값
            .orderCancelStatus("ACTIVE") // 기본값
            .firstProductName("상품명") // 기본값
            .build();
    }
}