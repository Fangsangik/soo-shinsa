package com.Soo_Shinsa.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UrlConst {
    //로그인 필터 화이트 리스트
    public static final String[] WHITE_LIST = {"/", "/users/signin", "/users/refresh", "/users/logout", "/users/login", "/api/**", "/test","/stylesheets/**","/success"};

    //사장 인터셉터 리스트
    public static final String[] ADMIN_INTERCEPTOR_LIST = {"/admin", "/admin/**"};

    //사장 인터셉터 리스트
    public static final String[] VENDOR_INTERCEPTOR_LIST = {"/owner", "/owner/**"};

    //손님 인터셉터 리스트
    public static final String[] CUSTOMER_INTERCEPTOR_LIST = {"/users", "/users/**"};

}
