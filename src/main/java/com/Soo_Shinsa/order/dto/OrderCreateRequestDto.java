package com.Soo_Shinsa.order.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class OrderCreateRequestDto {

    private Long cartId;

    /** 사용할 포인트(선택). 결제 금액에서 차감된다. */
    private java.math.BigDecimal usePoint;

    @Builder
    public OrderCreateRequestDto(Long cartId) {
        this.cartId = cartId;
    }
}
