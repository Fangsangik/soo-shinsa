package com.Soo_Shinsa.order.dto;

import com.Soo_Shinsa.global.constant.OrdersStatus;
import com.Soo_Shinsa.order.model.Orders;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
@NoArgsConstructor
@Getter
public class OrdersResponseDto {
    private Long id;

    private String orderId;

    private BigDecimal totalPrice;

    private OrdersStatus status;

    private Long userId;

    private List<OrderItemResponseDto> orderItems;


    @Builder
    public OrdersResponseDto(Long id, String orderId, BigDecimal totalPrice, OrdersStatus status, Long userId, List<OrderItemResponseDto> orderItems) {
        this.id = id;
        this.orderId = orderId;
        this.totalPrice = totalPrice;
        this.status = status;
        this.userId = userId;
        this.orderItems = orderItems;
    }

    public static OrdersResponseDto toDto(Orders orders) {
        return OrdersResponseDto.builder()
                .id(orders.getId())
                .orderId(orders.getOrderId())
                .totalPrice(orders.getTotalPrice())
                .status(orders.getStatus())
                .userId(orders.getUser().getUserId())
                .orderItems(orders.getOrderItems().stream().map(OrderItemResponseDto::toDto).collect(Collectors.toList()))
                .build();
    }

}
