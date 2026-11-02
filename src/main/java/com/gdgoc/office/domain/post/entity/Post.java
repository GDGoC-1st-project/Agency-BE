package com.gdgoc.office.domain.post.entity;

import com.gdgoc.office.domain.keyword.entity.Keyword;
import com.gdgoc.office.domain.user.entity.User;
import com.gdgoc.office.global.common.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "posts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, length = 50)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private boolean deleted;

    @ElementCollection
    @CollectionTable(name = "post_required_roles", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "role", nullable = false, length = 100)
    private List<String> requiredRoles = new ArrayList<>();

    @OrderBy("id asc")
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostKeyword> postKeywords = new ArrayList<>();

    public Post(User author, String title, String description, List<String> requiredRoles, Collection<Keyword> keywords) {
        this.author = author;
        this.title = title;
        this.description = description;
        this.deleted = false;
        this.requiredRoles.addAll(requiredRoles);
        keywords.forEach(keyword -> this.postKeywords.add(new PostKeyword(this, keyword)));
    }

    public boolean isAuthor(Long userId) {
        return author.getId().equals(userId);
    }

    public void changeTitle(String title) {
        this.title = title;
    }

    public void changeDescription(String description) {
        this.description = description;
    }

    public void replaceRequiredRoles(List<String> roles) {
        this.requiredRoles.clear();
        this.requiredRoles.addAll(roles);
    }

    /** 유지할 키워드는 그대로 두고, 빠진 키워드는 삭제, 새 키워드는 추가한다 (UNIQUE 충돌 방지). */
    public void replaceKeywords(Collection<Keyword> keywords) {
        Set<Long> newIds = new LinkedHashSet<>();
        keywords.forEach(keyword -> newIds.add(keyword.getId()));
        this.postKeywords.removeIf(pk -> !newIds.contains(pk.getKeyword().getId()));
        Set<Long> existing = new LinkedHashSet<>();
        this.postKeywords.forEach(pk -> existing.add(pk.getKeyword().getId()));
        keywords.stream()
                .filter(keyword -> !existing.contains(keyword.getId()))
                .forEach(keyword -> this.postKeywords.add(new PostKeyword(this, keyword)));
    }

    public List<String> keywordNames() {
        return postKeywords.stream().map(pk -> pk.getKeyword().getName()).toList();
    }

    public void delete() {
        this.deleted = true;
    }
}
