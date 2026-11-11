package com.Soo_Shinsa.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** 모든 오류 응답의 형태 {httpStatus, code, message}. 프론트는 code 로 분기하고 message 를 그대로 사용자에게 보여준다. */
@Getter
public class ExceptionResponseDto {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    public ExceptionResponseDto(ErrorCode errorCode) {
        this.httpStatus = errorCode.getHttpStatus();
        this.code = errorCode.name();
        this.message = errorCode.getMessage();
    }

    public ExceptionResponseDto(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.code = "VALIDATION";
        this.message = message;
    }

    /** 필터처럼 @ControllerAdvice 밖에서 같은 형태로 응답해야 할 때 */
    public static void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(new ExceptionResponseDto(errorCode)));
    }
}
