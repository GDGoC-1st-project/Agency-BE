package com.gdgoc.office.domain.post.service;

import com.gdgoc.office.domain.keyword.entity.Keyword;
import com.gdgoc.office.domain.keyword.service.KeywordService;
import com.gdgoc.office.domain.post.dto.PostRequest;
import com.gdgoc.office.domain.post.dto.PostResponse;
import com.gdgoc.office.domain.post.entity.Application;
import com.gdgoc.office.domain.post.entity.Post;
import com.gdgoc.office.domain.post.repository.ApplicationRepository;
import com.gdgoc.office.domain.post.repository.PostRepository;
import com.gdgoc.office.domain.user.entity.User;
import com.gdgoc.office.domain.user.repository.UserRepository;
import com.gdgoc.office.global.common.PageResult;
import com.gdgoc.office.global.exception.BusinessException;
import com.gdgoc.office.global.exception.ErrorCode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostService {

    public static final String SORT_LATEST = "LATEST";

    private final PostRepository postRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final KeywordService keywordService;

    @Transactional(readOnly = true)
    public PageResult<PostResponse.ListItem> getPosts(Long me, String sort, int page, int size) {
        if (!SORT_LATEST.equalsIgnoreCase(sort)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        return PageResult.of(postRepository.findLatest(me, PageRequest.of(page, size)), PostResponse.ListItem::from);
    }

    @Transactional(readOnly = true)
    public PostResponse.Detail getPost(Long me, Long postId) {
        Post post = getActivePost(postId);
        return new PostResponse.Detail(post.getId(), post.getTitle(), post.getDescription(),
                List.copyOf(post.getRequiredRoles()), post.keywordNames(),
                new PostResponse.Author(post.getAuthor().getId(), post.getAuthor().getName()),
                post.isAuthor(me), applicationRepository.existsByPostIdAndApplicantId(postId, me),
                post.getCreatedAt());
    }

    @Transactional
    public PostResponse.Created create(Long me, PostRequest.Create request) {
        List<Keyword> keywords = resolveKeywords(request.keywordIds());
        User author = userRepository.getReferenceById(me);
        Post post = postRepository.save(new Post(author, request.title().trim(), request.description(),
                trimRoles(request.requiredRoles()), keywords));
        return new PostResponse.Created(post.getId());
    }

    @Transactional
    public PostResponse.Applied apply(Long me, Long postId) {
        Post post = getActivePost(postId);
        if (post.isAuthor(me)) {
            throw new BusinessException(ErrorCode.APPLICATION_SELF);
        }
        if (applicationRepository.existsByPostIdAndApplicantId(postId, me)) {
            throw new BusinessException(ErrorCode.APPLICATION_DUPLICATED);
        }
        Application application = applicationRepository.save(new Application(post, userRepository.getReferenceById(me)));
        return new PostResponse.Applied(application.getId());
    }

    @Transactional
    public void update(Long me, Long postId, PostRequest.Update request) {
        Post post = getActivePost(postId);
        if (!post.isAuthor(me)) {
            throw new BusinessException(ErrorCode.POST_UPDATE_FORBIDDEN);
        }
        if (request.keywordIds() != null) {
            post.replaceKeywords(resolveKeywords(request.keywordIds()));
        }
        if (request.title() != null) {
            post.changeTitle(request.title().trim());
        }
        if (request.description() != null) {
            post.changeDescription(request.description());
        }
        if (request.requiredRoles() != null) {
            post.replaceRequiredRoles(trimRoles(request.requiredRoles()));
        }
    }

    @Transactional
    public void delete(Long me, Long postId) {
        Post post = getActivePost(postId);
        if (!post.isAuthor(me)) {
            throw new BusinessException(ErrorCode.POST_DELETE_FORBIDDEN);
        }
        post.delete();
    }

    private Post getActivePost(Long postId) {
        return postRepository.findActiveById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
    }

    private List<Keyword> resolveKeywords(List<Long> keywordIds) {
        if (keywordIds == null || keywordIds.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Long> ids = new LinkedHashSet<>(keywordIds);
        Map<Long, Keyword> found = keywordService.findAllOrThrow(ids);
        List<Keyword> ordered = new ArrayList<>();
        ids.forEach(id -> ordered.add(found.get(id)));
        return ordered;
    }

    private static List<String> trimRoles(List<String> roles) {
        return roles.stream().map(String::trim).toList();
    }
}
