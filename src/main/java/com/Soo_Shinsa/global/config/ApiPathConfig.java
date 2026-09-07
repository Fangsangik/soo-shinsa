package com.Soo_Shinsa.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API 경로 접두사.
 *
 * 컨트롤러마다 /brands, /orders 처럼 접두사 없이 붙어 있어서 버전을 올릴 자리가 없었다.
 * 컨트롤러 15개의 @RequestMapping 을 일일이 고치는 대신 여기서 한 번에 붙인다.
 *
 * 대상은 우리 패키지의 @RestController 뿐이다.
 * - springdoc 의 /v3/api-docs 같은 라이브러리 컨트롤러가 딸려 오지 않게 패키지를 제한한다.
 * - TossPaymentsController 는 @Controller(뷰 반환)라 제외된다. 결제사가 직접 호출하는
 *   /api/success, /api/fail 콜백 주소를 바꾸면 토스 쪽 설정도 같이 바꿔야 하므로 그대로 둔다.
 */
@Configuration
public class ApiPathConfig implements WebMvcConfigurer {

    public static final String API_PREFIX = "/api/v1";

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // HandlerTypePredicate.builder() 는 basePackage 와 annotation 을 OR 로 합친다.
        // 그대로 쓰면 springdoc 의 @RestController 까지 걸려 /v3/api-docs 가 옮겨간다(404).
        // 두 조건을 AND 로 묶는다.
        configurer.addPathPrefix(API_PREFIX,
                HandlerTypePredicate.forBasePackage("com.Soo_Shinsa")
                        .and(type -> type.isAnnotationPresent(RestController.class)));
    }
}
