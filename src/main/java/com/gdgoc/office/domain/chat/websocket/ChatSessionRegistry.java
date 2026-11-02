package com.gdgoc.office.domain.chat.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/** chatRoomId → 세션 목록 (단일 서버 메모리 맵) */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatSessionRegistry {

    public static final String ATTR_USER_ID = "userId";
    public static final String ATTR_CHAT_ROOM_ID = "chatRoomId";

    private final Map<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public void register(Long chatRoomId, WebSocketSession session) {
        sessions.computeIfAbsent(chatRoomId, id -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void unregister(Long chatRoomId, WebSocketSession session) {
        sessions.computeIfPresent(chatRoomId, (id, set) -> {
            set.remove(session);
            return set.isEmpty() ? null : set;
        });
    }

    /** 방에 연결된 세션 중 excludeUserId 가 아닌 사용자에게 이벤트를 보낸다. */
    public void sendToOthers(Long chatRoomId, Long excludeUserId, String event, Object data) {
        Set<WebSocketSession> targets = sessions.get(chatRoomId);
        if (targets == null || targets.isEmpty()) {
            return;
        }
        String payload;
        try {
            payload = objectMapper.writeValueAsString(Map.of("event", event, "data", data));
        } catch (JsonProcessingException e) {
            log.error("WebSocket 이벤트 직렬화 실패", e);
            return;
        }
        TextMessage message = new TextMessage(payload);
        for (WebSocketSession session : targets) {
            Object userId = session.getAttributes().get(ATTR_USER_ID);
            if (excludeUserId.equals(userId) || !session.isOpen()) {
                continue;
            }
            try {
                session.sendMessage(message);
            } catch (IOException | IllegalStateException e) {
                log.debug("WebSocket 전송 실패: {}", e.getMessage());
            }
        }
    }
}
