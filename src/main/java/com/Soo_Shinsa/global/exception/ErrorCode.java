package com.Soo_Shinsa.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    //InvalidInputException
    //비밀번호가 틀렸을 때 출력하는 오류 메시지
    WRONG_PASSWORD("비밀 번호가 틀렸습니다.", HttpStatus.BAD_REQUEST),
    //아이디 비밀번호가 잘못됨
    DIFFERENT_EMAIL_PASSWORD("이메일 혹은 비밀번호가 잘못되었습니다.", HttpStatus.BAD_REQUEST),
    //탈퇴한 이메일로 가입을 시도할 때 출력하는 오류 메시지
    EMAIL_DELETED("삭제된 이메일 입니다.", HttpStatus.BAD_REQUEST),
    // 이미 삭제된 사용자가 조회 되었을 때 출력하는 오류 메시지
    DELETED_USER("이미 삭제된 회원입니다.", HttpStatus.BAD_REQUEST),

    // Exception
    NOT_FOUND_USER("유저를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    NOT_FOUND_CART("카트를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    NOT_FOUND_PRODUCT("상품를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    NOT_FOUND_PRODUCT_OPTION("상품옵션을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    NOT_FOUND_ORDER("오더를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    NOT_FOUND_ORDER_OPTION("오더옵션을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    NOT_FOUND_REVIEW("리뷰를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    NOT_FOUND_REPORT("리폿을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    NOT_FOUND_BRAND("브랜드를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    NOT_FOUND_CATEGORY("카테고리를 찾을 수 없습니다.",HttpStatus.NOT_FOUND),
    //잘못된 요청
    WRONG_REQUEST("지원하지 않는 요청입니다.", HttpStatus.SERVICE_UNAVAILABLE),

    // DuplicatedException
    //중복된 이메일로 가일 할 때 출력하는 오류 메시지
    EMAIL_EXIST("중복된 아이디 입니다.", HttpStatus.BAD_REQUEST),

    // NoAuthorizedException
    //로그인이 안 되었을 떄 출력하는 오류 메시지
    NO_TOKEN("로그인이 필요합니다.", HttpStatus.UNAUTHORIZED),
    //이미 로그인이 되어있을 때 출력하는 오류 메시지
    ALREADY_LOGIN("이미 로그인이 되어있습니다.", HttpStatus.UNAUTHORIZED),
    //권한이 없는 사용자가 수정, 삭제를 하려고 할 때
    NO_AUTHOR_CHANGE("수정, 삭제는 작성자만 할 수 있습니다.", HttpStatus.UNAUTHORIZED),
    NO_AUTHOR_READ_ONLY("읽기 전용 권한은 수정, 삭제를 할 수 없습니다.", HttpStatus.UNAUTHORIZED),
    FAIl_IMAGE_DELETE("로컬 파일 삭제에 실패했습니다.", HttpStatus.BAD_REQUEST),
    MAX_10MB_SIZE("파일 크기가 10MB를 초과할 수 없습니다.", HttpStatus.BAD_REQUEST),
    NO_EXTENSION("확장자가 없는 파일은 업로드할 수 없습니다.", HttpStatus.BAD_REQUEST),
    NO_AUTHORITY("권헌이 없습니다.", HttpStatus.UNAUTHORIZED),
    NO_CONNECT("수정 또는 삭제할 권한이 없습니다.", HttpStatus.UNAUTHORIZED),
    SELECT_COLOR_OR_SIZE("색상과 사이즈 중 하나는 필수입니다.",HttpStatus.BAD_REQUEST),
    FAIL_UPLOAD_FILE("이미지 업로드 중 오류가 발생했습니다.",HttpStatus.BAD_REQUEST),
    CAN_NOT_USE_PRODUCT("해당 상품을 사용할 수 없습니다.",HttpStatus.BAD_REQUEST),
    NOT_ACTIVE_USER("활동이 정지된 회원 입니다", HttpStatus.BAD_REQUEST),
    DUPLICATED_COUPON("이미 쿠폰을 발급 받으셨습니다", HttpStatus.BAD_REQUEST),
    NOT_FOUND_COUPON_COUNT("발급 가능한 쿠폰이 없습니다.", HttpStatus.NOT_FOUND),
    NOT_APPROPRIATE_COUPON("해당 쿠폰은 여기에 사용할 수 없습니다.", HttpStatus.BAD_REQUEST),
    ALREADY_USED_COUPON("이미 사용된 쿠폰 입니다.", HttpStatus.BAD_REQUEST),
    EXPIRED_COUPON("기간이 만료된 쿠폰입니다.", HttpStatus.BAD_REQUEST),
    NOT_APPROPRIATE_PERCENTAGE("할인율은 0%에서 100% 사이여야 합니다.", HttpStatus.BAD_REQUEST),
    NEED_ID("Id 값이 필요합니다", HttpStatus.BAD_REQUEST),
    INVALID_CART_ITEM_PRICE("적합하지 않은 금액 입니다.", HttpStatus.BAD_REQUEST),
    COUPON_OUT_OF_STOCK("더이상 쿠폰이 존재하지 않습니다.", HttpStatus.BAD_REQUEST),
    NOT_FOUND_COUPON("쿠폰을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    ONLY_BEFORE_PAYMENT("결제 전 상태만 이용할 수 있습니다.", HttpStatus.BAD_REQUEST),
    NOT_FOUND_SUB_CATEGORY("해당 카테고리를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    INVALID_REFRESH_TOKEN("유효하지 않은 토큰 입니다.", HttpStatus.BAD_REQUEST),
    ACCESS_TOKEN_VALID("아직 AccessToken이 유효 합니다.", HttpStatus.MULTI_STATUS),
    DUPLICATED_KAKAO_ACCOUNT("이미 계정이 존재 합니다", HttpStatus.BAD_REQUEST),
    NOT_APPLICABLE_COUPON("적합하지 않은 쿠폰 입니다.", HttpStatus.BAD_REQUEST),
    ORDER_CREATION_FAILED("주문 생성 실패", HttpStatus.BAD_REQUEST),
    COUPON_NOT_APPLIED("쿠폰이 적용되지 않았습니다.", HttpStatus.BAD_REQUEST),
    COUPON_DISCOUNT_PRICE_MISSING("쿠폰 할인 금액이 적용되지 않았습니다.", HttpStatus.BAD_REQUEST),
    ALREADY_CANCELED("이미 취소된 상품입니다", HttpStatus.BAD_REQUEST),
    INVALID_PAYMENT_AMOUNT("결제금액이 맞지 않습니다.", HttpStatus.BAD_REQUEST),
    INVALID_PRODUCT_OPTION("옳지 않은 옵션 입니다.", HttpStatus.BAD_REQUEST),
    LOCK_FAILED("락 획득 실패", HttpStatus.BAD_REQUEST),;


    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(String message, HttpStatus httpStatus) {
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
