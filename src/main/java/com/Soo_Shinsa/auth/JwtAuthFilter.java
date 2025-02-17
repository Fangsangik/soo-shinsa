package com.Soo_Shinsa.auth;

import com.Soo_Shinsa.constant.AuthenticationScheme;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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


@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final JwtBlackListService jwtBlackListService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        if (requestURI.equals("/users/login") || requestURI.equals("/users/signin")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = getTokenFromRequest(request);
        if (token != null && jwtBlackListService.isBlackListed(token)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        if (jwtBlackListService.isBlackListed(token)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        authenticate(request);
        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request) {
        String token = getTokenFromRequest(request);
        if (!jwtProvider.validAccessToken(token)) {
            return;
        }

        String username = jwtProvider.getUsername(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
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
}
