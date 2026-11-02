package com.gdgoc.office.domain.user.entity;

import com.gdgoc.office.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "login_id", nullable = false, unique = true, length = 20)
    private String loginId;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(name = "keyword_registered", nullable = false)
    private boolean keywordRegistered;

    private User(String loginId, String password, String name, UserStatus status, Role role, boolean keywordRegistered) {
        this.loginId = loginId;
        this.password = password;
        this.name = name;
        this.status = status;
        this.role = role;
        this.keywordRegistered = keywordRegistered;
    }

    public static User createMember(String loginId, String encodedPassword, String name) {
        return new User(loginId, encodedPassword, name, UserStatus.PENDING, Role.MEMBER, false);
    }

    public static User createAdmin(String loginId, String encodedPassword, String name) {
        return new User(loginId, encodedPassword, name, UserStatus.APPROVED, Role.ADMIN, true);
    }

    public boolean isApproved() {
        return status == UserStatus.APPROVED;
    }

    public void approve() {
        this.status = UserStatus.APPROVED;
    }

    public void markKeywordRegistered() {
        this.keywordRegistered = true;
    }
}
