package com.gdgoc.office.global.security;

import com.gdgoc.office.domain.user.entity.Role;
import com.gdgoc.office.domain.user.entity.UserStatus;

/** 인증된 사용자 정보 (SecurityContext principal) */
public record AuthUser(Long userId, Role role, UserStatus status) {

    public boolean isApproved() {
        return status == UserStatus.APPROVED;
    }
}
