package com.Soo_Shinsa.global.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 시큐리티 규칙에 쓰는 경로 목록.
 *
 * 우리 @RestController 는 ApiPathConfig 가 /api/v1 을 붙이므로 여기도 접두사가 붙어 있다.
 * 접두사가 없는 것은 결제사 콜백(/api/success, /api/fail)과 프레임워크 경로뿐이다.
 */
@Getter
@RequiredArgsConstructor
public class UrlConst {

    public static final String API = "/api/v1";

    //로그인 필터 화이트 리스트
    public static final String[] WHITE_LIST =
            {API + "/users/login", API + "/users/signin",
                    // 갱신은 액세스 토큰이 만료된 뒤에 부르는 것이므로 인증을 요구하면 안 된다
                    API + "/users/refresh",
                    // 주의: /users/logout 은 여기 넣으면 안 된다. 토큰을 파싱해야
                    // 블랙리스트에 올릴 수 있는데, 화이트리스트면 파싱을 건너뛴다.
                    API + "/auth/**",
                    "/v3/api-docs/**", "/oauth2/**", "/swagger-ui/**", "/swagger-ui.html",
                    "/api/chat/**", "/ws/**", "/stylesheets/**",
                    // 토스가 직접 호출하는 콜백. 주소를 바꾸면 토스 설정도 바꿔야 한다
                    "/api/success", "/api/fail",
                    "/actuator/health", "/actuator/info"};

    //어드민 인터셉터 리스트
    public static final String[] ADMIN_INTERCEPTOR_LIST = {API + "/admin", API + "/admin/**"};

    //사장 인터셉터 리스트
    public static final String[] VENDOR_INTERCEPTOR_LIST = {API + "/vendor", API + "/vendor/**"};

    //손님 인터셉터 리스트
    public static final String[] CUSTOMER_INTERCEPTOR_LIST = {API + "/users", API + "/users/**"};

    public static final String[] CUSTOMER_DENY_INTERCEPTOR_LIST = {
            API + "/brands", API + "/brands/**",
            API + "/categories", API + "/categories/**",
            API + "/products", API + "/products/**",
            API + "/sub-categories", API + "/sub-categories/**"};

}
