package com.Soo_Shinsa.order.service;

import com.Soo_Shinsa.order.dto.OrderDateRequestDto;
import com.Soo_Shinsa.order.dto.OrderItemResponseDto;
import com.Soo_Shinsa.user.model.User;
import org.springframework.data.domain.Page;

public interface OrderItemService {
    OrderItemResponseDto findById(Long orderItemsId,User user);
    Page<OrderItemResponseDto> findByAll(User user, OrderDateRequestDto dateRequestDto, int page, int size);

    OrderItemResponseDto update(Long orderItemsId,Integer quantity,User user);
    OrderItemResponseDto delete(Long orderItemsId,User user);
}
