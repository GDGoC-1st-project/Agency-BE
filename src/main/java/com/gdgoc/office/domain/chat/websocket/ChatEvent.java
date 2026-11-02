package com.gdgoc.office.domain.chat.websocket;

/** 트랜잭션 커밋 후 WebSocket 으로 푸시할 채팅 이벤트 */
public record ChatEvent(Long chatRoomId, Long actorUserId, String event, Object data) {

    public static final String MESSAGE_NEW = "message.new";
    public static final String MESSAGE_READ = "message.read";
}
