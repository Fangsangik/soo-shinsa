package com.Soo_Shinsa.coupon.dto;

import com.Soo_Shinsa.global.constant.CouponType;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@NoArgsConstructor
public class CouponRequestDto {
    private Long couponId;
    private Long productId;
    private String couponName;
    private BigDecimal discountRate;
    private boolean isUsed;
    private CouponType couponType;
    private boolean isLimited;
    private Integer maxCount;
    private Integer issuedCount;

    private List<CouponBrandRelationDto> brands;

    @Builder
    public CouponRequestDto(Long couponId, Long productId, String couponName, BigDecimal discountRate, boolean isUsed, CouponType couponType, boolean isLimited, Integer maxCount, Integer issuedCount, List<CouponBrandRelationDto> brands) {
        this.couponId = couponId;
        this.productId = productId;
        this.couponName = couponName;
        this.discountRate = discountRate;
        this.isUsed = isUsed;
        this.couponType = couponType;
        this.isLimited = isLimited;
        this.maxCount = maxCount;
        this.issuedCount = issuedCount;
        this.brands = brands;
    }
}
