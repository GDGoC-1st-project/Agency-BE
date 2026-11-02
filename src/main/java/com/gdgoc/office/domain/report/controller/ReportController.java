package com.gdgoc.office.domain.report.controller;

import com.gdgoc.office.domain.report.dto.ReportDtos;
import com.gdgoc.office.domain.report.service.ReportService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "06. 신고", description = "멤버/모집글/메시지 신고")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "신고", description = "멤버, 모집글, 메시지를 신고합니다. 같은 대상은 한 번만 신고할 수 있습니다.")
    @ApiResponse(responseCode = "201", description = "신고 접수 성공", useReturnTypeSchema = true)
    @ApiErrorCodes({ErrorCode.REPORT_TARGET_NOT_FOUND, ErrorCode.REPORT_DUPLICATED})
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public CustomResponse<ReportDtos.Created> report(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody ReportDtos.CreateRequest request) {
        return CustomResponse.created("신고 접수 성공", reportService.report(authUser.userId(), request));
    }
}
