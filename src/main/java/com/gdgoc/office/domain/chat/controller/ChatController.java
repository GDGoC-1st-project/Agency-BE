package com.gdgoc.office.domain.chat.controller;

import com.gdgoc.office.domain.chat.dto.ChatDtos;
import com.gdgoc.office.domain.chat.service.ChatService;
import com.gdgoc.office.global.common.CustomResponse;
import com.gdgoc.office.global.common.PageResult;
import com.gdgoc.office.global.config.ApiErrorCodes;
import com.gdgoc.office.global.config.OpenApiConfig;
import com.gdgoc.office.global.exception.ErrorCode;
import com.gdgoc.office.global.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "11. 채팅",
        description = "1:1 채팅방 목록/상세, 메시지 조회/전송, 읽음 처리. 채팅방은 요청 수락 시 자동 생성됩니다. "
                + "실시간 수신은 WebSocket /ws/chatrooms/{chatRoomId}?token={accessToken} 으로 연결하며 "
                + "message.new, message.read 이벤트를 받습니다(수신 전용).")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chatrooms")
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "채팅방 목록",
            description = "내가 참여한 채팅방을 최근 메시지 시각(메시지가 없으면 방 생성 시각) 내림차순으로 조회합니다. 내가 차단한 상대와의 방은 제외됩니다.")
    @ApiResponse(responseCode = "200", description = "채팅방 목록 조회 성공", useReturnTypeSchema = true)
    @GetMapping
    public CustomResponse<PageResult<ChatDtos.RoomItem>> getRooms(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "페이지 크기 (기본 20, 최대 100)", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return CustomResponse.ok("채팅방 목록 조회 성공", chatService.getRooms(authUser.userId(), page, size));
    }

    @Operation(summary = "채팅방 상세 조회", description = "채팅 상대와 연결 계기(같이 해요 제안 / 모집글 지원) 카드를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "채팅방 조회 성공", useReturnTypeSchema = true)
    @ApiErrorCodes({ErrorCode.CHAT_ROOM_NOT_FOUND, ErrorCode.CHAT_FORBIDDEN})
    @GetMapping("/{chatRoomId}")
    public CustomResponse<ChatDtos.RoomDetail> getRoom(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "채팅방 ID", example = "1") @PathVariable Long chatRoomId) {
        return CustomResponse.ok("채팅방 조회 성공", chatService.getRoom(authUser.userId(), chatRoomId));
    }

    @Operation(summary = "메시지 목록 조회", description = "최신 메시지가 먼저 오는 커서 기반 목록입니다. cursor를 주면 그 메시지 ID보다 이전 메시지를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "메시지 목록 조회 성공", useReturnTypeSchema = true)
    @ApiErrorCodes({ErrorCode.CHAT_ROOM_NOT_FOUND, ErrorCode.CHAT_FORBIDDEN})
    @GetMapping("/{chatRoomId}/messages")
    public CustomResponse<ChatDtos.MessagePage> getMessages(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "채팅방 ID", example = "1") @PathVariable Long chatRoomId,
            @Parameter(description = "커서 (이 메시지 ID보다 이전 메시지를 조회, 첫 조회 시 생략)", example = "41")
            @RequestParam(required = false) Long cursor,
            @Parameter(description = "조회 개수 (기본 30, 최대 100)", example = "30")
            @RequestParam(defaultValue = "30") @Min(1) @Max(100) int size) {
        return CustomResponse.ok("메시지 목록 조회 성공", chatService.getMessages(authUser.userId(), chatRoomId, cursor, size));
    }

    @Operation(summary = "메시지 전송", description = "메시지를 저장하고 상대가 WebSocket에 연결되어 있으면 message.new 이벤트를 푸시합니다. 차단 관계의 방에는 보낼 수 없습니다.")
    @ApiResponse(responseCode = "201", description = "메시지 전송 성공", useReturnTypeSchema = true)
    @ApiErrorCodes({ErrorCode.CHAT_ROOM_NOT_FOUND, ErrorCode.CHAT_FORBIDDEN})
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/{chatRoomId}/messages")
    public CustomResponse<ChatDtos.Sent> send(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "채팅방 ID", example = "1") @PathVariable Long chatRoomId,
            @Valid @RequestBody ChatDtos.SendRequest request) {
        return CustomResponse.created("메시지 전송 성공", chatService.send(authUser.userId(), chatRoomId, request.content()));
    }

    @Operation(summary = "읽음 처리", description = "내 마지막 읽은 메시지 위치를 갱신(작아지는 방향으로는 갱신하지 않음)하고 상대에게 message.read 이벤트를 푸시합니다.")
    @ApiResponse(responseCode = "200", description = "읽음 처리 성공", useReturnTypeSchema = true)
    @ApiErrorCodes({ErrorCode.CHAT_ROOM_NOT_FOUND, ErrorCode.CHAT_FORBIDDEN})
    @PatchMapping("/{chatRoomId}/read")
    public CustomResponse<Void> read(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "채팅방 ID", example = "1") @PathVariable Long chatRoomId,
            @Valid @RequestBody ChatDtos.ReadRequest request) {
        chatService.read(authUser.userId(), chatRoomId, request.lastReadMessageId());
        return CustomResponse.ok("읽음 처리 성공");
    }
}
