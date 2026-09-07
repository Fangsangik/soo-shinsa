package com.Soo_Shinsa.global.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderItemStatus {
    ORDERED("주문됨"),
    CANCELLED("취소됨"),
    REFUNDED("환불됨");

    private final String description;
}