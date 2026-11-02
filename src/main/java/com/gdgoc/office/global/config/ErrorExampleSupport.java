package com.gdgoc.office.global.config;

import com.gdgoc.office.global.exception.ErrorCode;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.LinkedHashMap;
import java.util.Map;

final class ErrorExampleSupport {

    private static final String JSON = "application/json";

    private ErrorExampleSupport() {
    }

    /** 응답 코드(HTTP 상태)별로 실패 예시를 병합해 추가한다. */
    static void addExample(Operation operation, ErrorCode errorCode) {
        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            responses = new ApiResponses();
            operation.setResponses(responses);
        }
        String status = String.valueOf(errorCode.getHttpStatus().value());
        ApiResponse response = responses.get(status);
        if (response == null) {
            response = new ApiResponse().description(errorCode.getHttpStatus().getReasonPhrase());
            responses.addApiResponse(status, response);
        }
        if (response.getContent() == null) {
            response.setContent(new Content());
        }
        MediaType mediaType = response.getContent().get(JSON);
        if (mediaType == null) {
            mediaType = new MediaType().schema(new Schema<>().$ref("#/components/schemas/ErrorResponse"));
            response.getContent().addMediaType(JSON, mediaType);
        }
        String exampleName = errorCode.getCode() + " " + errorCode.getMessage();
        if (mediaType.getExamples() != null && mediaType.getExamples().containsKey(exampleName)) {
            return;
        }
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("isSuccess", false);
        value.put("code", errorCode.getCode());
        value.put("message", errorCode.getMessage());
        mediaType.addExamples(exampleName, new Example().summary(errorCode.getMessage()).value(value));
        response.setDescription(appendDescription(response.getDescription(), errorCode));
    }

    private static String appendDescription(String current, ErrorCode errorCode) {
        String line = errorCode.getCode() + ": " + errorCode.getMessage();
        if (current == null || current.isBlank() || current.equals(errorCode.getHttpStatus().getReasonPhrase())) {
            return line;
        }
        return current.contains(line) ? current : current + " / " + line;
    }
}
