package com.gdgoc.office.domain.keyword.entity;

import com.gdgoc.office.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "user_keywords", uniqueConstraints = @UniqueConstraint(
        name = "uk_user_keywords_user_keyword_relation", columnNames = {"user_id", "keyword_id", "relation"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "keyword_id", nullable = false)
    private Keyword keyword;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Relation relation;

    public UserKeyword(User user, Keyword keyword, Relation relation) {
        this.user = user;
        this.keyword = keyword;
        this.relation = relation;
    }
}
