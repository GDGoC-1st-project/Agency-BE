package com.gdgoc.office.domain.keyword.service;

import com.gdgoc.office.domain.keyword.entity.UserKeyword;
import com.gdgoc.office.domain.keyword.repository.UserKeywordRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 대표 키워드(최대 3개) 계산 (§6).
 * CAN → LEARNING → INTERESTED 순, 각 관계 내에서는 등록 순서(user_keywords.id 오름차순).
 * 검색어가 있으면 검색어와 일치(부분 일치, 대소문자 무시)하는 키워드를 먼저 담는다. 같은 키워드는 한 번만 담는다.
 */
@Component
@RequiredArgsConstructor
public class RepresentativeKeywordResolver {

    public static final int MAX_REPRESENTATIVE = 3;

    private final UserKeywordRepository userKeywordRepository;

    public Map<Long, List<String>> resolve(Collection<Long> userIds) {
        return resolve(userIds, null);
    }

    public Map<Long, List<String>> resolve(Collection<Long> userIds, String searchKeyword) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<UserKeyword>> byUser = userKeywordRepository.findAllByUserIdInWithKeyword(userIds).stream()
                .collect(Collectors.groupingBy(uk -> uk.getUser().getId()));

        String needle = (searchKeyword == null || searchKeyword.isBlank()) ? null : searchKeyword.trim().toLowerCase();
        Comparator<UserKeyword> order = Comparator
                .comparing((UserKeyword uk) -> needle != null && uk.getKeyword().getName().toLowerCase().contains(needle) ? 0 : 1)
                .thenComparing(UserKeyword::getRelation)
                .thenComparing(UserKeyword::getId);

        Map<Long, List<String>> result = new HashMap<>();
        for (Long userId : userIds) {
            List<UserKeyword> keywords = new ArrayList<>(byUser.getOrDefault(userId, List.of()));
            keywords.sort(order);
            Set<Long> seen = new LinkedHashSet<>();
            List<String> names = new ArrayList<>();
            for (UserKeyword uk : keywords) {
                if (names.size() >= MAX_REPRESENTATIVE) {
                    break;
                }
                if (seen.add(uk.getKeyword().getId())) {
                    names.add(uk.getKeyword().getName());
                }
            }
            result.put(userId, names);
        }
        return result;
    }
}
