package com.Soo_Shinsa.global.exception;

import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
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

    // ── 아래는 스프링이 던지는 요청 형식 오류들.
    // 핸들러가 없으면 whitelabel 형식({"timestamp":...})으로 나가서
    // 프런트가 매번 두 가지 에러 모양을 처리해야 했다.

    /** @Valid 검증 실패. 첫 번째 필드 메시지를 그대로 보여준다. */
    @ExceptionHandler
    public ResponseEntity<ExceptionResponseDto> methodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .orElse("입력값이 올바르지 않습니다.");
        return new ResponseEntity<>(new ExceptionResponseDto(HttpStatus.BAD_REQUEST, message), HttpStatus.BAD_REQUEST);
    }

    /** 본문이 없거나 JSON 이 깨진 경우. */
    @ExceptionHandler
    public ResponseEntity<ExceptionResponseDto> messageNotReadable(HttpMessageNotReadableException e) {
        return new ResponseEntity<>(
                new ExceptionResponseDto(HttpStatus.BAD_REQUEST, "요청 본문을 읽을 수 없습니다."),
                HttpStatus.BAD_REQUEST);
    }

    /** 쿼리 파라미터 타입 불일치 (?page=abc 같은 것). */
    @ExceptionHandler
    public ResponseEntity<ExceptionResponseDto> typeMismatch(MethodArgumentTypeMismatchException e) {
        return new ResponseEntity<>(
                new ExceptionResponseDto(HttpStatus.BAD_REQUEST, e.getName() + " 파라미터 형식이 올바르지 않습니다."),
                HttpStatus.BAD_REQUEST);
    }

    /** 필수 파라미터 누락. */
    @ExceptionHandler
    public ResponseEntity<ExceptionResponseDto> missingParameter(MissingServletRequestParameterException e) {
        return new ResponseEntity<>(
                new ExceptionResponseDto(HttpStatus.BAD_REQUEST, e.getParameterName() + " 파라미터가 필요합니다."),
                HttpStatus.BAD_REQUEST);
    }
}
