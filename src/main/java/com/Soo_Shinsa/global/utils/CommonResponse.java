package com.Soo_Shinsa.global.utils;

import lombok.Getter;

@Getter
public class CommonResponse<T> {

    private final String message;
    private final T data;

    public CommonResponse(String message, T data) {
        this.message = message;
        this.data = data;
    }
    public CommonResponse(String message) {
        this.message = message;
        this.data = null;
    }
}
