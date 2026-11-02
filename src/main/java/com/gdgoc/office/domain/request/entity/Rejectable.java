package com.gdgoc.office.domain.request.entity;

import com.gdgoc.office.global.exception.BusinessException;
import com.gdgoc.office.global.exception.ErrorCode;

/** 수락/거절이 가능한 요청(Proposal, Application) 공통 동작 */
public interface Rejectable {

    RequestStatus getStatus();

    void accept();

    void reject(RejectReasonType type, String detail);

    default void ensurePending() {
        if (getStatus() != RequestStatus.PENDING) {
            throw new BusinessException(ErrorCode.REQUEST_ALREADY_PROCESSED);
        }
    }
}
