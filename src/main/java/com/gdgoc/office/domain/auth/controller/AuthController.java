package com.gdgoc.office.domain.auth.controller;

import com.gdgoc.office.domain.auth.dto.AuthRequest;
import com.gdgoc.office.domain.auth.dto.AuthResponse;
import com.gdgoc.office.domain.auth.service.AuthService;
import com.gdgoc.office.global.common.CustomResponse;
import com.gdgoc.office.global.config.ApiErrorCodes;
import com.gdgoc.office.global.config.OpenApiConfig;
import com.gdgoc.office.global.exception.ErrorCode;
import com.gdgoc.office.global.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "01. 회원가입 · 로그인", description = "초대 코드 기반 회원가입, 로그인, 토큰 재발급, 승인 상태 조회")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "회원가입", description = "운영진이 발급한 초대 코드로 가입합니다. 이름은 초대 코드에 등록된 이름이 사용되며, 가입 직후 상태는 PENDING입니다.")
    @ApiResponse(responseCode = "201", description = "회원가입 성공", useReturnTypeSchema = true)
    @ApiErrorCodes({ErrorCode.INVITE_CODE_INVALID, ErrorCode.LOGIN_ID_DUPLICATED})
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/signup")
    public CustomResponse<AuthResponse.Signup> signup(@Valid @RequestBody AuthRequest.Signup request) {
        return CustomResponse.created("회원가입 성공", authService.signup(request));
    }

    @Operation(summary = "로그인", description = "아이디/비밀번호로 로그인하고 토큰과 온보딩 단계를 반환합니다.")
    @ApiResponse(responseCode = "200", description = "로그인 성공", useReturnTypeSchema = true)
    @ApiErrorCodes({ErrorCode.LOGIN_FAILED})
    @PostMapping("/login")
    public CustomResponse<AuthResponse.Login> login(@Valid @RequestBody AuthRequest.Login request) {
        return CustomResponse.ok("로그인 성공", authService.login(request));
    }

    @Operation(summary = "토큰 재발급", description = "Refresh Token으로 새 Access/Refresh Token을 발급합니다. 사용한 Refresh Token은 폐기됩니다.")
    @ApiResponse(responseCode = "200", description = "토큰 재발급 성공", useReturnTypeSchema = true)
    @ApiErrorCodes({ErrorCode.REFRESH_TOKEN_INVALID})
    @PostMapping("/refresh")
    public CustomResponse<AuthResponse.Token> refresh(@Valid @RequestBody AuthRequest.Refresh request) {
        return CustomResponse.ok("토큰 재발급 성공", authService.refresh(request));
    }

    @Operation(summary = "승인 상태 조회", description = "내 계정의 운영진 승인 상태를 조회합니다. 승인 대기(PENDING) 사용자도 호출할 수 있습니다.")
    @ApiResponse(responseCode = "200", description = "승인 상태 조회 성공", useReturnTypeSchema = true)
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @GetMapping("/approval-status")
    public CustomResponse<AuthResponse.ApprovalStatus> getApprovalStatus(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUser authUser) {
        return CustomResponse.ok("승인 상태 조회 성공", authService.getApprovalStatus(authUser.userId()));
    }
}
