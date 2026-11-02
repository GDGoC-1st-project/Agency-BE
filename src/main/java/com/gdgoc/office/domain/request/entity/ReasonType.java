package com.gdgoc.office.domain.request.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 같이 해요 제안 사유 */
@Getter
@RequiredArgsConstructor
public enum ReasonType {
    PROJECT("프로젝트"),
    STUDY("스터디"),
    CONTEST("해커톤·공모전"),
    COFFEE_CHAT("커피챗"),
    HOBBY("취미·관심사"),
    ETC("기타");

    private final String label;
}
