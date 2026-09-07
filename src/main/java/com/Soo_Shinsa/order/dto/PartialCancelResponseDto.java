package com.Soo_Shinsa.order.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class PartialCancelResponseDto {
    
    private Long orderId;
    private String orderNumber;
    private List<CancelledOrderItemDto> cancelledItems;
    private BigDecimal totalCancelledAmount;
    private BigDecimal refundAmount;
    private String refundStatus;
    private String message;

    @Getter
    @Builder
    public static class CancelledOrderItemDto {
        private Long orderItemId;
        private String productName;
        private String optionName;
        private Integer quantity;
        private BigDecimal price;
        private BigDecimal totalPrice;
        private String cancelReason;
    }
}