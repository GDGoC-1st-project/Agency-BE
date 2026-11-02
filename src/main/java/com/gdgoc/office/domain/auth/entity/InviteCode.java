package com.gdgoc.office.domain.auth.entity;

import com.gdgoc.office.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "invite_codes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InviteCode extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    /** 초대 대상자 이름 (가입 시 사용자 이름으로 사용) */
    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private boolean used;

    @Column(name = "used_by")
    private Long usedBy;

    public InviteCode(String code, String name) {
        this.code = code;
        this.name = name;
        this.used = false;
    }

    public void use(Long userId) {
        this.used = true;
        this.usedBy = userId;
    }
}
