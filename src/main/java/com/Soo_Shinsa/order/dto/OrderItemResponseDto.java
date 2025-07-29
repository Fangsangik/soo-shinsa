package com.Soo_Shinsa.order.dto;

import com.Soo_Shinsa.global.constant.OrderItemStatus;
import com.Soo_Shinsa.order.model.OrderItem;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@NoArgsConstructor
@Getter
public class OrderItemResponseDto {

    private Long orderItemId;
    private Long productId;
    private String productName;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private OrderItemStatus status;
    private LocalDateTime cancelledAt;
    private String cancelReason;

    @Builder
    public OrderItemResponseDto(Long orderItemId, Long productId, String productName, Integer quantity, 
                               BigDecimal price, BigDecimal discountPrice, OrderItemStatus status, 
                               LocalDateTime cancelledAt, String cancelReason) {
        this.orderItemId = orderItemId;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.price = price;
        this.discountPrice = discountPrice;
        this.status = status;
        this.cancelledAt = cancelledAt;
        this.cancelReason = cancelReason;
    }


    public static OrderItemResponseDto toDto(OrderItem orderItem) {
        return OrderItemResponseDto.builder()
                .orderItemId(orderItem.getId())
                .productId(orderItem.getProduct().getId())
                .productName(orderItem.getProduct().getName())
                .quantity(orderItem.getQuantity())
                .price(orderItem.getPrice())
                .discountPrice(orderItem.getDiscountPrice())
                .status(orderItem.getStatus())
                .cancelledAt(orderItem.getCancelledAt())
                .cancelReason(orderItem.getCancelReason())
                .build();
    }
}
