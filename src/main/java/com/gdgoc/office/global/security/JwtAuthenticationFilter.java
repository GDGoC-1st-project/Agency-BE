package com.gdgoc.office.global.security;

import com.gdgoc.office.domain.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authorization: Bearer 헤더의 Access Token을 검증해 SecurityContext에 {@link AuthUser}를 설정한다.
 * 토큰이 없거나 잘못되면 인증 없이 통과시키고, 인증이 필요한 API는 EntryPoint가 AUTH401로 응답한다.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request.getHeader(HttpHeaders.AUTHORIZATION));
        if (token != null) {
            jwtProvider.parseAccessToken(token)
                    .flatMap(userRepository::findById)
                    .ifPresent(user -> {
                        AuthUser authUser = new AuthUser(user.getId(), user.getRole(), user.getStatus());
                        var authentication = new UsernamePasswordAuthenticationToken(authUser, null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    });
        }
        filterChain.doFilter(request, response);
    }

    public static String resolveToken(String header) {
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            return token.isEmpty() ? null : token;
        }
        return null;
    }
}
