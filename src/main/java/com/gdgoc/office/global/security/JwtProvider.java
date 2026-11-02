package com.gdgoc.office.global.security;

import com.gdgoc.office.domain.user.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtProvider {

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final Duration accessExpiration;
    private final Duration refreshExpiration;

    public JwtProvider(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.accessExpiration = Duration.ofMinutes(properties.accessExpirationMinutes());
        this.refreshExpiration = Duration.ofDays(properties.refreshExpirationDays());
    }

    public String createAccessToken(Long userId, Role role) {
        Date now = new Date();
        return Jwts.builder()
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_ROLE, role.name())
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessExpiration.toMillis()))
                .signWith(key)
                .compact();
    }

    public String createRefreshToken(Long userId) {
        Date now = new Date();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshExpiration.toMillis()))
                .signWith(key)
                .compact();
    }

    public LocalDateTime refreshExpiresAt() {
        return LocalDateTime.now(ZoneId.of("Asia/Seoul")).plus(refreshExpiration);
    }

    /** 유효한 Access Token이면 userId를 반환한다. */
    public Optional<Long> parseAccessToken(String token) {
        return parse(token, TYPE_ACCESS);
    }

    /** 서명/만료가 유효한 Refresh Token이면 userId를 반환한다. */
    public Optional<Long> parseRefreshToken(String token) {
        return parse(token, TYPE_REFRESH);
    }

    private Optional<Long> parse(String token, String expectedType) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            if (!expectedType.equals(claims.get(CLAIM_TYPE, String.class))) {
                return Optional.empty();
            }
            Object userId = claims.get(CLAIM_USER_ID);
            if (!(userId instanceof Number number)) {
                return Optional.empty();
            }
            return Optional.of(number.longValue());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
