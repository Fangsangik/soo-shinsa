package com.Soo_Shinsa.global.constant;

import lombok.Getter;

@Getter
public enum TossPayStatus {
    CANCELED("취소"),
    READY("준비"),
    DONE("완료");

    private String message;

    TossPayStatus(String message) {
        this.message = this.name();
    }

}
