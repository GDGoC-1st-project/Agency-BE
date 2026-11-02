package com.gdgoc.office.domain.member.controller;

import com.gdgoc.office.domain.keyword.entity.Relation;
import com.gdgoc.office.domain.member.dto.MemberResponse;
import com.gdgoc.office.domain.member.service.MemberService;
import com.gdgoc.office.domain.user.entity.Department;
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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "04. 멤버 탐색", description = "키워드 기반 멤버 검색, 멤버 프로필 조회")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/members")
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "멤버 검색",
            description = "승인된 멤버를 검색합니다. 본인과 내가 차단한 멤버는 제외되며 id 내림차순으로 정렬됩니다. "
                    + "keyword가 있으면 해당 키워드(부분 일치)를 가진 멤버만, relation을 함께 주면 그 관계로 등록한 경우만 조회합니다.")
    @ApiResponse(responseCode = "200", description = "멤버 검색 성공", useReturnTypeSchema = true)
    @GetMapping
    public CustomResponse<PageResult<MemberResponse.SearchItem>> search(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "검색 키워드 (부분 일치)", example = "Spring")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "키워드 관계 (CAN / LEARNING / INTERESTED), keyword와 함께 사용", example = "CAN")
            @RequestParam(required = false) Relation relation,
            @Parameter(description = "소속 분야 (DEV / AI / DESIGN)", example = "DEV")
            @RequestParam(required = false) Department department,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "페이지 크기 (기본 20, 최대 100)", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return CustomResponse.ok("멤버 검색 성공",
                memberService.search(authUser.userId(), keyword, relation, department, page, size));
    }

    @Operation(summary = "멤버 프로필 조회", description = "승인된 멤버의 프로필과 관계별 키워드를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "멤버 프로필 조회 성공", useReturnTypeSchema = true)
    @ApiErrorCodes({ErrorCode.MEMBER_NOT_FOUND})
    @GetMapping("/{memberId}")
    public CustomResponse<MemberResponse.Profile> getProfile(
            @Parameter(description = "멤버 ID", example = "3") @PathVariable Long memberId) {
        return CustomResponse.ok("멤버 프로필 조회 성공", memberService.getProfile(memberId));
    }
}
