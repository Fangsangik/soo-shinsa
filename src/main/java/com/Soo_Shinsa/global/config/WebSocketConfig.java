package com.Soo_Shinsa.global.config;

import com.Soo_Shinsa.chat.ChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    
    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:8080}")
    private String allowedOrigins;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 🔒 보안 강화: 환경변수로 지정된 특정 도메인만 허용
        String[] origins = allowedOrigins.split(",");
        
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .setAllowedOrigins(origins)  // 특정 출처만 허용
                .withSockJS();  // SockJS fallback 지원
    }
}
