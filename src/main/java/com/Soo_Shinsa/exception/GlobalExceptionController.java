package com.Soo_Shinsa.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

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

    @ExceptionHandler
    public ResponseEntity<ExceptionResponseDto> validationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getDefaultMessage())
                .findFirst().orElse("잘못된 요청입니다.");
        return ResponseEntity.badRequest().body(new ExceptionResponseDto(HttpStatus.BAD_REQUEST, message));
    }

    @ExceptionHandler
    public ResponseEntity<ExceptionResponseDto> authenticationException(AuthenticationException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ExceptionResponseDto(ErrorCode.NO_TOKEN));
    }

    @ExceptionHandler
    public ResponseEntity<ExceptionResponseDto> accessDeniedException(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ExceptionResponseDto(ErrorCode.NO_AUTHORITY));
    }
}
