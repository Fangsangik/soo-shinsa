package com.Soo_Shinsa.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UrlConst {
    //로그인 필터 화이트 리스트
    public static final String[] WHITE_LIST = {"/", "/users/signin", "/users/refresh", "/users/logout", "/users/login", "/api/success", "/api/fail", "/api/client-key", "/index.html", "/app.js", "/app.css", "/style.css", "/favicon.ico"};

    //비로그인 조회 허용 (GET 전용)
    public static final String[] PUBLIC_GET_LIST = {"/brands", "/brands/{id:\\d+}", "/products", "/products/{id:\\d+}", "/products/brands/*",
            "/options/{id:\\d+}", "/reviews/{id:\\d+}", "/reviews/products/*", "/categories", "/categories/*", "/sub-categories", "/sub-categories/*"};

    //관리자 인터셉터 리스트
    public static final String[] ADMIN_INTERCEPTOR_LIST = {"/admin", "/admin/**"};

    //사장 인터셉터 리스트
    public static final String[] VENDOR_INTERCEPTOR_LIST = {"/owner", "/owner/**"};

    //손님 인터셉터 리스트
    public static final String[] CUSTOMER_INTERCEPTOR_LIST = {"/users", "/users/**"};

}
