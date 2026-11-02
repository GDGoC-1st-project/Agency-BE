package com.gdgoc.office;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdgoc.office.domain.connection.dto.ConnectionResponse;
import com.gdgoc.office.domain.connection.service.ConnectionService;
import com.gdgoc.office.domain.request.entity.RequestType;
import com.gdgoc.office.domain.user.entity.User;
import com.gdgoc.office.domain.user.repository.UserRepository;
import com.gdgoc.office.global.security.JwtProvider;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ChatWebSocketIntegrationTest {

    @LocalServerPort
    int port;
    @Autowired
    TestRestTemplate rest;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    UserRepository userRepository;
    @Autowired
    ConnectionService connectionService;
    @Autowired
    JwtProvider jwtProvider;

    @Test
    void pushesNewMessageAndReadEvents() throws Exception {
        User a = approvedUser("wsUserA", "에이");
        User b = approvedUser("wsUserB", "비");
        User outsider = approvedUser("wsUserC", "씨");
        ConnectionResponse.Connected connected = connectionService.connect(a, b, RequestType.PROPOSAL, 0L);
        Long roomId = connected.chatRoomId();
        String tokenA = jwtProvider.createAccessToken(a.getId(), a.getRole());
        String tokenB = jwtProvider.createAccessToken(b.getId(), b.getRole());

        // B가 쿼리 토큰으로 WebSocket 연결
        BlockingQueue<String> received = new LinkedBlockingQueue<>();
        WebSocketSession session = new StandardWebSocketClient().execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession s, TextMessage message) {
                received.add(message.getPayload());
            }
        }, "ws://localhost:" + port + "/ws/chatrooms/" + roomId + "?token=" + tokenB).get(5, TimeUnit.SECONDS);

        // A가 REST로 메시지 전송 → B에게 message.new
        ResponseEntity<String> sent = exchange(HttpMethod.POST, "/api/v1/chatrooms/" + roomId + "/messages", tokenA,
                Map.of("content", "실시간 안녕"));
        assertThat(sent.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long messageId = objectMapper.readTree(sent.getBody()).at("/result/messageId").asLong();

        JsonNode newEvent = objectMapper.readTree(received.poll(5, TimeUnit.SECONDS));
        assertThat(newEvent.get("event").asText()).isEqualTo("message.new");
        assertThat(newEvent.at("/data/messageId").asLong()).isEqualTo(messageId);
        assertThat(newEvent.at("/data/isMine").asBoolean()).isFalse();
        assertThat(newEvent.at("/data/content").asText()).isEqualTo("실시간 안녕");

        // B가 자기 읽음 처리 → B 본인 세션에는 푸시하지 않음
        exchange(HttpMethod.PATCH, "/api/v1/chatrooms/" + roomId + "/read", tokenB, Map.of("lastReadMessageId", messageId));
        assertThat(received.poll(500, TimeUnit.MILLISECONDS)).isNull();

        // A가 읽음 처리 → B에게 message.read
        exchange(HttpMethod.PATCH, "/api/v1/chatrooms/" + roomId + "/read", tokenA, Map.of("lastReadMessageId", messageId));
        JsonNode readEvent = objectMapper.readTree(received.poll(5, TimeUnit.SECONDS));
        assertThat(readEvent.get("event").asText()).isEqualTo("message.read");
        assertThat(readEvent.at("/data/lastReadMessageId").asLong()).isEqualTo(messageId);
        session.close();

        // 비참여자 / 잘못된 토큰은 연결 거부
        String outsiderToken = jwtProvider.createAccessToken(outsider.getId(), outsider.getRole());
        assertThatThrownBy(() -> new StandardWebSocketClient().execute(new TextWebSocketHandler(),
                "ws://localhost:" + port + "/ws/chatrooms/" + roomId + "?token=" + outsiderToken).get(5, TimeUnit.SECONDS))
                .isNotNull();
        assertThatThrownBy(() -> new StandardWebSocketClient().execute(new TextWebSocketHandler(),
                "ws://localhost:" + port + "/ws/chatrooms/" + roomId + "?token=invalid").get(5, TimeUnit.SECONDS))
                .isNotNull();
    }

    @Test
    void swaggerUiIsServed() {
        ResponseEntity<String> response = rest.getForEntity("/swagger-ui/index.html", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("swagger-ui");
    }

    @Test
    void unknownPathReturnsJsonError() throws Exception {
        String token = jwtProvider.createAccessToken(approvedUser("wsUserD", "디").getId(),
                com.gdgoc.office.domain.user.entity.Role.MEMBER);
        ResponseEntity<String> response = exchange(HttpMethod.GET, "/api/v1/nope", token, null);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(objectMapper.readTree(response.getBody()).get("isSuccess").asBoolean()).isFalse();
    }

    private User approvedUser(String loginId, String name) {
        User user = User.createMember(loginId, "{noop}x", name);
        user.approve();
        return userRepository.save(user);
    }

    private ResponseEntity<String> exchange(HttpMethod method, String path, String token, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange(URI.create("http://localhost:" + port + path), method, new HttpEntity<>(body, headers), String.class);
    }
}
