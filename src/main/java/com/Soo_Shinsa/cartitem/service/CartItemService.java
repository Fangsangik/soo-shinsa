package com.Soo_Shinsa.cartitem.service;

import com.Soo_Shinsa.cartitem.dto.*;
import com.Soo_Shinsa.user.model.User;
import org.springframework.data.domain.Page;

public interface CartItemService {
    CartItemResponseDto create(User user, CartItemRequestDto requestDto);
    CartItemResponseDto findById(Long cartId, User user);
    CartItemResponseDto update(User user, CartItemUpdateRequestDto requestDto);
    Page<CartItemResponseDto> findByAll(User user, CartItemDateRequestDto requestDto, int page, int size);
    void delete(Long cartId, User user);
    ApplyCouponCartResponseDto applyCoupon(Long cartId, ApplyCouponCartRequestDto requestDto, User user);
}