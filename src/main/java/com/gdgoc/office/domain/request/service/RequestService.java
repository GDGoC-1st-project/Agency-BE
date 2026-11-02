package com.gdgoc.office.domain.request.service;

import com.gdgoc.office.domain.connection.dto.ConnectionResponse;
import com.gdgoc.office.domain.connection.service.ConnectionService;
import com.gdgoc.office.domain.post.entity.Application;
import com.gdgoc.office.domain.post.repository.ApplicationRepository;
import com.gdgoc.office.domain.request.dto.RequestDtos;
import com.gdgoc.office.domain.request.entity.Proposal;
import com.gdgoc.office.domain.request.entity.RejectReasonType;
import com.gdgoc.office.domain.request.entity.RequestStatus;
import com.gdgoc.office.domain.request.entity.RequestType;
import com.gdgoc.office.domain.request.repository.ProposalRepository;
import com.gdgoc.office.global.common.PageResult;
import com.gdgoc.office.global.exception.BusinessException;
import com.gdgoc.office.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RequestService {

    private final ProposalRepository proposalRepository;
    private final ApplicationRepository applicationRepository;
    private final ConnectionService connectionService;

    /** 병합 정렬용 래퍼 */
    private record Sortable<T>(T item, LocalDateTime createdAt, RequestType type, Long id) {
    }

    private static final Comparator<Sortable<?>> NEWEST_FIRST = Comparator
            .comparing((Sortable<?> s) -> s.createdAt(), Comparator.reverseOrder())
            .thenComparing(s -> s.type().ordinal())
            .thenComparing(Sortable::id, Comparator.reverseOrder());

    @Transactional(readOnly = true)
    public PageResult<RequestDtos.ReceivedItem> getReceived(Long me, RequestStatus status, int page, int size) {
        PageRequest limit = mergeLimit(page, size);
        List<Sortable<RequestDtos.ReceivedItem>> merged = new ArrayList<>();
        proposalRepository.findReceived(me, status, limit).forEach(p -> merged.add(new Sortable<>(
                new RequestDtos.ReceivedItem(p.getId(), RequestType.PROPOSAL,
                        new RequestDtos.MemberSummary(p.getSender().getId(), p.getSender().getName()),
                        p.getMessage(), p.getReasonType(), p.getStatus(), p.getCreatedAt()),
                p.getCreatedAt(), RequestType.PROPOSAL, p.getId())));
        applicationRepository.findReceived(me, status, limit).forEach(a -> merged.add(new Sortable<>(
                new RequestDtos.ReceivedItem(a.getId(), RequestType.APPLICATION,
                        new RequestDtos.MemberSummary(a.getApplicant().getId(), a.getApplicant().getName()),
                        a.getPost().getTitle(), null, a.getStatus(), a.getCreatedAt()),
                a.getCreatedAt(), RequestType.APPLICATION, a.getId())));
        return slice(merged, page, size);
    }

    @Transactional(readOnly = true)
    public PageResult<RequestDtos.SentItem> getSent(Long me, RequestStatus status, int page, int size) {
        PageRequest limit = mergeLimit(page, size);
        List<Sortable<RequestDtos.SentItem>> merged = new ArrayList<>();
        proposalRepository.findSent(me, status, limit).forEach(p -> merged.add(new Sortable<>(
                new RequestDtos.SentItem(p.getId(), RequestType.PROPOSAL,
                        new RequestDtos.MemberSummary(p.getReceiver().getId(), p.getReceiver().getName()),
                        p.getMessage(), p.getStatus(),
                        rejectReason(p.getStatus(), p.getRejectType(), p.getRejectDetail()), p.getCreatedAt()),
                p.getCreatedAt(), RequestType.PROPOSAL, p.getId())));
        applicationRepository.findSent(me, status, limit).forEach(a -> merged.add(new Sortable<>(
                new RequestDtos.SentItem(a.getId(), RequestType.APPLICATION,
                        new RequestDtos.MemberSummary(a.getPost().getAuthor().getId(), a.getPost().getAuthor().getName()),
                        a.getPost().getTitle(), a.getStatus(),
                        rejectReason(a.getStatus(), a.getRejectType(), a.getRejectDetail()), a.getCreatedAt()),
                a.getCreatedAt(), RequestType.APPLICATION, a.getId())));
        return slice(merged, page, size);
    }

    @Transactional
    public ConnectionResponse.Connected accept(Long me, RequestType type, Long requestId) {
        if (type == RequestType.PROPOSAL) {
            Proposal proposal = getProposalForReceiver(me, requestId);
            proposal.accept();
            return connectionService.connect(proposal.getSender(), proposal.getReceiver(),
                    RequestType.PROPOSAL, proposal.getId());
        }
        Application application = getApplicationForReceiver(me, requestId);
        application.accept();
        return connectionService.connect(application.getApplicant(), application.getPost().getAuthor(),
                RequestType.APPLICATION, application.getId());
    }

    @Transactional
    public void reject(Long me, RequestType type, Long requestId, RequestDtos.RejectRequest request) {
        String detail = request.reasonDetail() == null || request.reasonDetail().isBlank()
                ? null : request.reasonDetail().trim();
        if (request.reasonType() == RejectReasonType.ETC && detail == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        if (type == RequestType.PROPOSAL) {
            getProposalForReceiver(me, requestId).reject(request.reasonType(), detail);
        } else {
            getApplicationForReceiver(me, requestId).reject(request.reasonType(), detail);
        }
    }

    private Proposal getProposalForReceiver(Long me, Long proposalId) {
        Proposal proposal = proposalRepository.findWithUsersById(proposalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REQUEST_NOT_FOUND));
        if (!proposal.getReceiver().getId().equals(me)) {
            throw new BusinessException(ErrorCode.REQUEST_FORBIDDEN);
        }
        return proposal;
    }

    private Application getApplicationForReceiver(Long me, Long applicationId) {
        Application application = applicationRepository.findWithPostById(applicationId)
                .filter(a -> !a.getPost().isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.REQUEST_NOT_FOUND));
        if (!application.getPost().isAuthor(me)) {
            throw new BusinessException(ErrorCode.REQUEST_FORBIDDEN);
        }
        return application;
    }

    private static RequestDtos.RejectReason rejectReason(RequestStatus status, RejectReasonType type, String detail) {
        return status == RequestStatus.REJECTED ? new RequestDtos.RejectReason(type, detail) : null;
    }

    /** 각 테이블에서 (page+1)*size+1 건씩 조회한다 (§6). */
    private static PageRequest mergeLimit(int page, int size) {
        return PageRequest.of(0, (page + 1) * size + 1);
    }

    private static <T> PageResult<T> slice(List<Sortable<T>> merged, int page, int size) {
        merged.sort(NEWEST_FIRST);
        int from = Math.min(page * size, merged.size());
        int to = Math.min(from + size, merged.size());
        List<T> items = merged.subList(from, to).stream().map(Sortable::item).toList();
        boolean hasNext = merged.size() > (page + 1) * size;
        return PageResult.of(items, page, size, hasNext);
    }
}
