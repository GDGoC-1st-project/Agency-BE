package com.gdgoc.office.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdgoc.office.global.common.CustomResponse;
import com.gdgoc.office.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/** Security 필터 단계에서 §3.1 포맷의 JSON 실패 응답을 작성한다. */
@Component
@RequiredArgsConstructor
public class SecurityResponseWriter {

    private final ObjectMapper objectMapper;

    public void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(),
                CustomResponse.onFailure(errorCode.getCode(), errorCode.getMessage()));
    }
}
