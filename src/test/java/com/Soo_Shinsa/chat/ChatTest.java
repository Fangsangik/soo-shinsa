package com.Soo_Shinsa.chat;

import com.Soo_Shinsa.auth.JwtAccessTokenService;
import com.Soo_Shinsa.auth.JwtBlackListService;
import com.Soo_Shinsa.auth.JwtProvider;
import com.Soo_Shinsa.brand.repository.BrandRepository;
import com.Soo_Shinsa.constant.Role;
import com.Soo_Shinsa.user.model.User;
import com.Soo_Shinsa.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.Soo_Shinsa.support.Fixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 상담 채팅: 방 권한(핸드셰이크)과 방별 전달(Redis → 그 방 세션만). */
@ExtendWith(MockitoExtension.class)
class ChatTest {
    @Mock UserRepository userRepository;
    @Mock JwtBlackListService blackList;
    @Mock JwtAccessTokenService accessTokens;
    @Mock BrandRepository brandRepository;
    @Mock ServerHttpRequest request;
    @Mock ServerHttpResponse response;

    private JwtProvider provider() {
        JwtProvider p = new JwtProvider(userRepository, blackList);
        ReflectionTestUtils.setField(p, "secret", "unit-test-secret-key-that-is-at-least-32-bytes-long!!");
        return p;
    }

    private String token(JwtProvider p, User u) {
        when(userRepository.findByEmailOrElseThrow(u.getEmail())).thenReturn(u);
        return p.generateTokenBy(u.getEmail(), 60_000);
    }

    @Test
    void 입점사는_자기_브랜드_방만_고객은_아무_방이나_들어간다() {
        JwtProvider p = provider();
        ChatHandshakeInterceptor it = new ChatHandshakeInterceptor(p, accessTokens, brandRepository);
        User vendor = user(3L, Role.VENDOR), customer = user(1L, Role.CUSTOMER);
        String vt = token(p, vendor), ct = token(p, customer);
        when(accessTokens.getAccessToken(any())).thenReturn("x");
        when(brandRepository.existsByIdAndUserUserId(7L, 3L)).thenReturn(true);
        when(brandRepository.existsByIdAndUserUserId(8L, 3L)).thenReturn(false);

        Map<String, Object> attrs = new HashMap<>();
        when(request.getURI()).thenReturn(URI.create("ws://h/ws/chat?token=" + vt + "&room=7"));
        assertThat(it.beforeHandshake(request, response, null, attrs)).isTrue();
        assertThat(attrs).containsEntry("room", "7").containsEntry("role", "VENDOR").containsEntry("userId", 3L);

        when(request.getURI()).thenReturn(URI.create("ws://h/ws/chat?token=" + vt + "&room=8"));
        assertThat(it.beforeHandshake(request, response, null, new HashMap<>())).isFalse();

        when(request.getURI()).thenReturn(URI.create("ws://h/ws/chat?token=" + ct + "&room=8"));
        assertThat(it.beforeHandshake(request, response, null, new HashMap<>())).isTrue();
    }

    @Test
    void 세션이_없거나_토큰이_잘못되면_거절() {
        JwtProvider p = provider();
        ChatHandshakeInterceptor it = new ChatHandshakeInterceptor(p, accessTokens, brandRepository);
        when(request.getURI()).thenReturn(URI.create("ws://h/ws/chat?token=bad&room=1"));
        assertThat(it.beforeHandshake(request, response, null, new HashMap<>())).isFalse();
        String t = token(p, user(1L, Role.CUSTOMER));
        when(accessTokens.getAccessToken(t)).thenReturn(null); // 유휴 만료/로그아웃
        when(request.getURI()).thenReturn(URI.create("ws://h/ws/chat?token=" + t + "&room=1"));
        assertThat(it.beforeHandshake(request, response, null, new HashMap<>())).isFalse();
    }

    @Test
    void Redis_메시지는_같은_방의_열린_세션에만_전달된다() throws Exception {
        ChatRoomRegistry registry = new ChatRoomRegistry();
        ChatWebSocketHandler handler = new ChatWebSocketHandler(registry, mock(ChatHistory.class), mock(StringRedisTemplate.class));
        WebSocketSession a = mock(WebSocketSession.class), b = mock(WebSocketSession.class), closed = mock(WebSocketSession.class);
        when(a.isOpen()).thenReturn(true); when(closed.isOpen()).thenReturn(false);
        registry.join("7", a); registry.join("7", closed); registry.join("8", b);

        handler.onMessage(new DefaultMessage("chat:room:7".getBytes(), "{\"text\":\"hi\"}".getBytes()), null);

        verify(a).sendMessage(any(TextMessage.class));
        verify(closed, never()).sendMessage(any());
        verify(b, never()).sendMessage(any());
    }

    @Test
    void 보낸_메시지는_히스토리에_쌓이고_방_채널로_발행된다() throws Exception {
        ChatHistory history = mock(ChatHistory.class); StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ChatWebSocketHandler handler = new ChatWebSocketHandler(new ChatRoomRegistry(), history, redis);
        WebSocketSession s = mock(WebSocketSession.class);
        when(s.getAttributes()).thenReturn(Map.of("room", "7", "userId", 1L, "name", "고객", "role", "CUSTOMER"));

        handler.handleTextMessage(s, new TextMessage("안녕하세요"));

        verify(history).append(argThat(m -> m.room().equals("7") && m.text().equals("안녕하세요") && m.sender().equals("고객")));
        verify(redis).convertAndSend(eq("chat:room:7"), contains("안녕하세요"));
    }
}
