package com.gdgoc.office.domain.chat.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/** 채팅 수신 전용 WebSocket 핸들러. 클라이언트가 보낸 메시지는 무시한다(전송은 REST). */
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final int SEND_TIME_LIMIT_MS = 10_000;
    private static final int BUFFER_SIZE_LIMIT = 512 * 1024;
    private static final String DECORATED = "decoratedSession";

    private final ChatSessionRegistry registry;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        WebSocketSession decorated = new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MS, BUFFER_SIZE_LIMIT);
        session.getAttributes().put(DECORATED, decorated);
        registry.register(chatRoomId(session), decorated);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 수신 전용: 클라이언트 메시지는 처리하지 않는다.
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object decorated = session.getAttributes().get(DECORATED);
        if (decorated instanceof WebSocketSession decoratedSession) {
            registry.unregister(chatRoomId(session), decoratedSession);
        }
    }

    private Long chatRoomId(WebSocketSession session) {
        return (Long) session.getAttributes().get(ChatSessionRegistry.ATTR_CHAT_ROOM_ID);
    }
}
