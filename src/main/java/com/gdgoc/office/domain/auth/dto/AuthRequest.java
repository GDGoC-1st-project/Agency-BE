package com.gdgoc.office.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthRequest {

    private AuthRequest() {
    }

    @Schema(description = "회원가입 요청")
    public record Signup(
            @Schema(description = "로그인 아이디 (영문/숫자 4~20자)", example = "gdgoc01")
            @NotBlank @Pattern(regexp = "^[A-Za-z0-9]{4,20}$")
            String loginId,

            @Schema(description = "비밀번호 (8자 이상)", example = "password1234")
            @NotBlank @Size(min = 8)
            String password,

            @Schema(description = "운영진이 발급한 초대 코드", example = "GDGOC-INVITE-01")
            @NotBlank
            String inviteCode
    ) {
    }

    @Schema(description = "로그인 요청")
    public record Login(
            @Schema(description = "로그인 아이디", example = "gdgoc01")
            @NotBlank
            String loginId,

            @Schema(description = "비밀번호", example = "password1234")
            @NotBlank
            String password
    ) {
    }

    @Schema(description = "토큰 재발급 요청")
    public record Refresh(
            @Schema(description = "발급받은 Refresh Token", example = "eyJhbGciOiJIUzI1NiJ9...")
            @NotBlank
            String refreshToken
    ) {
    }
}
