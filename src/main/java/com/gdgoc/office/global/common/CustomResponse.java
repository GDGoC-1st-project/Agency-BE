package com.gdgoc.office.global.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"isSuccess", "code", "message", "result"})
@Schema(description = "공통 응답 포맷")
public class CustomResponse<T> {

    @JsonProperty("isSuccess")
    @Schema(description = "요청 성공 여부", example = "true")
    private final boolean isSuccess;

    @Schema(description = "응답 코드", example = "200 OK")
    private final String code;

    @Schema(description = "응답 메시지", example = "요청 성공")
    private final String message;

    @Schema(description = "응답 데이터 (없으면 생략)")
    private final T result;

    private CustomResponse(boolean isSuccess, String code, String message, T result) {
        this.isSuccess = isSuccess;
        this.code = code;
        this.message = message;
        this.result = result;
    }

    public static <T> CustomResponse<T> onSuccess(HttpStatus status, String message, T result) {
        return new CustomResponse<>(true, toCode(status), message, result);
    }

    public static <T> CustomResponse<T> onSuccess(HttpStatus status, String message) {
        return new CustomResponse<>(true, toCode(status), message, null);
    }

    public static <T> CustomResponse<T> onFailure(String code, String message) {
        return new CustomResponse<>(false, code, message, null);
    }

    public static <T> CustomResponse<T> ok(String message, T result) {
        return onSuccess(HttpStatus.OK, message, result);
    }

    public static <T> CustomResponse<T> ok(String message) {
        return onSuccess(HttpStatus.OK, message);
    }

    public static <T> CustomResponse<T> created(String message, T result) {
        return onSuccess(HttpStatus.CREATED, message, result);
    }

    public static <T> CustomResponse<T> created(String message) {
        return onSuccess(HttpStatus.CREATED, message);
    }

    private static String toCode(HttpStatus status) {
        return status.value() + " " + status.getReasonPhrase();
    }
}
