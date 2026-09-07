package com.Soo_Shinsa.coupon.dto;

import com.Soo_Shinsa.global.constant.CouponType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 쿠폰 정의 생성 요청.
 * 발급과 달리 couponId 를 받지 않는다 — 없는 id 로 요청이 오면 새로 만들어버리던 문제의 원인이었다.
 */
@Getter
@NoArgsConstructor
public class CouponCreateRequestDto {

    @NotBlank(message = "쿠폰 이름은 필수입니다.")
    private String couponName;

    @NotNull(message = "할인율은 필수입니다.")
    private BigDecimal discountRate;

    private CouponType couponType;

    @NotNull(message = "발급 정원은 필수입니다.")
    @Positive(message = "발급 정원은 1 이상이어야 합니다.")
    private Integer maxCount;

    /** 이 쿠폰을 쓸 수 있는 브랜드들 */
    private List<CouponBrandRelationDto> brands;

    @Builder
    public CouponCreateRequestDto(String couponName, BigDecimal discountRate, CouponType couponType,
                                  Integer maxCount, List<CouponBrandRelationDto> brands) {
        this.couponName = couponName;
        this.discountRate = discountRate;
        this.couponType = couponType;
        this.maxCount = maxCount;
        this.brands = brands;
    }
}
