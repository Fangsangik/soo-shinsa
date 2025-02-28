package com.Soo_Shinsa.global.constant;

public enum UserStatus {
    ACTIVE,
    UN_ACTIVE,
    DELETED,;

    public static UserStatus of(String status) {
        return UserStatus.valueOf(status.toUpperCase());
    }
}
