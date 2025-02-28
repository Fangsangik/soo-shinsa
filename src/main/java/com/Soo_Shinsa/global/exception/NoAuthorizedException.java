package com.Soo_Shinsa.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NoAuthorizedException extends RuntimeException {
    private final ErrorCode errorCode;
}
