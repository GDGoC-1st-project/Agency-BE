package com.gdgoc.office.domain.member.dto;

import com.gdgoc.office.domain.keyword.dto.KeywordResponse;
import com.gdgoc.office.domain.user.entity.Department;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public final class MemberResponse {

    private MemberResponse() {
    }

    @Schema(description = "멤버 검색 결과 항목")
    public record SearchItem(
            @Schema(description = "멤버 ID", example = "3") Long memberId,
            @Schema(description = "이름", example = "김지디") String name,
            @Schema(description = "소속 분야", example = "DEV", nullable = true) Department department,
            @ArraySchema(arraySchema = @Schema(description = "대표 키워드 (최대 3개)", example = "[\"Spring\", \"Java\", \"AI\"]"))
            List<String> keywords
    ) {
    }

    @Schema(description = "멤버 프로필")
    public record Profile(
            @Schema(description = "멤버 ID", example = "3") Long memberId,
            @Schema(description = "이름", example = "김지디") String name,
            @Schema(description = "한 줄 소개", example = "백엔드 개발을 좋아하는 3학년입니다.", nullable = true) String bio,
            @Schema(description = "전공", example = "컴퓨터공학과", nullable = true) String major,
            @Schema(description = "소속 분야", example = "DEV", nullable = true) Department department,
            @Schema(description = "연락처", example = "https://open.kakao.com/o/abcd", nullable = true) String contact,
            @Schema(description = "관계별 키워드") KeywordResponse.Grouped keywords
    ) {
    }
}
