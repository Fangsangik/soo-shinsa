package com.Soo_Shinsa.order.dto;

import com.Soo_Shinsa.global.constant.OrdersStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 주문 목록 조회용 경량 DTO
 * - 상세 정보는 제외하고 필수 정보만 포함
 * - N+1 문제 방지를 위한 투영(Projection) DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummaryDto {
    
    private Long orderId;
    private String orderNumber;
    private BigDecimal totalPrice;
    private OrdersStatus status;
    private LocalDateTime createdAt;
    
    // 주문 아이템 요약 정보
    private Long totalItems;
    private Long activeItems;
    private Long cancelledItems;
    private BigDecimal activeTotalAmount;
    private BigDecimal cancelledTotalAmount;
    
    // 주문 상태 구분
    private String orderCancelStatus; // ACTIVE, PARTIALLY_CANCELLED, FULLY_CANCELLED
    
    // 대표 상품 정보 (첫 번째 상품)
    private String firstProductName;
    private String orderDisplayName; // "나이키 신발 외 2개"
    
    /**
     * 주문 표시명 생성
     * 예: "나이키 에어맥스 외 2개" 또는 "아디다스 티셔츠"
     */
    public String getOrderDisplayName() {
        if (orderDisplayName != null) {
            return orderDisplayName;
        }
        
        if (firstProductName == null) {
            return "상품명 없음";
        }
        
        if (totalItems <= 1) {
            return firstProductName;
        }
        
        return firstProductName + " 외 " + (totalItems - 1) + "개";
    }
    
    /**
     * 취소 상태 한글 표시
     */
    public String getOrderCancelStatusKorean() {
        return switch (orderCancelStatus) {
            case "ACTIVE" -> "정상";
            case "PARTIALLY_CANCELLED" -> "부분취소";
            case "FULLY_CANCELLED" -> "전체취소";
            default -> "알 수 없음";
        };
    }
    
    /**
     * 주문 상태가 부분 취소인지 확인
     */
    public boolean isPartiallyCancelled() {
        return "PARTIALLY_CANCELLED".equals(orderCancelStatus);
    }
    
    /**
     * 취소된 아이템이 있는지 확인
     */
    public boolean hasCancelledItems() {
        return cancelledItems != null && cancelledItems > 0;
    }
}