package com.Soo_Shinsa.global.constant;

import lombok.Getter;

@Getter
public enum TargetType {
    BRAND("브랜드"),
    PRODUCT("상품"),
    REVIEW("후기");

    private String message;

    TargetType(String message) {
        this.message = this.name();
    }
}
