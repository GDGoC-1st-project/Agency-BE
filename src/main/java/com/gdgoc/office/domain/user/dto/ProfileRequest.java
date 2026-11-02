package com.gdgoc.office.domain.user.dto;

import com.gdgoc.office.domain.user.entity.Department;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class ProfileRequest {

    /** null은 허용하되, 값이 있으면 공백만으로 이루어질 수 없다. */
    private static final String NOT_BLANK_IF_PRESENT = "(?s).*\\S.*";

    private ProfileRequest() {
    }

    @Schema(description = "프로필 작성 요청")
    public record Create(
            @Schema(description = "한 줄 소개 (100자 이내)", example = "백엔드 개발을 좋아하는 3학년입니다.")
            @NotBlank @Size(max = 100)
            String bio,

            @Schema(description = "전공", example = "컴퓨터공학과")
            @NotBlank @Size(max = 100)
            String major,

            @Schema(description = "소속 분야 (DEV / AI / DESIGN)", example = "DEV")
            @NotNull
            Department department,

            @Schema(description = "연락처 (카카오톡 오픈채팅, 이메일 등)", example = "https://open.kakao.com/o/abcd")
            @NotBlank @Size(max = 200)
            String contact
    ) {
    }

    @Schema(description = "프로필 수정 요청 (전달한 필드만 수정)")
    public record Update(
            @Schema(description = "한 줄 소개 (100자 이내)", example = "AI에 관심이 많은 백엔드 개발자입니다.")
            @Size(max = 100) @Pattern(regexp = NOT_BLANK_IF_PRESENT)
            String bio,

            @Schema(description = "전공", example = "소프트웨어학과")
            @Size(max = 100) @Pattern(regexp = NOT_BLANK_IF_PRESENT)
            String major,

            @Schema(description = "소속 분야 (DEV / AI / DESIGN)", example = "AI")
            Department department,

            @Schema(description = "연락처", example = "gdgoc@example.com")
            @Size(max = 200) @Pattern(regexp = NOT_BLANK_IF_PRESENT)
            String contact
    ) {
    }
}
