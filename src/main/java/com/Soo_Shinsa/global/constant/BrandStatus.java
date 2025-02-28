package com.Soo_Shinsa.global.constant;

import lombok.Getter;

@Getter
public enum BrandStatus {
    APPLY("입점 신청"),
    OPEN("입점 승인"),
    REJECT("입점 거절");

    private String status;

    BrandStatus(String status) {
        this.status = this.name();
    }
}
