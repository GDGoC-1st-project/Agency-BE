package com.gdgoc.office.domain.post.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class PostRequest {

    private static final String NOT_BLANK_IF_PRESENT = "(?s).*\\S.*";

    private PostRequest() {
    }

    @Schema(description = "모집글 작성 요청")
    public record Create(
            @Schema(description = "제목 (50자 이내)", example = "해커톤 같이 나갈 백엔드 개발자 구해요")
            @NotBlank @Size(max = 50)
            String title,

            @Schema(description = "설명", example = "11월 GDG 해커톤에 함께 나갈 팀원을 찾습니다. Spring 경험이 있으면 좋아요.")
            @NotBlank
            String description,

            @ArraySchema(arraySchema = @Schema(description = "모집 역할 (1개 이상)", example = "[\"백엔드\", \"디자이너\"]"))
            @NotEmpty
            List<@NotBlank @Size(max = 100) String> requiredRoles,

            @ArraySchema(arraySchema = @Schema(description = "키워드 ID 목록 (선택)", example = "[1, 2]"))
            List<@NotNull Long> keywordIds
    ) {
    }

    @Schema(description = "모집글 수정 요청 (전달한 필드만 수정)")
    public record Update(
            @Schema(description = "제목 (50자 이내)", example = "해커톤 팀원 모집 (백엔드 1명)")
            @Size(max = 50) @Pattern(regexp = NOT_BLANK_IF_PRESENT)
            String title,

            @Schema(description = "설명", example = "백엔드 1명만 더 구합니다.")
            @Pattern(regexp = NOT_BLANK_IF_PRESENT)
            String description,

            @ArraySchema(arraySchema = @Schema(description = "모집 역할 (전달 시 전체 교체, 1개 이상)", example = "[\"백엔드\"]"))
            @Size(min = 1)
            List<@NotBlank @Size(max = 100) String> requiredRoles,

            @ArraySchema(arraySchema = @Schema(description = "키워드 ID 목록 (전달 시 전체 교체)", example = "[1]"))
            List<@NotNull Long> keywordIds
    ) {
    }
}
