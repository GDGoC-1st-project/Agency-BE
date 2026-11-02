package com.gdgoc.office.domain.connection.controller;

import com.gdgoc.office.domain.connection.dto.ConnectionResponse;
import com.gdgoc.office.domain.connection.service.ConnectionService;
import com.gdgoc.office.global.common.CustomResponse;
import com.gdgoc.office.global.common.PageResult;
import com.gdgoc.office.global.config.OpenApiConfig;
import com.gdgoc.office.global.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "10. 내 연결", description = "요청 수락으로 성사된 연결 목록")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/connections")
public class ConnectionController {

    private final ConnectionService connectionService;

    @Operation(summary = "내 연결 목록", description = "내가 속한 연결을 최신순으로 조회합니다.")
    @ApiResponse(responseCode = "200", description = "내 연결 목록 조회 성공", useReturnTypeSchema = true)
    @GetMapping
    public CustomResponse<PageResult<ConnectionResponse.ListItem>> getMyConnections(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "페이지 크기 (기본 20, 최대 100)", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return CustomResponse.ok("내 연결 목록 조회 성공", connectionService.getMyConnections(authUser.userId(), page, size));
    }
}
