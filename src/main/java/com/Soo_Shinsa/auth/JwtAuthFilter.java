package com.Soo_Shinsa.auth;

import com.Soo_Shinsa.constant.AuthenticationScheme;
import com.Soo_Shinsa.exception.ErrorCode;
import com.Soo_Shinsa.exception.ExceptionResponseDto;
import com.Soo_Shinsa.user.model.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 요청마다: 서명/만료 검증 → Redis 세션(access token → email) 존재 확인 → 유휴 TTL 연장 → 클레임으로 인증 주체 생성.
 * DB 조회는 하지 않는다. 세션 키가 없으면(30분 유휴 만료 또는 로그아웃) 토큰을 블랙리스트에 넣고 refresh token 도 지워 강제 로그아웃한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final JwtBlackListService jwtBlackListService;
    private final JwtAccessTokenService jwtAccessTokenService;
    private final JwtRefreshTokenService jwtRefreshTokenService;

    /** 마지막 요청 후 이 시간 동안 활동이 없으면 세션 만료 (기본 30분) */
    @Value("${jwt.idle-millis:1800000}")
    private long idleMillis;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (uri.equals("/users/login") || uri.equals("/users/signin")) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = getTokenFromRequest(request);
        if (token == null) {
            filterChain.doFilter(request, response); // 비로그인 → 보호 URL 은 EntryPoint 가 NO_TOKEN 401
            return;
        }
        ErrorCode problem = jwtProvider.check(token);
        if (problem != null) { // 만료(TOKEN_EXPIRED → 프론트가 refresh) 또는 위조(INVALID_TOKEN)
            ExceptionResponseDto.write(response, problem);
            return;
        }
        // 세션 저장소에 있으면 로그아웃·유휴만료되지 않은 토큰
        String email = jwtAccessTokenService.getAccessToken(token);
        if (email == null) {
            boolean loggedOut = jwtBlackListService.isBlackListed(token);
            if (!loggedOut) expireSession(token);
            ExceptionResponseDto.write(response, loggedOut ? ErrorCode.LOGGED_OUT : ErrorCode.SESSION_EXPIRED);
            return;
        }
        jwtAccessTokenService.touch(token, idleMillis);
        setAuthentication(request, jwtProvider.principalFrom(token));
        filterChain.doFilter(request, response);
    }

    /** 유휴 만료: 토큰 블랙리스트 + refresh token 삭제 → 프론트의 자동 갱신도 실패해 로그인 화면으로 */
    private void expireSession(String token) {
        jwtBlackListService.addBlackList(token, jwtProvider.remainingMillis(token));
        jwtRefreshTokenService.deleteRefreshToken(jwtProvider.getUsername(token));
        log.info("유휴 세션 만료 처리: {}", jwtProvider.getUsername(token));
    }

    private void setAuthentication(HttpServletRequest request, User principal) {
        UserDetailsImp userDetails = new UserDetailsImp(principal);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
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
}
