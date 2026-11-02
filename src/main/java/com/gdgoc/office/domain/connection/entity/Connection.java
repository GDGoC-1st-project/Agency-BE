package com.gdgoc.office.domain.connection.entity;

import com.gdgoc.office.domain.request.entity.RequestType;
import com.gdgoc.office.domain.user.entity.User;
import com.gdgoc.office.global.common.BaseEntity;
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

/** 두 멤버의 연결. user_a 는 항상 id가 작은 쪽으로 정규화한다. */
@Getter
@Entity
@Table(name = "connections", uniqueConstraints = @UniqueConstraint(
        name = "uk_connections_pair", columnNames = {"user_a_id", "user_b_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Connection extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_a_id", nullable = false)
    private User userA;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_b_id", nullable = false)
    private User userB;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private RequestType sourceType;

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    private Connection(User userA, User userB, RequestType sourceType, Long sourceId) {
        this.userA = userA;
        this.userB = userB;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
    }

    public static Connection of(User first, User second, RequestType sourceType, Long sourceId) {
        if (first.getId() < second.getId()) {
            return new Connection(first, second, sourceType, sourceId);
        }
        return new Connection(second, first, sourceType, sourceId);
    }

    public User partnerOf(Long userId) {
        return userA.getId().equals(userId) ? userB : userA;
    }
}
