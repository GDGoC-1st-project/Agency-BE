package com.gdgoc.office.domain.member.dto;

import com.gdgoc.office.domain.user.entity.Department;

/** 멤버 검색 쿼리 결과 행 */
public record MemberRow(Long id, String name, Department department) {
}
