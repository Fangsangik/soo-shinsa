package com.Soo_Shinsa.auth;

import com.Soo_Shinsa.constant.AuthenticationScheme;
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


@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final JwtBlackListService jwtBlackListService;
    private final UserDetailsService userDetailsService;
    private final JwtAccessTokenService jwtAccessTokenService;
    private final List<String> WHITE_LIST = List.of("/users/login", "/users/signin", "/kakao/login", "/kakao/logout", "/kakao/token",
            "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/api/chat/**", "/kakao/callback");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        if (isWhiteListed(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = getTokenFromRequest(request);
        if (token == null) {
            log.warn("요청에 토큰이 포함되지 않음.");
            filterChain.doFilter(request, response);
            return;
        }

        if (jwtBlackListService.isBlackListed(token)) {
            log.warn("토큰이 블랙리스트에 있음: {}", token);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        authenticate(request, token);
        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, String token) {
        if (!jwtProvider.validAccessToken(token)) {
            return;
        }

        String email = jwtProvider.getUsername(token);

        String storedAccessToken = jwtAccessTokenService.getAccessToken(email);
        if (storedAccessToken == null) {
            log.warn("Redis에서 AccessToken을 찾을 수 없음! username: {}", email);
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        if (userDetails == null) {
            log.warn("UserDetails를 찾을 수 없음! username: {}", email);
            return;
        }
        setAuthentication(request, userDetails);
    }

    private void setAuthentication(HttpServletRequest request, UserDetails userDetails) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails, userDetails.getPassword(), userDetails.getAuthorities());
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
