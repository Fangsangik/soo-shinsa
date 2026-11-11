package com.Soo_Shinsa.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

/**
 * /ws/chat?token=&room={brandId}
 * 연결: 핸드셰이크 인터셉터가 검증한 사용자/방을 세션 속성에서 읽어 방에 등록하고 최근 대화를 보낸다.
 * 메시지: Redis 채널 chat:{room} 에 발행 → 모든 서버의 onMessage 가 그 방의 세션에 전달.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler implements MessageListener {
    static final String CHANNEL_PREFIX = "chat:room:";
    private final ChatRoomRegistry registry;
    private final ChatHistory history;
    private final StringRedisTemplate redis;
    private final ObjectMapper om = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        String room = room(session);
        registry.join(room, session);
        for (ChatMessage m : history.recent(room)) session.sendMessage(new TextMessage(om.writeValueAsString(m)));
        log.info("chat join room={} user={}", room, session.getAttributes().get("name"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        String text = message.getPayload().trim();
        if (text.isEmpty()) return;
        if ("ping".equalsIgnoreCase(text)) { session.sendMessage(new TextMessage("pong")); return; }
        if (text.length() > 1000) text = text.substring(0, 1000);
        ChatMessage m = new ChatMessage(room(session), (Long) session.getAttributes().get("userId"),
                (String) session.getAttributes().get("name"), (String) session.getAttributes().get("role"), text, System.currentTimeMillis());
        history.append(m);
        redis.convertAndSend(CHANNEL_PREFIX + m.room(), om.writeValueAsString(m));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        registry.leave(room(session), session);
    }

    /** Redis pub/sub 수신 → 해당 방 세션에만 전달 */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel());
        String room = channel.substring(CHANNEL_PREFIX.length());
        TextMessage out = new TextMessage(message.getBody());
        for (WebSocketSession s : registry.sessions(room)) {
            try { if (s.isOpen()) s.sendMessage(out); } catch (IOException e) { log.warn("chat send failed: {}", e.getMessage()); }
        }
    }

    static String room(WebSocketSession session) { return String.valueOf(session.getAttributes().get("room")); }
}
