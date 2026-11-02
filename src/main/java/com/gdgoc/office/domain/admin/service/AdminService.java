package com.gdgoc.office.domain.admin.service;

import com.gdgoc.office.domain.admin.dto.AdminDtos;
import com.gdgoc.office.domain.chat.entity.Message;
import com.gdgoc.office.domain.chat.repository.MessageRepository;
import com.gdgoc.office.domain.keyword.service.RepresentativeKeywordResolver;
import com.gdgoc.office.domain.post.entity.Post;
import com.gdgoc.office.domain.post.repository.PostRepository;
import com.gdgoc.office.domain.report.entity.Report;
import com.gdgoc.office.domain.report.entity.ReportStatus;
import com.gdgoc.office.domain.report.entity.ReportTargetType;
import com.gdgoc.office.domain.report.repository.ReportRepository;
import com.gdgoc.office.domain.user.entity.Profile;
import com.gdgoc.office.domain.user.entity.User;
import com.gdgoc.office.domain.user.repository.ProfileRepository;
import com.gdgoc.office.domain.user.repository.UserRepository;
import com.gdgoc.office.global.common.CountedPageResult;
import com.gdgoc.office.global.exception.BusinessException;
import com.gdgoc.office.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final ReportRepository reportRepository;
    private final PostRepository postRepository;
    private final MessageRepository messageRepository;
    private final RepresentativeKeywordResolver representativeKeywordResolver;

    @Transactional(readOnly = true)
    public AdminDtos.Dashboard getDashboard() {
        return new AdminDtos.Dashboard((int) userRepository.countPendingSignups(),
                (int) reportRepository.countByStatus(ReportStatus.PENDING));
    }

    @Transactional(readOnly = true)
    public CountedPageResult<AdminDtos.SignupItem> getPendingSignups(int page, int size) {
        Page<User> users = userRepository.findPendingSignups(PageRequest.of(page, size));
        List<Long> ids = users.getContent().stream().map(User::getId).toList();
        Map<Long, Profile> profiles = ids.isEmpty() ? Map.of()
                : profileRepository.findAllByUserIdIn(ids).stream()
                        .collect(Collectors.toMap(p -> p.getUser().getId(), Function.identity()));
        Map<Long, List<String>> keywords = representativeKeywordResolver.resolve(ids);

        return CountedPageResult.of(users, user -> {
            Profile profile = profiles.get(user.getId());
            return new AdminDtos.SignupItem(user.getId(), user.getName(),
                    profile == null ? null : profile.getMajor(),
                    profile == null ? null : profile.getDepartment(),
                    keywords.getOrDefault(user.getId(), List.of()));
        });
    }

    @Transactional
    public void approve(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (user.isApproved()) {
            throw new BusinessException(ErrorCode.USER_ALREADY_APPROVED);
        }
        user.approve();
    }

    @Transactional(readOnly = true)
    public CountedPageResult<AdminDtos.ReportItem> getReports(ReportStatus status, int page, int size) {
        Page<Report> reports = reportRepository.findByStatus(status, PageRequest.of(page, size));
        Map<Long, String> userNames = namesOf(reports, ReportTargetType.USER, ids ->
                userRepository.findAllById(ids).stream().collect(Collectors.toMap(User::getId, User::getName)));
        Map<Long, String> postTitles = namesOf(reports, ReportTargetType.POST, ids ->
                postRepository.findAllById(ids).stream().collect(Collectors.toMap(Post::getId, Post::getTitle)));
        Map<Long, String> messageSenders = namesOf(reports, ReportTargetType.MESSAGE, ids ->
                messageRepository.findAllWithSenderByIdIn(ids).stream()
                        .collect(Collectors.toMap(Message::getId, m -> m.getSender().getName())));

        return CountedPageResult.of(reports, report -> {
            String targetName = switch (report.getTargetType()) {
                case USER -> userNames.get(report.getTargetId());
                case POST -> postTitles.get(report.getTargetId());
                case MESSAGE -> messageSenders.get(report.getTargetId());
            };
            return new AdminDtos.ReportItem(report.getId(), report.getTargetType(), report.getTargetId(), targetName,
                    report.getReporter().getName(), report.getReason(), report.getStatus(), report.getCreatedAt());
        });
    }

    @Transactional
    public void resolveReport(Long reportId, String memo) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_FOUND));
        if (report.getStatus() == ReportStatus.RESOLVED) {
            throw new BusinessException(ErrorCode.REPORT_ALREADY_RESOLVED);
        }
        String trimmed = memo == null || memo.isBlank() ? null : memo.trim();
        report.resolve(trimmed, LocalDateTime.now(ZoneId.of("Asia/Seoul")));
    }

    private static Map<Long, String> namesOf(Page<Report> reports, ReportTargetType type,
                                             Function<List<Long>, Map<Long, String>> loader) {
        List<Long> ids = reports.getContent().stream()
                .filter(r -> r.getTargetType() == type)
                .map(Report::getTargetId)
                .distinct()
                .toList();
        return ids.isEmpty() ? Map.of() : loader.apply(ids);
    }
}
