package com.gdgoc.office.global.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        long accessExpirationMinutes,
        long refreshExpirationDays
) {
}
