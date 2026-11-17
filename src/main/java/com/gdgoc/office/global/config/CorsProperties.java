package com.gdgoc.office.global.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * CORS 허용 origin (환경변수 CORS_ALLOWED_ORIGINS, 쉼표 구분).
 * REST(Security 필터 체인)와 WebSocket(/ws/**) 핸드셰이크에 같은 값을 쓴다.
 * 정확한 주소(https://agency.vercel.app)와 와일드카드 패턴(https://*.vercel.app) 모두 가능하다.
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : allowedOrigins.stream()
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
    }

    public String[] allowedOriginsArray() {
        return allowedOrigins.toArray(String[]::new);
    }
}
