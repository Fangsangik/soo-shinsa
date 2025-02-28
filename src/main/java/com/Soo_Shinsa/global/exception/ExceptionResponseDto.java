package com.Soo_Shinsa.global.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public class ExceptionResponseDto {

    private static final ObjectMapper objectmapper = new ObjectMapper();

    private HttpStatus httpStatus;

    private String message;

    public ExceptionResponseDto(ErrorCode errorCode) {
        this.message = errorCode.getMessage();
        this.httpStatus = errorCode.getHttpStatus();
    }



}
