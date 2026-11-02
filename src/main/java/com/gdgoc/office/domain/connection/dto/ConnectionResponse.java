package com.gdgoc.office.domain.connection.dto;

import com.gdgoc.office.domain.user.entity.Department;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public final class ConnectionResponse {

    private ConnectionResponse() {
    }

    @Schema(description = "요청 수락 결과 (생성되었거나 재사용된 연결/채팅방)")
    public record Connected(
            @Schema(description = "연결 ID", example = "1") Long connectionId,
            @Schema(description = "채팅방 ID", example = "1") Long chatRoomId
    ) {
    }

    @Schema(description = "연결된 멤버")
    public record Member(
            @Schema(description = "멤버 ID", example = "3") Long memberId,
            @Schema(description = "이름", example = "김지디") String name,
            @Schema(description = "전공", example = "컴퓨터공학과", nullable = true) String major,
            @Schema(description = "소속 분야", example = "DEV", nullable = true) Department department
    ) {
    }

    @Schema(description = "내 연결 목록 항목")
    public record ListItem(
            @Schema(description = "연결 ID", example = "1") Long connectionId,
            @Schema(description = "연결된 멤버") Member member,
            @ArraySchema(arraySchema = @Schema(description = "상대의 대표 키워드 (최대 3개)", example = "[\"Spring\", \"Java\"]"))
            List<String> keywords,
            @Schema(description = "채팅방 ID", example = "1") Long chatRoomId,
            @Schema(description = "연결 시각", example = "2026-10-05T10:00:00") LocalDateTime createdAt
    ) {
    }
}
