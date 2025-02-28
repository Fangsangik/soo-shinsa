package com.Soo_Shinsa.global.exception;

import lombok.Getter;

@Getter
public class InvalidInputException extends RuntimeException {
    private final ErrorCode errorCode;

    public InvalidInputException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
