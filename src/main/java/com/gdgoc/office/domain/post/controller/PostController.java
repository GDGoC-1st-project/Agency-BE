package com.gdgoc.office.domain.post.controller;

import com.gdgoc.office.domain.post.dto.PostRequest;
import com.gdgoc.office.domain.post.dto.PostResponse;
import com.gdgoc.office.domain.post.service.PostService;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "08. 모집", description = "모집글 목록/상세/작성/수정/삭제, 모집글 지원")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/posts")
public class PostController {

    private final PostService postService;

    @Operation(summary = "모집글 목록 조회", description = "삭제되지 않은 모집글을 최신순으로 조회합니다. 내가 차단한 멤버의 글은 제외됩니다.")
    @ApiResponse(responseCode = "200", description = "모집글 목록 조회 성공", useReturnTypeSchema = true)
    @GetMapping
    public CustomResponse<PageResult<PostResponse.ListItem>> getPosts(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "정렬 기준 (현재 LATEST만 지원)", example = "LATEST")
            @RequestParam(defaultValue = PostService.SORT_LATEST) String sort,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "페이지 크기 (기본 20, 최대 100)", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return CustomResponse.ok("모집글 목록 조회 성공", postService.getPosts(authUser.userId(), sort, page, size));
    }

    @Operation(summary = "모집글 상세 조회", description = "모집글 상세 정보와 내가 작성자인지, 지원했는지 여부를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "모집글 상세 조회 성공", useReturnTypeSchema = true)
    @ApiErrorCodes({ErrorCode.POST_NOT_FOUND})
    @GetMapping("/{postId}")
    public CustomResponse<PostResponse.Detail> getPost(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "모집글 ID", example = "1") @PathVariable Long postId) {
        return CustomResponse.ok("모집글 상세 조회 성공", postService.getPost(authUser.userId(), postId));
    }

    @Operation(summary = "모집글 작성", description = "새 모집글을 작성합니다.")
    @ApiResponse(responseCode = "201", description = "모집글 작성 성공", useReturnTypeSchema = true)
    @ApiErrorCodes({ErrorCode.KEYWORD_NOT_FOUND})
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public CustomResponse<PostResponse.Created> create(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody PostRequest.Create request) {
        return CustomResponse.created("모집글 작성 성공", postService.create(authUser.userId(), request));
    }

    @Operation(summary = "모집글 지원", description = "모집글에 지원합니다. 요청 바디는 없습니다.")
    @ApiResponse(responseCode = "201", description = "지원 성공", useReturnTypeSchema = true)
    @ApiErrorCodes({ErrorCode.APPLICATION_DUPLICATED, ErrorCode.APPLICATION_SELF, ErrorCode.POST_NOT_FOUND})
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/{postId}/applications")
    public CustomResponse<PostResponse.Applied> apply(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "지원할 모집글 ID", example = "1") @PathVariable Long postId) {
        return CustomResponse.created("지원 성공", postService.apply(authUser.userId(), postId));
    }

    @Operation(summary = "모집글 수정", description = "작성자만 수정할 수 있으며 전달한 필드만 수정합니다. requiredRoles, keywordIds는 전달 시 전체 교체됩니다.")
    @ApiResponse(responseCode = "200", description = "모집글 수정 성공", useReturnTypeSchema = true)
    @ApiErrorCodes({ErrorCode.POST_NOT_FOUND, ErrorCode.POST_UPDATE_FORBIDDEN, ErrorCode.KEYWORD_NOT_FOUND})
    @PatchMapping("/{postId}")
    public CustomResponse<Void> update(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "수정할 모집글 ID", example = "1") @PathVariable Long postId,
            @Valid @RequestBody PostRequest.Update request) {
        postService.update(authUser.userId(), postId, request);
        return CustomResponse.ok("모집글 수정 성공");
    }

    @Operation(summary = "모집글 삭제", description = "작성자만 삭제할 수 있습니다(soft delete). 이미 성사된 연결과 채팅방은 유지됩니다.")
    @ApiResponse(responseCode = "200", description = "모집글 삭제 성공", useReturnTypeSchema = true)
    @ApiErrorCodes({ErrorCode.POST_NOT_FOUND, ErrorCode.POST_DELETE_FORBIDDEN})
    @DeleteMapping("/{postId}")
    public CustomResponse<Void> delete(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "삭제할 모집글 ID", example = "1") @PathVariable Long postId) {
        postService.delete(authUser.userId(), postId);
        return CustomResponse.ok("모집글 삭제 성공");
    }
}
