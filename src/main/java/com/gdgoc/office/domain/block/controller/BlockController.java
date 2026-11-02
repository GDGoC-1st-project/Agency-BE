package com.gdgoc.office.domain.block.controller;

import com.gdgoc.office.domain.block.dto.BlockRequest;
import com.gdgoc.office.domain.block.service.BlockService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "07. 차단", description = "멤버 차단/차단 해제")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/blocks")
public class BlockController {

    private final BlockService blockService;

    @Operation(summary = "차단", description = "멤버를 차단합니다. 차단한 멤버는 멤버 검색, 모집글 목록, 요청 목록, 채팅방 목록에서 제외됩니다.")
    @ApiResponse(responseCode = "201", description = "차단 성공", useReturnTypeSchema = true)
    @ApiErrorCodes({ErrorCode.BLOCK_SELF, ErrorCode.BLOCK_DUPLICATED, ErrorCode.MEMBER_NOT_FOUND})
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public CustomResponse<Void> block(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody BlockRequest request) {
        blockService.block(authUser.userId(), request.targetMemberId());
        return CustomResponse.created("차단 성공");
    }

    @Operation(summary = "차단 해제", description = "차단한 멤버의 차단을 해제합니다.")
    @ApiResponse(responseCode = "200", description = "차단 해제 성공", useReturnTypeSchema = true)
    @ApiErrorCodes({ErrorCode.BLOCK_NOT_FOUND})
    @DeleteMapping("/{targetMemberId}")
    public CustomResponse<Void> unblock(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "차단 해제할 멤버 ID", example = "3") @PathVariable Long targetMemberId) {
        blockService.unblock(authUser.userId(), targetMemberId);
        return CustomResponse.ok("차단 해제 성공");
    }
}
