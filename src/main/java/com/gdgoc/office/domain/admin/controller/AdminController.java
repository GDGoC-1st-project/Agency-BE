package com.gdgoc.office.domain.admin.controller;

import com.gdgoc.office.domain.admin.dto.AdminDtos;
import com.gdgoc.office.domain.admin.service.AdminService;
import com.gdgoc.office.domain.report.entity.ReportStatus;
import com.gdgoc.office.global.common.CountedPageResult;
import com.gdgoc.office.global.common.CustomResponse;
import com.gdgoc.office.global.config.ApiErrorCodes;
import com.gdgoc.office.global.config.OpenApiConfig;
import com.gdgoc.office.global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "12. 운영 관리", description = "운영진(ADMIN) 전용: 현황, 가입 승인, 신고 처리")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;

    @Operation(summary = "운영 관리 현황", description = "가입 승인 대기 수와 처리 대기 신고 수를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "운영 관리 현황 조회 성공", useReturnTypeSchema = true)
    @GetMapping("/dashboard")
    public CustomResponse<AdminDtos.Dashboard> getDashboard() {
        return CustomResponse.ok("운영 관리 현황 조회 성공", adminService.getDashboard());
    }

    @Operation(summary = "가입 승인 대기 목록", description = "승인 대기(PENDING) 중이면서 프로필 작성과 키워드 등록을 마친 사용자를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "가입 승인 대기 목록 조회 성공", useReturnTypeSchema = true)
    @GetMapping("/signups")
    public CustomResponse<CountedPageResult<AdminDtos.SignupItem>> getPendingSignups(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "페이지 크기 (기본 20, 최대 100)", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return CustomResponse.ok("가입 승인 대기 목록 조회 성공", adminService.getPendingSignups(page, size));
    }

    @Operation(summary = "가입 승인", description = "사용자의 상태를 APPROVED로 변경합니다.")
    @ApiResponse(responseCode = "200", description = "가입 승인 성공", useReturnTypeSchema = true)
    @ApiErrorCodes({ErrorCode.USER_NOT_FOUND, ErrorCode.USER_ALREADY_APPROVED})
    @PatchMapping("/signups/{userId}/approve")
    public CustomResponse<Void> approve(
            @Parameter(description = "승인할 사용자 ID", example = "5") @PathVariable Long userId) {
        adminService.approve(userId);
        return CustomResponse.ok("가입 승인 성공");
    }

    @Operation(summary = "신고 목록", description = "상태별 신고 목록을 최신순으로 조회합니다.")
    @ApiResponse(responseCode = "200", description = "신고 목록 조회 성공", useReturnTypeSchema = true)
    @GetMapping("/reports")
    public CustomResponse<CountedPageResult<AdminDtos.ReportItem>> getReports(
            @Parameter(description = "신고 상태 (PENDING / RESOLVED, 기본 PENDING)", example = "PENDING")
            @RequestParam(defaultValue = "PENDING") ReportStatus status,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "페이지 크기 (기본 20, 최대 100)", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return CustomResponse.ok("신고 목록 조회 성공", adminService.getReports(status, page, size));
    }

    @Operation(summary = "신고 처리", description = "신고를 처리 완료(RESOLVED)로 변경하고 처리 시각을 기록합니다.")
    @ApiResponse(responseCode = "200", description = "신고 처리 성공", useReturnTypeSchema = true)
    @ApiErrorCodes({ErrorCode.REPORT_NOT_FOUND, ErrorCode.REPORT_ALREADY_RESOLVED})
    @PatchMapping("/reports/{reportId}/resolve")
    public CustomResponse<Void> resolve(
            @Parameter(description = "처리할 신고 ID", example = "1") @PathVariable Long reportId,
            @Valid @RequestBody(required = false) AdminDtos.ResolveRequest request) {
        adminService.resolveReport(reportId, request == null ? null : request.memo());
        return CustomResponse.ok("신고 처리 성공");
    }
}
