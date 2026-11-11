package com.Soo_Shinsa.chat;

/** 상담방 메시지. room = 브랜드 id (고객 ↔ 그 브랜드 입점사). JSON 으로 Redis 채널과 WebSocket 을 오간다. */
public record ChatMessage(String room, Long senderId, String sender, String role, String text, long at) {
    public static final String SYSTEM = "SYSTEM";
}
