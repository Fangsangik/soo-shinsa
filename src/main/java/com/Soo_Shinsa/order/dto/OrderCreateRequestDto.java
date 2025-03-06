package com.Soo_Shinsa.order.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class OrderCreateRequestDto {

    private Long cartId;

    @Builder
    public OrderCreateRequestDto(Long cartId) {
        this.cartId = cartId;
    }
}
