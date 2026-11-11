package com.Soo_Shinsa.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/** 방마다 최근 메시지 50개를 Redis 리스트에 보관 (새로 들어온 쪽에 이전 대화를 보여주기 위해). */
@Component
@RequiredArgsConstructor
public class ChatHistory {
    static final int KEEP = 50;
    private final StringRedisTemplate redis;
    private final ObjectMapper om = new ObjectMapper();

    private static String key(String room) { return "chat:history:" + room; }

    @SneakyThrows
    public void append(ChatMessage m) {
        redis.opsForList().rightPush(key(m.room()), om.writeValueAsString(m));
        redis.opsForList().trim(key(m.room()), -KEEP, -1);
    }

    public List<ChatMessage> recent(String room) {
        List<String> raw = redis.opsForList().range(key(room), 0, -1);
        return raw == null ? List.of() : raw.stream().map(this::parse).filter(Objects::nonNull).toList();
    }

    public ChatMessage last(String room) {
        String raw = redis.opsForList().index(key(room), -1);
        return raw == null ? null : parse(raw);
    }

    private ChatMessage parse(String raw) { try { return om.readValue(raw, ChatMessage.class); } catch (Exception e) { return null; } }
}
