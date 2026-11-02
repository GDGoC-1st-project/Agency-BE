package com.gdgoc.office.domain.post.entity;

import com.gdgoc.office.domain.keyword.entity.Keyword;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "post_keywords", uniqueConstraints = @UniqueConstraint(
        name = "uk_post_keywords_post_keyword", columnNames = {"post_id", "keyword_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "keyword_id", nullable = false)
    private Keyword keyword;

    PostKeyword(Post post, Keyword keyword) {
        this.post = post;
        this.keyword = keyword;
    }
}
