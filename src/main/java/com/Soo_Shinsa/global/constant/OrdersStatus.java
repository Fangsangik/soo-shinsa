package com.Soo_Shinsa.global.constant;

import lombok.Getter;

@Getter
public enum OrdersStatus {
    ORDERCANCEL("주문 취소"),
    ORDERCOMPLETED("주문 완료"),
    PENDING("결제 대기"),;
    private final String message;

    OrdersStatus(String message) {
        this.message = message;
    }
}
