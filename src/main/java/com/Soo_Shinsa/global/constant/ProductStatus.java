package com.Soo_Shinsa.global.constant;

import lombok.Getter;

@Getter
public enum ProductStatus {
    AVAILABLE("구매 가능"),
    SOLD_OUT("매진"),
    UNAVAILABLE("판매 중지"),
    DISCONTINUED ("할인중");

    private final String message;

    ProductStatus(String message) {
        this.message = message;
    }
}

