package com.Soo_Shinsa.order.service;

import com.Soo_Shinsa.global.constant.OrdersStatus;
import com.Soo_Shinsa.order.dto.OrderDateRequestDto;
import com.Soo_Shinsa.order.dto.OrdersResponseDto;
import com.Soo_Shinsa.user.model.User;
import org.springframework.data.domain.Page;

public interface OrdersService {
    OrdersResponseDto getOrderById(Long orderId, User user);
    Page<OrdersResponseDto> getAllByUserId(User user, OrderDateRequestDto dateRequestDto, int page, int size);
    OrdersResponseDto createSingleProductOrder(User user, Long productId, Integer quantity);
    OrdersResponseDto createAllOrderFromCart(User user);
    OrdersResponseDto createSingleOrderCartItem (User user, Long cartItemId);
    OrdersResponseDto createOrder (User user);

    OrdersResponseDto updateOrder (User user, Long orderId, OrdersStatus status);
}
