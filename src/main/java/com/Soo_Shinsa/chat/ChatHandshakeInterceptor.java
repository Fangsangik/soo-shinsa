package com.Soo_Shinsa.chat;

import com.Soo_Shinsa.auth.JwtAccessTokenService;
import com.Soo_Shinsa.auth.JwtProvider;
import com.Soo_Shinsa.brand.repository.BrandRepository;
import com.Soo_Shinsa.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * 브라우저 WebSocket 은 Authorization 헤더를 못 보내므로 ?token= 으로 받아 JWT 필터와 같은 규칙으로 검증한다.
 * 방 권한: CUSTOMER 는 아무 브랜드 방, VENDOR 는 자기 브랜드 방만, ADMIN 은 전부.
 */
@Component
@RequiredArgsConstructor
public class ChatHandshakeInterceptor implements HandshakeInterceptor {
    private final JwtProvider jwtProvider;
    private final JwtAccessTokenService jwtAccessTokenService;
    private final BrandRepository brandRepository;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        var q = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams();
        String token = q.getFirst("token"), room = q.getFirst("room");
        if (token == null || room == null || !room.matches("\\d+") || jwtProvider.check(token) != null || jwtAccessTokenService.getAccessToken(token) == null) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        User user = jwtProvider.principalFrom(token);
        if (!canEnter(user, Long.parseLong(room))) { response.setStatusCode(HttpStatus.FORBIDDEN); return false; }
        attributes.put("userId", user.getUserId()); attributes.put("name", user.getName()); attributes.put("role", user.getRole().name()); attributes.put("room", room);
        return true;
    }

    boolean canEnter(User user, Long brandId) {
        return switch (user.getRole()) {
            case ADMIN, CUSTOMER -> true;
            case VENDOR -> brandRepository.existsByIdAndUserUserId(brandId, user.getUserId());
        };
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) { }
}
