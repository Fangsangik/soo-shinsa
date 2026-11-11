package com.Soo_Shinsa.chat;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/** room → 열려 있는 WebSocket 세션. 서버가 여러 대면 각 서버가 자기 세션만 들고, 전달은 Redis pub/sub 이 맡는다. */
@Component
public class ChatRoomRegistry {
    private final Map<String, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();

    public void join(String room, WebSocketSession session) { rooms.computeIfAbsent(room, k -> new CopyOnWriteArraySet<>()).add(session); }
    public void leave(String room, WebSocketSession session) { Set<WebSocketSession> s = rooms.get(room); if (s != null) { s.remove(session); if (s.isEmpty()) rooms.remove(room); } }
    public Set<WebSocketSession> sessions(String room) { return rooms.getOrDefault(room, Set.of()); }
}
