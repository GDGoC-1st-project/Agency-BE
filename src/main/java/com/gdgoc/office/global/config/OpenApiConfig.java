package com.gdgoc.office.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        ObjectSchema errorResponse = new ObjectSchema();
        errorResponse.description("실패 응답");
        errorResponse.addProperty("isSuccess", new BooleanSchema().description("요청 성공 여부").example(false));
        errorResponse.addProperty("code", new StringSchema().description("에러 코드").example("COMMON400"));
        errorResponse.addProperty("message", new StringSchema().description("에러 메시지").example("요청 값이 올바르지 않습니다."));

        return new OpenAPI()
                .info(new Info()
                        .title("GDGoC 인력사무소 API")
                        .description("GDGoC 동아리원 키워드 기반 매칭 서비스 백엔드 API 문서입니다. "
                                + "로그인 후 발급받은 accessToken을 우측 상단 Authorize 버튼에 입력하면 인증이 필요한 API를 호출할 수 있습니다.")
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("로그인/회원가입으로 발급받은 Access Token을 입력하세요. (Bearer 접두사 제외)"))
                        .addSchemas("ErrorResponse", errorResponse));
    }
}
