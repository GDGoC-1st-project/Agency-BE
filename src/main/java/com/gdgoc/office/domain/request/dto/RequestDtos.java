package com.gdgoc.office.domain.request.dto;

import com.gdgoc.office.domain.request.entity.ReasonType;
import com.gdgoc.office.domain.request.entity.RejectReasonType;
import com.gdgoc.office.domain.request.entity.RequestStatus;
import com.gdgoc.office.domain.request.entity.RequestType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public final class RequestDtos {

    private RequestDtos() {
    }

    @Schema(description = "같이 해요 제안 요청")
    public record ProposalCreateRequest(
            @Schema(description = "제안 받을 멤버 ID", example = "3")
            @NotNull
            Long receiverId,

            @Schema(description = "제안 사유 (PROJECT / STUDY / CONTEST / COFFEE_CHAT / HOBBY / ETC)", example = "PROJECT")
            @NotNull
            ReasonType reasonType,

            @Schema(description = "제안 메시지 (200자 이내)", example = "같이 해커톤 나가실래요?")
            @NotBlank @Size(max = 200)
            String message
    ) {
    }

    @Schema(description = "제안 전송 결과")
    public record ProposalCreated(
            @Schema(description = "생성된 제안 ID", example = "1") Long proposalId
    ) {
    }

    @Schema(description = "요청 거절 요청")
    public record RejectRequest(
            @Schema(description = "거절 사유 (SCHEDULE / NOT_MATCHED / CLOSED / ETC)", example = "SCHEDULE")
            @NotNull
            RejectReasonType reasonType,

            @Schema(description = "상세 사유 (200자 이내, reasonType이 ETC이면 필수)", example = "이번 학기는 일정이 맞지 않아요.")
            @Size(max = 200)
            String reasonDetail
    ) {
    }

    @Schema(description = "요청 상대 멤버")
    public record MemberSummary(
            @Schema(description = "멤버 ID", example = "3") Long memberId,
            @Schema(description = "이름", example = "김지디") String name
    ) {
    }

    @Schema(description = "받은 요청 항목")
    public record ReceivedItem(
            @Schema(description = "요청 ID (requestType별로 따로 채번)", example = "1") Long requestId,
            @Schema(description = "요청 유형 (PROPOSAL: 같이 해요 / APPLICATION: 지원)", example = "PROPOSAL") RequestType requestType,
            @Schema(description = "보낸 멤버") MemberSummary sender,
            @Schema(description = "내용 (PROPOSAL: 제안 메시지 / APPLICATION: 지원한 모집글 제목)", example = "같이 해커톤 나가실래요?") String content,
            @Schema(description = "제안 사유 (PROPOSAL만 값이 있음)", example = "PROJECT", nullable = true) ReasonType reasonType,
            @Schema(description = "상태", example = "PENDING") RequestStatus status,
            @Schema(description = "요청 시각", example = "2026-10-05T10:00:00") LocalDateTime createdAt
    ) {
    }

    @Schema(description = "거절 사유")
    public record RejectReason(
            @Schema(description = "거절 사유 유형", example = "SCHEDULE") RejectReasonType type,
            @Schema(description = "상세 사유", example = "이번 학기는 일정이 맞지 않아요.", nullable = true) String detail
    ) {
    }

    @Schema(description = "보낸 요청 항목")
    public record SentItem(
            @Schema(description = "요청 ID (requestType별로 따로 채번)", example = "1") Long requestId,
            @Schema(description = "요청 유형 (PROPOSAL: 같이 해요 / APPLICATION: 지원)", example = "APPLICATION") RequestType requestType,
            @Schema(description = "받는 멤버 (APPLICATION이면 모집글 작성자)") MemberSummary receiver,
            @Schema(description = "내용 (PROPOSAL: 제안 메시지 / APPLICATION: 지원한 모집글 제목)", example = "해커톤 같이 나갈 백엔드 개발자 구해요") String content,
            @Schema(description = "상태", example = "REJECTED") RequestStatus status,
            @Schema(description = "거절 사유 (REJECTED일 때만 값이 있음)", nullable = true) RejectReason rejectReason,
            @Schema(description = "요청 시각", example = "2026-10-05T10:00:00") LocalDateTime createdAt
    ) {
    }
}
