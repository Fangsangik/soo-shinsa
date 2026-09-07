package com.Soo_Shinsa.global.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionController {

    @ExceptionHandler
    public ResponseEntity<ExceptionResponseDto> duplicatedException(DuplicatedException e) {
        return new ResponseEntity<>(new ExceptionResponseDto(e.getErrorCode()), e.getErrorCode().getHttpStatus());
    }

    @ExceptionHandler
    public ResponseEntity<ExceptionResponseDto> internalServerException(InternalServerException e) {
        return new ResponseEntity<>(new ExceptionResponseDto(e.getErrorCode()), e.getErrorCode().getHttpStatus());
    }

    @ExceptionHandler
    public ResponseEntity<ExceptionResponseDto> invalidInputException(InvalidInputException e) {
        return new ResponseEntity<>(new ExceptionResponseDto(e.getErrorCode()), e.getErrorCode().getHttpStatus());
    }

    @ExceptionHandler
    public ResponseEntity<ExceptionResponseDto> notFoundException(NotFoundException e) {
        return new ResponseEntity<>(new ExceptionResponseDto(e.getErrorCode()), e.getErrorCode().getHttpStatus());
    }

    @ExceptionHandler
    public ResponseEntity<ExceptionResponseDto> noAuthorizedException(NoAuthorizedException e) {
        return new ResponseEntity<>(new ExceptionResponseDto(e.getErrorCode()), e.getErrorCode().getHttpStatus());
    }

    /**
     * 인증 실패(비로그인). Delegated*Handler 가 이리로 보내는데 핸들러가 없으면
     * resolver 가 null 을 돌려주고 응답이 200 빈 본문이 되어버린다.
     */
    @ExceptionHandler
    public ResponseEntity<ExceptionResponseDto> authenticationException(AuthenticationException e) {
        return new ResponseEntity<>(
                new ExceptionResponseDto(HttpStatus.UNAUTHORIZED, ErrorCode.NO_TOKEN.getMessage()),
                HttpStatus.UNAUTHORIZED);
    }

    /** 인가 실패(로그인은 했으나 권한 부족). */
    @ExceptionHandler
    public ResponseEntity<ExceptionResponseDto> accessDeniedException(AccessDeniedException e) {
        return new ResponseEntity<>(
                new ExceptionResponseDto(HttpStatus.FORBIDDEN, ErrorCode.NO_AUTHORITY.getMessage()),
                HttpStatus.FORBIDDEN);
    }
}
