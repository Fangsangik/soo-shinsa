package com.Soo_Shinsa.cartitem.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ApplyCouponCartRequestDto {
    private Long couponId;

    @Builder
    public ApplyCouponCartRequestDto(Long couponId) {
        this.couponId = couponId;
    }
}
