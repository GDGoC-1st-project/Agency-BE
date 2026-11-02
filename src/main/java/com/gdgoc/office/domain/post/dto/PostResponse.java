package com.gdgoc.office.domain.post.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gdgoc.office.domain.post.entity.Post;
import com.gdgoc.office.global.util.TextUtils;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public final class PostResponse {

    public static final int PREVIEW_LENGTH = 100;

    private PostResponse() {
    }

    @Schema(description = "모집글 목록 항목")
    public record ListItem(
            @Schema(description = "모집글 ID", example = "1") Long postId,
            @Schema(description = "제목", example = "해커톤 같이 나갈 백엔드 개발자 구해요") String title,
            @Schema(description = "설명 미리보기 (앞 100자)", example = "11월 GDG 해커톤에 함께 나갈 팀원을 찾습니다.") String descriptionPreview,
            @ArraySchema(arraySchema = @Schema(description = "키워드 이름 목록", example = "[\"Spring\", \"AI\"]"))
            List<String> keywords,
            @Schema(description = "작성자 이름", example = "김지디") String authorName,
            @Schema(description = "작성 시각", example = "2026-10-05T10:00:00") LocalDateTime createdAt
    ) {
        public static ListItem from(Post post) {
            return new ListItem(post.getId(), post.getTitle(),
                    TextUtils.preview(post.getDescription(), PREVIEW_LENGTH), post.keywordNames(),
                    post.getAuthor().getName(), post.getCreatedAt());
        }
    }

    @Schema(description = "작성자")
    public record Author(
            @Schema(description = "작성자 멤버 ID", example = "3") Long memberId,
            @Schema(description = "작성자 이름", example = "김지디") String name
    ) {
    }

    @Schema(description = "모집글 상세")
    public record Detail(
            @Schema(description = "모집글 ID", example = "1") Long postId,
            @Schema(description = "제목", example = "해커톤 같이 나갈 백엔드 개발자 구해요") String title,
            @Schema(description = "설명", example = "11월 GDG 해커톤에 함께 나갈 팀원을 찾습니다.") String description,
            @ArraySchema(arraySchema = @Schema(description = "모집 역할", example = "[\"백엔드\", \"디자이너\"]"))
            List<String> requiredRoles,
            @ArraySchema(arraySchema = @Schema(description = "키워드 이름 목록", example = "[\"Spring\", \"AI\"]"))
            List<String> keywords,
            @Schema(description = "작성자") Author author,
            @JsonProperty("isAuthor") @Schema(description = "내가 작성한 글인지 여부", example = "false") Boolean isAuthor,
            @JsonProperty("hasApplied") @Schema(description = "내가 지원했는지 여부", example = "false") Boolean hasApplied,
            @Schema(description = "작성 시각", example = "2026-10-05T10:00:00") LocalDateTime createdAt
    ) {
    }

    @Schema(description = "모집글 작성 결과")
    public record Created(
            @Schema(description = "생성된 모집글 ID", example = "1") Long postId
    ) {
    }

    @Schema(description = "지원 결과")
    public record Applied(
            @Schema(description = "생성된 지원 ID", example = "1") Long applicationId
    ) {
    }
}
