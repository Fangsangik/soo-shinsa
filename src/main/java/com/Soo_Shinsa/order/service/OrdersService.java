package com.Soo_Shinsa.order.service;

import com.Soo_Shinsa.constant.OrdersStatus;
import com.Soo_Shinsa.order.dto.OrderDateRequestDto;
import com.Soo_Shinsa.order.dto.OrdersResponseDto;
import com.Soo_Shinsa.user.model.User;
import org.springframework.data.domain.Page;

public interface OrdersService {
    OrdersResponseDto getOrderById(Long orderId, User user);
    Page<OrdersResponseDto> getAllByUserId(User user, OrderDateRequestDto dateRequestDto, int page, int size);
    OrdersResponseDto createSingleProductOrder(User user, Long productId, Integer quantity, java.math.BigDecimal usePoint);
    default OrdersResponseDto createSingleProductOrder(User user, Long productId, Integer quantity) { return createSingleProductOrder(user, productId, quantity, null); }
    OrdersResponseDto createAllOrderFromCart(User user, java.math.BigDecimal usePoint);
    default OrdersResponseDto createAllOrderFromCart(User user) { return createAllOrderFromCart(user, null); }
    OrdersResponseDto createSingleOrderCartItem (User user, Long cartItemId);
    OrdersResponseDto createOrder (User user);

    OrdersResponseDto updateOrder (User user, Long orderId, OrdersStatus status);

    Page<OrdersResponseDto> getAllForStaff(User user, OrdersStatus status, OrderDateRequestDto dateRequestDto, int page, int size);
}
