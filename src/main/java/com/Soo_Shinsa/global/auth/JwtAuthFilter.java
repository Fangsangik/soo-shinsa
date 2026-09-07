package com.Soo_Shinsa.global.auth;

import com.Soo_Shinsa.global.constant.AuthenticationScheme;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

import static com.Soo_Shinsa.global.constant.UrlConst.API;


@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final JwtBlackListService jwtBlackListService;
    private final UserDetailsService userDetailsService;
    private final JwtAccessTokenService jwtAccessTokenService;
    // 여기 걸리면 JWT 파싱 자체를 건너뛴다. 인가는 SecurityFilterChain 이 따로 본다.
    // "users/logout" 처럼 슬래시가 빠져 있어 매칭되지 않던 항목이 있었다.
    private final List<String> WHITE_LIST = List.of(
            API + "/users/login", API + "/users/signin", API + "/users/refresh", API + "/auth/**",
            "/api/success", "/api/fail",
            "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/api/chat/**");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        if (isWhiteListed(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 인증은 여기 한 곳에서만 한다. 예전에는 같은 일을 두 번 했고,
        // 두 번째 호출은 토큰이 없어도 실행돼 비로그인 요청마다 ERROR 가 찍혔다.
        String token = getTokenFromRequest(request);
        if (token != null && jwtProvider.validToken(token)) {
            if (jwtBlackListService.isBlackListed(token)) {
                // 토큰 전문을 로그에 남기면 로그가 유출됐을 때 그대로 재사용된다
                log.warn("🚨 블랙리스트된 JWT 사용 시도 감지: {}...", token.substring(0, Math.min(10, token.length())));
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "로그아웃된 토큰입니다.");
                return;
            }

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                String email = jwtProvider.getUsername(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                setAuthentication(request, userDetails);
            }
        }
        filterChain.doFilter(request, response);
    }

    private void setAuthentication(HttpServletRequest request, UserDetails userDetails) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        final String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        final String headerPrefix = AuthenticationScheme.generateType(AuthenticationScheme.BEARER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(headerPrefix)) {
            return bearerToken.substring(headerPrefix.length()).trim();
        }
        return null;
    }

    private boolean isWhiteListed(String requestURI) {
        return WHITE_LIST.stream().anyMatch(uri -> uri.equals(requestURI) || requestURI.matches(uri.replace("**", ".*")));
    }
}
