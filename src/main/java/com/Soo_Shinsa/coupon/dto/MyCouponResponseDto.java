package com.Soo_Shinsa.coupon.dto;

import com.Soo_Shinsa.coupon.model.CouponUser;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class MyCouponResponseDto {
    private Long couponUserId;
    private Long couponId;
    private String couponName;
    private String couponCode;
    private BigDecimal discountRate;
    private LocalDate expirationDate;
    private boolean used;
    private LocalDate usedAt;

    public static MyCouponResponseDto toDto(CouponUser cu) {
        return MyCouponResponseDto.builder()
                .couponUserId(cu.getId()).couponId(cu.getCoupon().getId())
                .couponName(cu.getCoupon().getCouponName()).couponCode(cu.getCoupon().getCouponCode())
                .discountRate(cu.getCoupon().getDiscountRate()).expirationDate(cu.getCoupon().getExpirationDate())
                .used(cu.isUsed()).usedAt(cu.getUsedAt())
                .build();
    }
}
