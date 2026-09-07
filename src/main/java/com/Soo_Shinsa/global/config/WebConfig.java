package com.Soo_Shinsa.global.config;

import com.Soo_Shinsa.global.auth.JwtAuthFilter;
import com.Soo_Shinsa.user.service.CustomOAuth2UserService;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import static org.springframework.security.config.Customizer.withDefaults;

import static com.Soo_Shinsa.global.constant.UrlConst.*;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class WebConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final AuthenticationProvider authenticationProvider;
    private final AuthenticationEntryPoint authEntryPoint;
    private final AccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(withDefaults())
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; " +
                                        "frame-src 'self' https://payment-gateway-sandbox.tosspayments.com https://js.tosspayments.com; " + // ✅ frame-src 추가
                                        "script-src 'self' 'unsafe-inline' https://js.tosspayments.com; " +
                                        "connect-src 'self' ws://localhost:8080 ws://127.0.0.1:8080 " +
                                        "https://api.tosspayments.com https://event.tosspayments.com https://apigw-sandbox.tosspayments.com; " +
                                        "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com;"
                        ))
                )

                .csrf(csrf -> csrf.disable())
                .formLogin(formLogin -> formLogin.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .authorizeHttpRequests(auth -> auth
                        // ✅ 정적 리소스 경로를 완전히 허용
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                        .requestMatchers("/", "/index.html", "/static/**", "/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                        .requestMatchers(WHITE_LIST).permitAll()
                        // 브랜드: 관리자 전용 경로가 먼저, 그 다음 공개 조회만 허용
                        .requestMatchers(API + "/brands/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, API + "/brands/vendor").authenticated()
                        .requestMatchers(HttpMethod.GET, API + "/brands", API + "/brands/*").permitAll()
                        // 상품: 비로그인 탐색/검색 허용 (등록·수정·삭제는 아래 규칙으로 인증 필요)
                        .requestMatchers(HttpMethod.GET, API + "/products/search", API + "/products/search/popular", API + "/products/autocomplete", API + "/products/*", API + "/products/brands/*").permitAll()
                        .requestMatchers(HttpMethod.GET, API + "/categories", API + "/categories/**", API + "/sub-categories", API + "/sub-categories/**").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.INCLUDE, DispatcherType.ERROR).permitAll()
                        .requestMatchers(ADMIN_INTERCEPTOR_LIST).hasRole("ADMIN")
                        .requestMatchers(VENDOR_INTERCEPTOR_LIST).hasRole("VENDOR")
                        // /users/** 는 "내 정보"다. 고객 전용으로 묶어 두는 바람에 업주는
                        // 자기 정보를 조회/수정/탈퇴할 수 없었다(403). 로그인만 하면 된다.
                        .requestMatchers(CUSTOMER_INTERCEPTOR_LIST).authenticated()
                        .requestMatchers(HttpMethod.GET, CUSTOMER_DENY_INTERCEPTOR_LIST).hasRole("CUSTOMER")
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                )
                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint(authEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy(
                """
                        ROLE_ADMIN > ROLE_VENDOR
                        ROLE_ADMIN > ROLE_CUSTOMER
                        """);
    }
}
