package com.gdgoc.office.domain.admin.dto;

import com.gdgoc.office.domain.report.entity.ReportStatus;
import com.gdgoc.office.domain.report.entity.ReportTargetType;
import com.gdgoc.office.domain.user.entity.Department;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public final class AdminDtos {

    private AdminDtos() {
    }

    @Schema(description = "운영 관리 현황")
    public record Dashboard(
            @Schema(description = "가입 승인 대기 수 (프로필·키워드 등록을 마친 PENDING 사용자)", example = "3") Integer pendingSignupCount,
            @Schema(description = "처리 대기 신고 수", example = "1") Integer pendingReportCount
    ) {
    }

    @Schema(description = "가입 승인 대기 사용자")
    public record SignupItem(
            @Schema(description = "사용자 ID", example = "5") Long userId,
            @Schema(description = "이름", example = "김지디") String name,
            @Schema(description = "전공", example = "컴퓨터공학과", nullable = true) String major,
            @Schema(description = "소속 분야", example = "DEV", nullable = true) Department department,
            @ArraySchema(arraySchema = @Schema(description = "대표 키워드 (최대 3개)", example = "[\"Spring\", \"Java\"]"))
            List<String> keywords
    ) {
    }

    @Schema(description = "신고 목록 항목")
    public record ReportItem(
            @Schema(description = "신고 ID", example = "1") Long reportId,
            @Schema(description = "신고 대상 유형", example = "USER") ReportTargetType targetType,
            @Schema(description = "신고 대상 ID", example = "3") Long targetId,
            @Schema(description = "대상 이름 (USER: 이름 / POST: 제목 / MESSAGE: 보낸 사람 이름)", example = "김지디", nullable = true) String targetName,
            @Schema(description = "신고자 이름", example = "이구글") String reporterName,
            @Schema(description = "신고 사유", example = "부적절한 메시지를 반복해서 보냅니다.") String reason,
            @Schema(description = "처리 상태", example = "PENDING") ReportStatus status,
            @Schema(description = "신고 시각", example = "2026-10-05T10:00:00") LocalDateTime createdAt
    ) {
    }

    @Schema(description = "신고 처리 요청")
    public record ResolveRequest(
            @Schema(description = "처리 메모 (선택, 200자 이내)", example = "경고 조치 완료")
            @Size(max = 200)
            String memo
    ) {
    }
}
