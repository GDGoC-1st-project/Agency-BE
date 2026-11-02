package com.gdgoc.office.domain.keyword.controller;

import com.gdgoc.office.domain.keyword.dto.KeywordResponse;
import com.gdgoc.office.domain.keyword.dto.SaveMyKeywordsRequest;
import com.gdgoc.office.domain.keyword.service.KeywordService;
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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "03. 키워드", description = "키워드 검색(자동완성), 내 키워드 저장/조회")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequiredArgsConstructor
public class KeywordController {

    private final KeywordService keywordService;

    @Operation(summary = "키워드 검색(자동완성)",
            description = "공식(APPROVED) 키워드 중 이름이 부분 일치(대소문자 무시)하는 키워드를 앞부분 일치 우선으로 반환합니다. 승인 대기 사용자도 호출할 수 있습니다.")
    @ApiResponse(responseCode = "200", description = "키워드 검색 성공", useReturnTypeSchema = true)
    @GetMapping("/api/v1/keywords")
    public CustomResponse<KeywordResponse.SearchResult> search(
            @Parameter(description = "검색어 (1자 이상)", example = "spr", required = true)
            @RequestParam @NotBlank String q,
            @Parameter(description = "최대 결과 개수 (기본 10, 최대 100)", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return CustomResponse.ok("키워드 검색 성공", keywordService.search(q, size));
    }

    @Operation(summary = "내 키워드 저장",
            description = "할 수 있어요 / 배우고 있어요 / 관심 있어요 관계별 키워드를 전달한 목록으로 전체 교체합니다. "
                    + "같은 키워드를 서로 다른 관계에 등록할 수 있으며, 저장에 성공하면 키워드 등록 완료로 표시됩니다. 승인 대기 사용자도 호출할 수 있습니다.")
    @ApiResponse(responseCode = "200", description = "내 키워드 저장 성공", useReturnTypeSchema = true)
    @ApiErrorCodes({ErrorCode.KEYWORD_LIMIT_EXCEEDED, ErrorCode.KEYWORD_NOT_FOUND})
    @PutMapping("/api/v1/users/me/keywords")
    public CustomResponse<Void> saveMyKeywords(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody SaveMyKeywordsRequest request) {
        keywordService.saveMyKeywords(authUser.userId(), request);
        return CustomResponse.ok("내 키워드 저장 성공");
    }

    @Operation(summary = "내 키워드 조회", description = "내가 등록한 키워드를 관계별로 조회합니다.")
    @ApiResponse(responseCode = "200", description = "내 키워드 조회 성공", useReturnTypeSchema = true)
    @GetMapping("/api/v1/users/me/keywords")
    public CustomResponse<KeywordResponse.Grouped> getMyKeywords(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUser authUser) {
        return CustomResponse.ok("내 키워드 조회 성공", keywordService.getKeywordsOf(authUser.userId()));
    }
}
