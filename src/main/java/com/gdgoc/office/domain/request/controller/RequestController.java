package com.gdgoc.office.domain.request.controller;

import com.gdgoc.office.domain.connection.dto.ConnectionResponse;
import com.gdgoc.office.domain.request.dto.RequestDtos;
import com.gdgoc.office.domain.request.entity.RequestStatus;
import com.gdgoc.office.domain.request.entity.RequestType;
import com.gdgoc.office.domain.request.service.RequestService;
import com.gdgoc.office.global.common.CustomResponse;
import com.gdgoc.office.global.common.PageResult;
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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "09. 요청", description = "받은/보낸 요청(같이 해요 제안 + 모집글 지원) 목록, 수락/거절")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/requests")
public class RequestController {

    private final RequestService requestService;

    @Operation(summary = "받은 요청 목록",
            description = "나에게 온 같이 해요 제안과 내 모집글(삭제되지 않은 글)에 들어온 지원을 최신순으로 합쳐 조회합니다. 내가 차단한 멤버의 요청은 제외됩니다.")
    @ApiResponse(responseCode = "200", description = "받은 요청 목록 조회 성공", useReturnTypeSchema = true)
    @GetMapping("/received")
    public CustomResponse<PageResult<RequestDtos.ReceivedItem>> getReceived(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "요청 상태 (PENDING / ACCEPTED / REJECTED, 미입력 시 전체)", example = "PENDING")
            @RequestParam(required = false) RequestStatus status,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "페이지 크기 (기본 20, 최대 100)", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return CustomResponse.ok("받은 요청 목록 조회 성공", requestService.getReceived(authUser.userId(), status, page, size));
    }

    @Operation(summary = "보낸 요청 목록",
            description = "내가 보낸 같이 해요 제안과 모집글 지원을 최신순으로 합쳐 조회합니다. 거절된 요청은 거절 사유를 함께 반환합니다.")
    @ApiResponse(responseCode = "200", description = "보낸 요청 목록 조회 성공", useReturnTypeSchema = true)
    @GetMapping("/sent")
    public CustomResponse<PageResult<RequestDtos.SentItem>> getSent(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "요청 상태 (PENDING / ACCEPTED / REJECTED, 미입력 시 전체)", example = "REJECTED")
            @RequestParam(required = false) RequestStatus status,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "페이지 크기 (기본 20, 최대 100)", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return CustomResponse.ok("보낸 요청 목록 조회 성공", requestService.getSent(authUser.userId(), status, page, size));
    }

    @Operation(summary = "요청 수락",
            description = "받은 요청을 수락합니다. 수신자만 대기 중인 요청을 수락할 수 있으며, 수락 시 연결과 1:1 채팅방이 생성됩니다(이미 연결된 경우 기존 연결/채팅방 재사용).")
    @ApiResponse(responseCode = "200", description = "요청 수락 성공", useReturnTypeSchema = true)
    @ApiErrorCodes({ErrorCode.REQUEST_NOT_FOUND, ErrorCode.REQUEST_FORBIDDEN, ErrorCode.REQUEST_ALREADY_PROCESSED})
    @PatchMapping("/{requestType}/{requestId}/accept")
    public CustomResponse<ConnectionResponse.Connected> accept(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "요청 유형 (PROPOSAL / APPLICATION)", example = "PROPOSAL") @PathVariable RequestType requestType,
            @Parameter(description = "요청 ID", example = "1") @PathVariable Long requestId) {
        return CustomResponse.ok("요청 수락 성공", requestService.accept(authUser.userId(), requestType, requestId));
    }

    @Operation(summary = "요청 거절", description = "받은 요청을 거절합니다. 거절 사유가 ETC이면 상세 사유가 필수입니다.")
    @ApiResponse(responseCode = "200", description = "요청 거절 성공", useReturnTypeSchema = true)
    @ApiErrorCodes({ErrorCode.REQUEST_NOT_FOUND, ErrorCode.REQUEST_FORBIDDEN, ErrorCode.REQUEST_ALREADY_PROCESSED})
    @PatchMapping("/{requestType}/{requestId}/reject")
    public CustomResponse<Void> reject(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "요청 유형 (PROPOSAL / APPLICATION)", example = "PROPOSAL") @PathVariable RequestType requestType,
            @Parameter(description = "요청 ID", example = "1") @PathVariable Long requestId,
            @Valid @RequestBody RequestDtos.RejectRequest request) {
        requestService.reject(authUser.userId(), requestType, requestId, request);
        return CustomResponse.ok("요청 거절 성공");
    }
}
