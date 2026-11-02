package com.gdgoc.office.domain.report.service;

import com.gdgoc.office.domain.chat.repository.MessageRepository;
import com.gdgoc.office.domain.post.repository.PostRepository;
import com.gdgoc.office.domain.report.dto.ReportDtos;
import com.gdgoc.office.domain.report.entity.Report;
import com.gdgoc.office.domain.report.entity.ReportTargetType;
import com.gdgoc.office.domain.report.repository.ReportRepository;
import com.gdgoc.office.domain.user.entity.UserStatus;
import com.gdgoc.office.domain.user.repository.UserRepository;
import com.gdgoc.office.global.exception.BusinessException;
import com.gdgoc.office.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final MessageRepository messageRepository;

    @Transactional
    public ReportDtos.Created report(Long me, ReportDtos.CreateRequest request) {
        if (!targetExists(request.targetType(), request.targetId())) {
            throw new BusinessException(ErrorCode.REPORT_TARGET_NOT_FOUND);
        }
        if (reportRepository.existsByReporterIdAndTargetTypeAndTargetId(me, request.targetType(), request.targetId())) {
            throw new BusinessException(ErrorCode.REPORT_DUPLICATED);
        }
        Report report = reportRepository.save(new Report(userRepository.getReferenceById(me),
                request.targetType(), request.targetId(), request.reason().trim()));
        return new ReportDtos.Created(report.getId());
    }

    private boolean targetExists(ReportTargetType type, Long targetId) {
        return switch (type) {
            case USER -> userRepository.findByIdAndStatus(targetId, UserStatus.APPROVED).isPresent();
            case POST -> postRepository.findActiveById(targetId).isPresent();
            case MESSAGE -> messageRepository.existsById(targetId);
        };
    }
}
