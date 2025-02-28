package com.Soo_Shinsa.global.constant;

import lombok.Getter;

@Getter
public enum OrdersStatus {
    BEFOREPAYMENT("결제 전"),
    PAYMENTCOMPLETED("결제 완료"),
    ONDELIVERY("배송 시작"),
    DELIVERYCOMPLETED("배송 완료"),
    ORDERCOMPLETED("주문 완료"),
    CANCEL("취소 신청"),
    RETURN("취소 완료"),
    REQUESTFORRETURN("반품 신청"),
    RETURNCOMPLETED("반품 완료"),
    CANCELRETURN("반품 취소"),
    APPLYFOREXCHANGE("교환 신청"),
    COMPLETEDEXCHANGE("교환 완료"),
    EXCHANGECANCELLATION("교환 취소");

    private final String message;

    OrdersStatus(String message) {
        this.message = message;
    }
}
