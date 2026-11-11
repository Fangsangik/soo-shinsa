package com.Soo_Shinsa.order.dto;

import com.Soo_Shinsa.constant.OrdersStatus;
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
    private String userName;
    private BigDecimal usedPoint;
    private BigDecimal earnedPoint;

    private List<OrderItemResponseDto> orderItems;


        public OrdersResponseDto(Long id, String orderId, BigDecimal totalPrice, OrdersStatus status, Long userId, String userName, List<OrderItemResponseDto> orderItems) {
        this(id, orderId, totalPrice, status, userId, userName, orderItems, null, null);
    }

    @Builder
    public OrdersResponseDto(Long id, String orderId, BigDecimal totalPrice, OrdersStatus status, Long userId, String userName, List<OrderItemResponseDto> orderItems, BigDecimal usedPoint, BigDecimal earnedPoint) {
        this.usedPoint = usedPoint;
        this.earnedPoint = earnedPoint;
        this.id = id;
        this.orderId = orderId;
        this.totalPrice = totalPrice;
        this.status = status;
        this.userId = userId;
        this.userName = userName;
        this.orderItems = orderItems;
    }

    public static OrdersResponseDto toDto(Orders orders) {
        return OrdersResponseDto.builder()
                .usedPoint(orders.getUsedPoint())
                .earnedPoint(orders.getEarnedPoint())
                .id(orders.getId())
                .orderId(orders.getOrderId())
                .totalPrice(orders.getTotalPrice())
                .status(orders.getStatus())
                .userId(orders.getUser().getUserId())
                .userName(orders.getUser().getName())
                .orderItems(orders.getOrderItems().stream().map(OrderItemResponseDto::toDto).collect(Collectors.toList()))
                .build();
    }

}
