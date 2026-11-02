package com.gdgoc.office.domain.auth.dto;

import com.gdgoc.office.domain.user.entity.Role;
import com.gdgoc.office.domain.user.entity.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public final class AuthResponse {

    private AuthResponse() {
    }

    @Schema(description = "회원가입 결과")
    public record Signup(
            @Schema(description = "생성된 사용자 ID", example = "1") Long userId,
            @Schema(description = "Access Token", example = "eyJhbGciOiJIUzI1NiJ9...") String accessToken,
            @Schema(description = "Refresh Token", example = "eyJhbGciOiJIUzI1NiJ9...") String refreshToken,
            @Schema(description = "승인 상태", example = "PENDING") UserStatus status
    ) {
    }

    @Schema(description = "로그인 결과")
    public record Login(
            @Schema(description = "Access Token", example = "eyJhbGciOiJIUzI1NiJ9...") String accessToken,
            @Schema(description = "Refresh Token", example = "eyJhbGciOiJIUzI1NiJ9...") String refreshToken,
            @Schema(description = "승인 상태", example = "APPROVED") UserStatus status,
            @Schema(description = "권한", example = "MEMBER") Role role,
            @Schema(description = "온보딩 단계 (PROFILE: 프로필 작성 필요 / KEYWORD: 키워드 등록 필요 / WAITING: 승인 대기 / DONE: 완료)",
                    example = "DONE") OnboardingStep onboardingStep
    ) {
    }

    @Schema(description = "토큰 재발급 결과")
    public record Token(
            @Schema(description = "새 Access Token", example = "eyJhbGciOiJIUzI1NiJ9...") String accessToken,
            @Schema(description = "새 Refresh Token", example = "eyJhbGciOiJIUzI1NiJ9...") String refreshToken
    ) {
    }

    @Schema(description = "승인 상태")
    public record ApprovalStatus(
            @Schema(description = "승인 상태 (PENDING: 승인 대기 / APPROVED: 승인 완료)", example = "PENDING") UserStatus status
    ) {
    }

    public enum OnboardingStep {
        PROFILE, KEYWORD, WAITING, DONE
    }
}
