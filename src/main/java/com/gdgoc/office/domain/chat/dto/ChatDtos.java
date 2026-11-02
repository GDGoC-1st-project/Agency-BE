package com.gdgoc.office.domain.chat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gdgoc.office.domain.request.entity.RequestType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public final class ChatDtos {

    private ChatDtos() {
    }

    @Schema(description = "채팅 상대")
    public record Member(
            @Schema(description = "멤버 ID", example = "3") Long memberId,
            @Schema(description = "이름", example = "김지디") String name
    ) {
    }

    @Schema(description = "채팅방 목록 항목")
    public record RoomItem(
            @Schema(description = "채팅방 ID", example = "1") Long chatRoomId,
            @Schema(description = "채팅 상대") Member member,
            @Schema(description = "마지막 메시지 (없으면 null)", example = "내일 회의 몇 시에 할까요?", nullable = true) String lastMessage,
            @Schema(description = "마지막 메시지 시각 (없으면 null)", example = "2026-10-05T10:00:00", nullable = true) LocalDateTime lastMessageAt,
            @Schema(description = "안 읽은 메시지 수", example = "2") Integer unreadCount
    ) {
    }

    @Schema(description = "채팅방이 만들어진 계기 카드")
    public record ContextCard(
            @Schema(description = "연결 계기 (PROPOSAL: 같이 해요 / APPLICATION: 모집글 지원)", example = "PROPOSAL") RequestType sourceType,
            @Schema(description = "제목 (PROPOSAL: 제안 사유 라벨 / APPLICATION: 모집글 제목)", example = "프로젝트") String title,
            @Schema(description = "내용 (PROPOSAL: 제안 메시지 / APPLICATION: 모집글 설명 앞 100자)", example = "같이 해커톤 나가실래요?") String content
    ) {
    }

    @Schema(description = "채팅방 상세")
    public record RoomDetail(
            @Schema(description = "채팅방 ID", example = "1") Long chatRoomId,
            @Schema(description = "채팅 상대") Member member,
            @Schema(description = "연결 계기 카드") ContextCard contextCard
    ) {
    }

    @Schema(description = "메시지")
    public record MessageItem(
            @Schema(description = "메시지 ID", example = "10") Long messageId,
            @Schema(description = "보낸 사람 ID", example = "3") Long senderId,
            @JsonProperty("isMine")
            @Schema(description = "내가 보낸 메시지인지 여부", example = "true") Boolean isMine,
            @Schema(description = "내용", example = "안녕하세요!") String content,
            @JsonProperty("isRead")
            @Schema(description = "읽음 여부 (내가 보낸 메시지는 상대가 읽었는지, 상대가 보낸 메시지는 항상 true)", example = "false") Boolean isRead,
            @Schema(description = "보낸 시각", example = "2026-10-05T10:00:00") LocalDateTime createdAt
    ) {
    }

    @Schema(description = "메시지 목록 (커서 기반, 최신순)")
    public record MessagePage(
            @Schema(description = "메시지 목록") List<MessageItem> items,
            @Schema(description = "이전 메시지 존재 여부", example = "true") Boolean hasNext,
            @Schema(description = "다음 조회에 사용할 커서 (없으면 null)", example = "41", nullable = true) Long nextCursor
    ) {
    }

    @Schema(description = "메시지 전송 요청")
    public record SendRequest(
            @Schema(description = "메시지 내용 (1~1000자)", example = "안녕하세요!")
            @NotBlank @Size(min = 1, max = 1000)
            String content
    ) {
    }

    @Schema(description = "메시지 전송 결과")
    public record Sent(
            @Schema(description = "메시지 ID", example = "10") Long messageId,
            @Schema(description = "내용", example = "안녕하세요!") String content,
            @Schema(description = "보낸 시각", example = "2026-10-05T10:00:00") LocalDateTime createdAt
    ) {
    }

    @Schema(description = "읽음 처리 요청")
    public record ReadRequest(
            @Schema(description = "마지막으로 읽은 메시지 ID", example = "10")
            @NotNull
            Long lastReadMessageId
    ) {
    }
}
