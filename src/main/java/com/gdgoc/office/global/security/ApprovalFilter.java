package com.gdgoc.office.global.security;

import com.gdgoc.office.global.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** 인증된 PENDING 사용자가 화이트리스트(§3.5) 밖의 API를 호출하면 AUTH403으로 응답한다. */
@RequiredArgsConstructor
public class ApprovalFilter extends OncePerRequestFilter {

    private final SecurityResponseWriter writer;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthUser authUser
                && !authUser.isApproved()) {
            String path = request.getRequestURI().substring(request.getContextPath().length());
            if (path.startsWith("/api/") && !ApprovalPolicy.isPendingAllowed(request.getMethod(), path)) {
                writer.write(response, ErrorCode.NOT_APPROVED);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
