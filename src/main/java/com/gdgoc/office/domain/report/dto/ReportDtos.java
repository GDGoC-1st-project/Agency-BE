package com.gdgoc.office.domain.report.dto;

import com.gdgoc.office.domain.report.entity.ReportTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ReportDtos {

    private ReportDtos() {
    }

    @Schema(description = "신고 요청")
    public record CreateRequest(
            @Schema(description = "신고 대상 유형 (USER: 멤버 / POST: 모집글 / MESSAGE: 메시지)", example = "USER")
            @NotNull
            ReportTargetType targetType,

            @Schema(description = "신고 대상 ID", example = "3")
            @NotNull
            Long targetId,

            @Schema(description = "신고 사유 (200자 이내)", example = "부적절한 메시지를 반복해서 보냅니다.")
            @NotBlank @Size(max = 200)
            String reason
    ) {
    }

    @Schema(description = "신고 접수 결과")
    public record Created(
            @Schema(description = "생성된 신고 ID", example = "1") Long reportId
    ) {
    }
}
