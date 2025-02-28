package com.Soo_Shinsa.global.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GradeType {
    ROOKIE("ROOKIE"),
    BRONZE("BRONZE"),
    SILVER("SILVER"),
    GOLD("GOLD");

    private final String name;
}
