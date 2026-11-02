package com.gdgoc.office.domain.keyword.service;

import com.gdgoc.office.domain.keyword.dto.KeywordResponse;
import com.gdgoc.office.domain.keyword.dto.SaveMyKeywordsRequest;
import com.gdgoc.office.domain.keyword.entity.Keyword;
import com.gdgoc.office.domain.keyword.entity.Relation;
import com.gdgoc.office.domain.keyword.entity.UserKeyword;
import com.gdgoc.office.domain.keyword.repository.KeywordRepository;
import com.gdgoc.office.domain.keyword.repository.UserKeywordRepository;
import com.gdgoc.office.domain.user.entity.User;
import com.gdgoc.office.domain.user.repository.UserRepository;
import com.gdgoc.office.global.exception.BusinessException;
import com.gdgoc.office.global.exception.ErrorCode;
import com.gdgoc.office.global.util.LikeEscaper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class KeywordService {

    public static final int MAX_KEYWORDS_PER_RELATION = 20;

    private final KeywordRepository keywordRepository;
    private final UserKeywordRepository userKeywordRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public KeywordResponse.SearchResult search(String q, int size) {
        List<KeywordResponse.Item> items = keywordRepository
                .search(LikeEscaper.contains(q), LikeEscaper.startsWith(q), PageRequest.of(0, size)).stream()
                .map(KeywordResponse.Item::from)
                .toList();
        return new KeywordResponse.SearchResult(items);
    }

    @Transactional
    public void saveMyKeywords(Long userId, SaveMyKeywordsRequest request) {
        Map<Relation, Set<Long>> requested = Map.of(
                Relation.CAN, new LinkedHashSet<>(request.can()),
                Relation.LEARNING, new LinkedHashSet<>(request.learning()),
                Relation.INTERESTED, new LinkedHashSet<>(request.interested()));
        if (requested.values().stream().anyMatch(ids -> ids.size() > MAX_KEYWORDS_PER_RELATION)) {
            throw new BusinessException(ErrorCode.KEYWORD_LIMIT_EXCEEDED);
        }

        Set<Long> allIds = requested.values().stream().flatMap(Set::stream).collect(Collectors.toSet());
        findAllOrThrow(allIds);

        // 벌크 삭제(영속성 컨텍스트 초기화) 후 필요한 엔티티를 다시 조회한다.
        userKeywordRepository.deleteAllByUserId(userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));
        Map<Long, Keyword> keywords = findAllOrThrow(allIds);

        List<UserKeyword> toSave = new ArrayList<>();
        for (Relation relation : Relation.values()) {
            for (Long keywordId : requested.get(relation)) {
                toSave.add(new UserKeyword(user, keywords.get(keywordId), relation));
            }
        }
        userKeywordRepository.saveAll(toSave);
        user.markKeywordRegistered();
    }

    @Transactional(readOnly = true)
    public KeywordResponse.Grouped getKeywordsOf(Long userId) {
        List<UserKeyword> userKeywords = userKeywordRepository.findAllByUserIdWithKeyword(userId);
        return new KeywordResponse.Grouped(
                filter(userKeywords, Relation.CAN),
                filter(userKeywords, Relation.LEARNING),
                filter(userKeywords, Relation.INTERESTED));
    }

    /** 전달된 키워드 ID가 모두 존재하는지 확인한다. 하나라도 없으면 KEYWORD404. */
    @Transactional(readOnly = true)
    public Map<Long, Keyword> findAllOrThrow(Collection<Long> keywordIds) {
        if (keywordIds.isEmpty()) {
            return Map.of();
        }
        Set<Long> ids = new LinkedHashSet<>(keywordIds);
        Map<Long, Keyword> found = keywordRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Keyword::getId, Function.identity()));
        if (found.size() != ids.size()) {
            throw new BusinessException(ErrorCode.KEYWORD_NOT_FOUND);
        }
        return found;
    }

    private List<KeywordResponse.Item> filter(List<UserKeyword> userKeywords, Relation relation) {
        return userKeywords.stream()
                .filter(uk -> uk.getRelation() == relation)
                .map(uk -> KeywordResponse.Item.from(uk.getKeyword()))
                .toList();
    }
}
