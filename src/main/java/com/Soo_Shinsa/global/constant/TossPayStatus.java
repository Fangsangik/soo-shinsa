package com.Soo_Shinsa.global.constant;

import lombok.Getter;

@Getter
public enum TossPayStatus {
    PENDING("결제 대기"),
    PAYMENT("결제 완료"),
    CANCEL("결제 취소"),
    REFUND("환불 완료");

    private String message;

    TossPayStatus(String message) {
        this.message = this.name();
    }

}
