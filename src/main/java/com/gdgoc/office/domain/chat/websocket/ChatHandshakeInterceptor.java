package com.gdgoc.office.domain.chat.websocket;

import com.gdgoc.office.domain.chat.repository.ChatParticipantRepository;
import com.gdgoc.office.global.security.JwtAuthenticationFilter;
import com.gdgoc.office.global.security.JwtProvider;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * /ws/chatrooms/{chatRoomId} 핸드셰이크 시 Access Token(Authorization 헤더 또는 token 쿼리)을 검증하고,
 * 해당 방의 참여자만 연결을 허용한다.
 */
@Component
@RequiredArgsConstructor
public class ChatHandshakeInterceptor implements HandshakeInterceptor {

    private static final String PATH_PREFIX = "/ws/chatrooms/";

    private final JwtProvider jwtProvider;
    private final ChatParticipantRepository chatParticipantRepository;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        Optional<Long> chatRoomId = parseChatRoomId(request.getURI().getPath());
        if (chatRoomId.isEmpty()) {
            response.setStatusCode(HttpStatus.NOT_FOUND);
            return false;
        }
        Optional<Long> userId = jwtProvider.parseAccessToken(resolveToken(request));
        if (userId.isEmpty()) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        if (!chatParticipantRepository.existsByChatRoomIdAndUserId(chatRoomId.get(), userId.get())) {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }
        attributes.put(ChatSessionRegistry.ATTR_USER_ID, userId.get());
        attributes.put(ChatSessionRegistry.ATTR_CHAT_ROOM_ID, chatRoomId.get());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }

    private String resolveToken(ServerHttpRequest request) {
        String fromHeader = JwtAuthenticationFilter.resolveToken(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        if (fromHeader != null) {
            return fromHeader;
        }
        return UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams().getFirst("token");
    }

    private static Optional<Long> parseChatRoomId(String path) {
        int index = path.indexOf(PATH_PREFIX);
        if (index < 0) {
            return Optional.empty();
        }
        String raw = path.substring(index + PATH_PREFIX.length());
        if (raw.endsWith("/")) {
            raw = raw.substring(0, raw.length() - 1);
        }
        try {
            return Optional.of(Long.parseLong(raw));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
