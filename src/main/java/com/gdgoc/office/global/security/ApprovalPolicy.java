package com.gdgoc.office.global.security;

import java.util.List;
import org.springframework.util.AntPathMatcher;

/** 승인 정책(§3.5): PENDING 사용자가 호출할 수 있는 API 화이트리스트 */
public final class ApprovalPolicy {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    private record Rule(String method, String pattern) {
    }

    private static final List<Rule> PENDING_ALLOWED = List.of(
            new Rule(null, "/api/v1/auth/**"),
            new Rule("POST", "/api/v1/users/me/profile"),
            new Rule("GET", "/api/v1/users/me"),
            new Rule("GET", "/api/v1/keywords"),
            new Rule("PUT", "/api/v1/users/me/keywords")
    );

    private ApprovalPolicy() {
    }

    public static boolean isPendingAllowed(String method, String path) {
        return PENDING_ALLOWED.stream().anyMatch(rule ->
                (rule.method() == null || rule.method().equalsIgnoreCase(method))
                        && MATCHER.match(rule.pattern(), path));
    }

    public static boolean isAdminPath(String path) {
        return MATCHER.match("/api/v1/admin/**", path);
    }
}
