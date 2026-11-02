package com.gdgoc.office.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gdgoc.office.domain.user.entity.Department;
import com.gdgoc.office.domain.user.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "내 정보 (프로필 미작성이면 major/department/bio/contact는 null)")
@JsonInclude(JsonInclude.Include.ALWAYS)
public record MyInfoResponse(
        @Schema(description = "사용자 ID", example = "1") Long userId,
        @Schema(description = "이름", example = "김지디") String name,
        @Schema(description = "전공", example = "컴퓨터공학과", nullable = true) String major,
        @Schema(description = "소속 분야", example = "DEV", nullable = true) Department department,
        @Schema(description = "한 줄 소개", example = "백엔드 개발을 좋아하는 3학년입니다.", nullable = true) String bio,
        @Schema(description = "연락처", example = "https://open.kakao.com/o/abcd", nullable = true) String contact,
        @Schema(description = "권한", example = "MEMBER") Role role
) {
}
