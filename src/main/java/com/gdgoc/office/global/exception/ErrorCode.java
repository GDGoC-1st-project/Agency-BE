package com.gdgoc.office.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 전역 공통
    INVALID_INPUT("COMMON400", HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    RESOURCE_NOT_FOUND("COMMON404", HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED("COMMON405", HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
    INTERNAL_SERVER_ERROR("COMMON500", HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),
    INVALID_TOKEN("AUTH401", HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    NOT_APPROVED("AUTH403", HttpStatus.FORBIDDEN, "운영진 승인 후 이용할 수 있습니다."),
    ADMIN_ONLY("ADMIN403", HttpStatus.FORBIDDEN, "운영진만 접근할 수 있습니다."),

    // 회원가입 · 로그인
    INVITE_CODE_INVALID("INVITE400", HttpStatus.BAD_REQUEST, "유효하지 않거나 이미 사용된 초대 코드입니다."),
    LOGIN_ID_DUPLICATED("AUTH409", HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다."),
    LOGIN_FAILED("LOGIN401", HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),
    REFRESH_TOKEN_INVALID("REFRESH401", HttpStatus.UNAUTHORIZED, "유효하지 않거나 만료된 Refresh Token입니다."),

    // 프로필
    PROFILE_ALREADY_EXISTS("PROFILE409", HttpStatus.CONFLICT, "이미 프로필이 작성되었습니다."),
    PROFILE_NOT_FOUND("PROFILE404", HttpStatus.NOT_FOUND, "작성된 프로필이 없습니다."),

    // 키워드
    KEYWORD_LIMIT_EXCEEDED("KEYWORD400", HttpStatus.BAD_REQUEST, "관계별 키워드는 최대 20개까지 등록할 수 있습니다."),
    KEYWORD_NOT_FOUND("KEYWORD404", HttpStatus.NOT_FOUND, "존재하지 않는 키워드가 포함되어 있습니다."),

    // 멤버
    MEMBER_NOT_FOUND("MEMBER404", HttpStatus.NOT_FOUND, "존재하지 않는 멤버입니다."),

    // 같이 해요 제안
    PROPOSAL_SELF("PROPOSAL400", HttpStatus.BAD_REQUEST, "본인에게는 제안할 수 없습니다."),
    PROPOSAL_DUPLICATED("PROPOSAL409", HttpStatus.CONFLICT, "이미 연결되었거나 대기 중인 제안이 있습니다."),

    // 신고
    REPORT_TARGET_NOT_FOUND("REPORT404", HttpStatus.NOT_FOUND, "신고 대상을 찾을 수 없습니다."),
    REPORT_DUPLICATED("REPORT409", HttpStatus.CONFLICT, "이미 신고한 대상입니다."),
    REPORT_NOT_FOUND("REPORT404", HttpStatus.NOT_FOUND, "존재하지 않는 신고입니다."),
    REPORT_ALREADY_RESOLVED("REPORT409", HttpStatus.CONFLICT, "이미 처리된 신고입니다."),

    // 차단
    BLOCK_SELF("BLOCK400", HttpStatus.BAD_REQUEST, "본인은 차단할 수 없습니다."),
    BLOCK_DUPLICATED("BLOCK409", HttpStatus.CONFLICT, "이미 차단한 멤버입니다."),
    BLOCK_NOT_FOUND("BLOCK404", HttpStatus.NOT_FOUND, "차단한 내역이 없습니다."),

    // 모집글 · 지원
    POST_NOT_FOUND("POST404", HttpStatus.NOT_FOUND, "존재하지 않는 모집글입니다."),
    POST_UPDATE_FORBIDDEN("POST403", HttpStatus.FORBIDDEN, "작성자만 수정할 수 있습니다."),
    POST_DELETE_FORBIDDEN("POST403", HttpStatus.FORBIDDEN, "작성자만 삭제할 수 있습니다."),
    APPLICATION_DUPLICATED("APPLICATION409", HttpStatus.CONFLICT, "이미 지원한 모집글입니다."),
    APPLICATION_SELF("APPLICATION400", HttpStatus.BAD_REQUEST, "본인의 모집글에는 지원할 수 없습니다."),

    // 요청
    REQUEST_NOT_FOUND("REQUEST404", HttpStatus.NOT_FOUND, "존재하지 않는 요청입니다."),
    REQUEST_FORBIDDEN("REQUEST403", HttpStatus.FORBIDDEN, "요청을 처리할 권한이 없습니다."),
    REQUEST_ALREADY_PROCESSED("REQUEST409", HttpStatus.CONFLICT, "이미 처리된 요청입니다."),

    // 채팅
    CHAT_ROOM_NOT_FOUND("CHAT404", HttpStatus.NOT_FOUND, "존재하지 않는 채팅방입니다."),
    CHAT_FORBIDDEN("CHAT403", HttpStatus.FORBIDDEN, "채팅방에 참여한 멤버만 접근할 수 있습니다."),

    // 운영 관리
    USER_NOT_FOUND("USER404", HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."),
    USER_ALREADY_APPROVED("USER409", HttpStatus.CONFLICT, "이미 승인된 사용자입니다.");

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;
}
