package com.gdgoc.office.global.config;

import com.gdgoc.office.global.exception.ErrorCode;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * API에서 발생할 수 있는 도메인 실패 응답을 Swagger 문서에 예시로 노출한다.
 * 전역 공통 에러(COMMON400, AUTH401, AUTH403, ADMIN403, COMMON500)는 경로/보안 설정에 따라 자동으로 추가된다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiErrorCodes {

    ErrorCode[] value();
}
