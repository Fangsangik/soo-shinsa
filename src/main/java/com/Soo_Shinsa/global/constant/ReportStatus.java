package com.Soo_Shinsa.global.constant;

import lombok.Getter;

@Getter
public enum ReportStatus {
    OPEN("신고 생성됨"),
    IN_PROGRESS("관리자가 검토 중"),
    APPROVED("신고 승인됨"),
    REJECTED("신고 반려됨"),
    RESOLVED("신고 처리 완료");

    private String message;

    ReportStatus(String message) {
        this.message = this.name();
    }
}
