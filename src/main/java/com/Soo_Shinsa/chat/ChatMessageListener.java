package com.Soo_Shinsa.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageListener implements MessageListener {

    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    /**
     * MessageListener 인터페이스를 통해 Redis PUB/SUB 메시지 수신
     * Redis에서 메시지 수신하면 onMessage 메서드 호출
     * @param message
     * @param pattern
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        String receivedMessage = new String(message.getBody());
        log.info("📩 Redis 메시지 수신: {}", receivedMessage);

        // 연결된 모든 WebSocket 클라이언트에 메시지 전송
        for (WebSocketSession session : sessions.values()) {
            try {
                session.sendMessage(new TextMessage(receivedMessage));
                log.info("WebSocket으로 메시지 전송: {}", receivedMessage);
            } catch (IOException e) {
                log.error("WebSocket 메시지 전송 실패", e);
            }
        }
    }
}