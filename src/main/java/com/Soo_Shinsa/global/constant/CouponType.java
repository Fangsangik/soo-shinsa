package com.Soo_Shinsa.global.constant;

import lombok.Getter;

@Getter
public enum CouponType {
    SPECIFIC_PRODUCT("특정 상품 쿠폰"),
    SPECIFIC_BRAND("특정 브랜드 쿠폰"),
    UNIVERSAL("전체 적용 쿠폰");

    private String couponType;

    CouponType(String couponType) {
        this.couponType = couponType;
    }
}
