package com.gdgoc.office.domain.user.controller;

import com.gdgoc.office.domain.user.dto.MyInfoResponse;
import com.gdgoc.office.domain.user.dto.ProfileRequest;
import com.gdgoc.office.domain.user.service.UserService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "02. 내 정보 · 프로필", description = "프로필 작성/수정, 내 정보 조회")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me")
public class UserController {

    private final UserService userService;

    @Operation(summary = "프로필 작성", description = "온보딩 단계에서 프로필을 최초 작성합니다. 승인 대기 사용자도 호출할 수 있습니다.")
    @ApiResponse(responseCode = "201", description = "프로필 작성 성공", useReturnTypeSchema = true)
    @ApiErrorCodes({ErrorCode.PROFILE_ALREADY_EXISTS})
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/profile")
    public CustomResponse<Void> createProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody ProfileRequest.Create request) {
        userService.createProfile(authUser.userId(), request);
        return CustomResponse.created("프로필 작성 성공");
    }

    @Operation(summary = "내 정보 조회", description = "내 이름, 프로필, 권한을 조회합니다. 프로필 미작성이면 프로필 필드는 null입니다. 승인 대기 사용자도 호출할 수 있습니다.")
    @ApiResponse(responseCode = "200", description = "내 정보 조회 성공", useReturnTypeSchema = true)
    @GetMapping
    public CustomResponse<MyInfoResponse> getMyInfo(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUser authUser) {
        return CustomResponse.ok("내 정보 조회 성공", userService.getMyInfo(authUser.userId()));
    }

    @Operation(summary = "프로필 수정", description = "전달한 필드만 수정합니다.")
    @ApiResponse(responseCode = "200", description = "프로필 수정 성공", useReturnTypeSchema = true)
    @ApiErrorCodes({ErrorCode.PROFILE_NOT_FOUND})
    @PatchMapping("/profile")
    public CustomResponse<Void> updateProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody ProfileRequest.Update request) {
        userService.updateProfile(authUser.userId(), request);
        return CustomResponse.ok("프로필 수정 성공");
    }
}
