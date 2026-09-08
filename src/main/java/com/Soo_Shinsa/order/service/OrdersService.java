package com.Soo_Shinsa.order.service;

import com.Soo_Shinsa.global.constant.OrdersStatus;
import com.Soo_Shinsa.order.dto.*;
import com.Soo_Shinsa.user.model.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.data.domain.Page;

public interface OrdersService {
    OrdersResponseDto getOrderById(Long orderId, User user);
    Page<OrdersResponseDto> getAllByUserId(User user, OrderDateRequestDto dateRequestDto, int page, int size);
    OrdersResponseDto createSingleProductOrder(User user, Long productOptionId, Integer quantity, java.math.BigDecimal usePoint);
    OrdersResponseDto createAllOrderFromCart(User user);
    OrdersResponseDto createSingleOrderCartItem(User user, OrderCreateRequestDto requestDto);
    void cancelOrder(User user, Long orderId) throws JsonProcessingException;

    /**
     * 결제되지 않은 채 방치된 주문을 취소하고 재고를 되돌린다.
     * @return 되돌린 주문 수
     */
    int expirePendingOrders(int batchSize);
    PartialCancelResponseDto partialCancelOrder(User user, Long orderId, PartialCancelRequestDto requestDto) throws JsonProcessingException;
    OrdersResponseDto updateOrder (User user, Long orderId, OrdersStatus status);

    // 🚀 성능 최적화된 메소드들
    OrdersResponseDto getOrderByIdOptimized(Long orderId, User user);
    Page<OrderSummaryDto> getOrderSummariesByUserId(User user, OrderDateRequestDto dateRequestDto, int page, int size);
    Page<OrdersResponseDto> getAllByUserIdOptimized(User user, OrderDateRequestDto dateRequestDto, int page, int size);
}
