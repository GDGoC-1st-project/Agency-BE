package com.gdgoc.office.domain.keyword.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "내 키워드 저장 요청 (관계별 전체 교체, 빈 배열 허용, 각 최대 20개)")
public record SaveMyKeywordsRequest(
        @ArraySchema(arraySchema = @Schema(description = "할 수 있어요 키워드 ID 목록", example = "[1, 2]"))
        @NotNull
        List<@NotNull Long> can,

        @ArraySchema(arraySchema = @Schema(description = "배우고 있어요 키워드 ID 목록", example = "[3]"))
        @NotNull
        List<@NotNull Long> learning,

        @ArraySchema(arraySchema = @Schema(description = "관심 있어요 키워드 ID 목록", example = "[]"))
        @NotNull
        List<@NotNull Long> interested
) {
}
