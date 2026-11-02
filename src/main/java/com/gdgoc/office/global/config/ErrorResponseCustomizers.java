package com.gdgoc.office.global.config;

import com.gdgoc.office.global.exception.ErrorCode;
import com.gdgoc.office.global.security.ApprovalPolicy;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ErrorResponseCustomizers {

    /** 메서드에 선언된 {@link ApiErrorCodes}를 실패 응답 예시로 추가한다. */
    @Bean
    public OperationCustomizer apiErrorCodesCustomizer() {
        return (operation, handlerMethod) -> {
            ApiErrorCodes annotation = handlerMethod.getMethodAnnotation(ApiErrorCodes.class);
            if (annotation != null) {
                for (ErrorCode errorCode : annotation.value()) {
                    ErrorExampleSupport.addExample(operation, errorCode);
                }
            }
            return operation;
        };
    }

    /** 경로와 보안 설정을 보고 전역 공통 에러 예시를 추가한다. */
    @Bean
    public OpenApiCustomizer globalErrorCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().forEach((path, pathItem) -> {
                for (Map.Entry<PathItem.HttpMethod, Operation> entry : pathItem.readOperationsMap().entrySet()) {
                    Operation operation = entry.getValue();
                    String method = entry.getKey().name();
                    boolean secured = operation.getSecurity() != null && !operation.getSecurity().isEmpty();

                    ErrorExampleSupport.addExample(operation, ErrorCode.INVALID_INPUT);
                    if (secured) {
                        ErrorExampleSupport.addExample(operation, ErrorCode.INVALID_TOKEN);
                        if (!ApprovalPolicy.isPendingAllowed(method, path)) {
                            ErrorExampleSupport.addExample(operation, ErrorCode.NOT_APPROVED);
                        }
                        if (ApprovalPolicy.isAdminPath(path)) {
                            ErrorExampleSupport.addExample(operation, ErrorCode.ADMIN_ONLY);
                        }
                    }
                    ErrorExampleSupport.addExample(operation, ErrorCode.INTERNAL_SERVER_ERROR);
                }
            });
        };
    }
}
