package com.gdgoc.office.domain.block.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "차단 요청")
public record BlockRequest(
        @Schema(description = "차단할 멤버 ID", example = "3")
        @NotNull
        Long targetMemberId
) {
}
