package com.gdgoc.office.domain.request.controller;

import com.gdgoc.office.domain.request.dto.RequestDtos;
import com.gdgoc.office.domain.request.service.ProposalService;
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

@Tag(name = "05. 같이 해요 제안", description = "멤버에게 같이 해요 제안 보내기")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/proposals")
public class ProposalController {

    private final ProposalService proposalService;

    @Operation(summary = "같이 해요 제안", description = "승인된 멤버에게 같이 해요 제안을 보냅니다. 이미 연결되었거나 대기 중인 제안이 있으면 보낼 수 없습니다.")
    @ApiResponse(responseCode = "201", description = "제안 전송 성공", useReturnTypeSchema = true)
    @ApiErrorCodes({ErrorCode.PROPOSAL_SELF, ErrorCode.PROPOSAL_DUPLICATED, ErrorCode.MEMBER_NOT_FOUND})
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public CustomResponse<RequestDtos.ProposalCreated> propose(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody RequestDtos.ProposalCreateRequest request) {
        return CustomResponse.created("제안 전송 성공", proposalService.propose(authUser.userId(), request));
    }
}
