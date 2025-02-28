package com.Soo_Shinsa.global.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PeriodType {
    DAY("%Y-%m-%d"),
    MONTH("%Y-%m"),
    YEAR("%Y");

    private final String dateFormat;

    @JsonCreator
    public static PeriodType from(String s) {
        return PeriodType.valueOf(s.toUpperCase());
    }
}
