package com.gdgoc.office.domain.request.service;

import com.gdgoc.office.domain.connection.repository.ConnectionRepository;
import com.gdgoc.office.domain.request.dto.RequestDtos;
import com.gdgoc.office.domain.request.entity.Proposal;
import com.gdgoc.office.domain.request.repository.ProposalRepository;
import com.gdgoc.office.domain.user.entity.User;
import com.gdgoc.office.domain.user.repository.UserRepository;
import com.gdgoc.office.domain.user.service.UserService;
import com.gdgoc.office.global.exception.BusinessException;
import com.gdgoc.office.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProposalService {

    private final ProposalRepository proposalRepository;
    private final ConnectionRepository connectionRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    @Transactional
    public RequestDtos.ProposalCreated propose(Long me, RequestDtos.ProposalCreateRequest request) {
        if (me.equals(request.receiverId())) {
            throw new BusinessException(ErrorCode.PROPOSAL_SELF);
        }
        User receiver = userService.getApprovedMember(request.receiverId());
        if (connectionRepository.existsBetween(me, receiver.getId())
                || proposalRepository.existsPendingBetween(me, receiver.getId())) {
            throw new BusinessException(ErrorCode.PROPOSAL_DUPLICATED);
        }
        Proposal proposal = proposalRepository.save(new Proposal(userRepository.getReferenceById(me), receiver,
                request.reasonType(), request.message().trim()));
        return new RequestDtos.ProposalCreated(proposal.getId());
    }
}
