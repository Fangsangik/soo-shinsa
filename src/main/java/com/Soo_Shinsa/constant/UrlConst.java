package com.Soo_Shinsa.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UrlConst {
    //로그인 필터 화이트 리스트
    public static final String[] WHITE_LIST =
            {"/users/login", "/users/signin", "users/logout", "/v3/api-docs/**", "/oauth2/**", "/auth/**",
                    "/swagger-ui/**", "/swagger-ui.html", "/api/v1/users", "/kakao/callback", "/api/chat/**", "/ws/**", "/test", "/stylesheets/**", "/success"};

    //어드민 인터셉터 리스트
    public static final String[] ADMIN_INTERCEPTOR_LIST = {"/admin", "/admin/**"};

    //사장 인터셉터 리스트
    public static final String[] VENDOR_INTERCEPTOR_LIST = {"/vendor", "/vendor/**"};

    //손님 인터셉터 리스트
    public static final String[] CUSTOMER_INTERCEPTOR_LIST = {"/users", "/users/**"};

    public static final String[] CUSTOMER_DENY_INTERCEPTOR_LIST = {"/brands","/brands/**","/categories","/categories/**","/products","/products/**","/sub-categories","/sub-categories/**"};

}
