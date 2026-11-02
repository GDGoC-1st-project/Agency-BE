package com.gdgoc.office.domain.member.service;

import com.gdgoc.office.domain.keyword.entity.Relation;
import com.gdgoc.office.domain.keyword.service.KeywordService;
import com.gdgoc.office.domain.keyword.service.RepresentativeKeywordResolver;
import com.gdgoc.office.domain.member.dto.MemberResponse;
import com.gdgoc.office.domain.member.dto.MemberRow;
import com.gdgoc.office.domain.member.repository.MemberRepository;
import com.gdgoc.office.domain.user.entity.Department;
import com.gdgoc.office.domain.user.entity.Profile;
import com.gdgoc.office.domain.user.entity.User;
import com.gdgoc.office.domain.user.repository.ProfileRepository;
import com.gdgoc.office.domain.user.service.UserService;
import com.gdgoc.office.global.common.PageResult;
import com.gdgoc.office.global.util.LikeEscaper;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final ProfileRepository profileRepository;
    private final UserService userService;
    private final KeywordService keywordService;
    private final RepresentativeKeywordResolver representativeKeywordResolver;

    @Transactional(readOnly = true)
    public PageResult<MemberResponse.SearchItem> search(Long me, String keyword, Relation relation,
                                                         Department department, int page, int size) {
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        String pattern = hasKeyword ? LikeEscaper.contains(keyword) : null;
        // relation 은 keyword 와 함께 사용할 때만 적용한다.
        Relation relationFilter = hasKeyword ? relation : null;

        Slice<MemberRow> rows = memberRepository.search(me, pattern, relationFilter, department,
                PageRequest.of(page, size));
        List<Long> ids = rows.getContent().stream().map(MemberRow::id).toList();
        Map<Long, List<String>> keywords = representativeKeywordResolver.resolve(ids, hasKeyword ? keyword : null);

        return PageResult.of(rows, row -> new MemberResponse.SearchItem(
                row.id(), row.name(), row.department(), keywords.getOrDefault(row.id(), List.of())));
    }

    @Transactional(readOnly = true)
    public MemberResponse.Profile getProfile(Long memberId) {
        User member = userService.getApprovedMember(memberId);
        Profile profile = profileRepository.findByUserId(memberId).orElse(null);
        return new MemberResponse.Profile(member.getId(), member.getName(),
                profile == null ? null : profile.getBio(),
                profile == null ? null : profile.getMajor(),
                profile == null ? null : profile.getDepartment(),
                profile == null ? null : profile.getContact(),
                keywordService.getKeywordsOf(memberId));
    }
}
