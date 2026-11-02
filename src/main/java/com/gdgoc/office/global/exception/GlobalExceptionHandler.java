package com.gdgoc.office.global.exception;

import com.gdgoc.office.global.common.CustomResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<CustomResponse<Void>> handleBusiness(BusinessException e) {
        return toResponse(e.getErrorCode());
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            ConstraintViolationException.class,
            HandlerMethodValidationException.class,
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MissingPathVariableException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMediaTypeNotSupportedException.class
    })
    public ResponseEntity<CustomResponse<Void>> handleInvalidInput(Exception e) {
        log.debug("잘못된 요청: {}", e.getMessage());
        return toResponse(ErrorCode.INVALID_INPUT);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<CustomResponse<Void>> handleNoResource(NoResourceFoundException e) {
        return toResponse(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<CustomResponse<Void>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        return toResponse(ErrorCode.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CustomResponse<Void>> handleException(Exception e) {
        log.error("처리되지 않은 예외", e);
        return toResponse(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    public static ResponseEntity<CustomResponse<Void>> toResponse(ErrorCode errorCode) {
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(CustomResponse.onFailure(errorCode.getCode(), errorCode.getMessage()));
    }
}
