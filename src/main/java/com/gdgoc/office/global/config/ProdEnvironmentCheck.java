package com.gdgoc.office.global.config;

import java.util.List;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

/**
 * prod 프로필에서 필수 환경변수가 비어 있으면, 다른 빈(DataSource 등)을 만들기 전에
 * 빠진 변수 이름을 모두 적어 기동을 멈춘다.
 * (값이 없는 ${...}는 그대로 문자열로 넘어가 "url attribute is not specified"처럼 원인을 알기 어려운 에러가 나기 때문)
 */
@Configuration(proxyBeanMethods = false)
@Profile("prod")
public class ProdEnvironmentCheck {

    static final List<String> REQUIRED = List.of(
            "SPRING_DATASOURCE_URL",
            "SPRING_DATASOURCE_USERNAME",
            "SPRING_DATASOURCE_PASSWORD",
            "JWT_SECRET");

    @Bean
    static BeanFactoryPostProcessor requiredProdEnvironment(Environment environment) {
        return beanFactory -> {
            List<String> missing = REQUIRED.stream()
                    .filter(name -> {
                        String value = environment.getProperty(name);
                        return value == null || value.isBlank();
                    })
                    .toList();
            if (!missing.isEmpty()) {
                throw new IllegalStateException(
                        "prod 프로필 필수 환경변수가 없습니다: " + String.join(", ", missing)
                                + " (Railway라면 백엔드 서비스의 Variables에 넣고 다시 배포하세요)");
            }
        };
    }
}
