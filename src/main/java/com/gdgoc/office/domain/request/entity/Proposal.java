package com.gdgoc.office.domain.request.entity;

import com.gdgoc.office.domain.user.entity.User;
import com.gdgoc.office.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 같이 해요 제안 */
@Getter
@Entity
@Table(name = "proposals")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Proposal extends BaseEntity implements Rejectable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_type", nullable = false, length = 20)
    private ReasonType reasonType;

    @Column(nullable = false, length = 200)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "reject_type", length = 20)
    private RejectReasonType rejectType;

    @Column(name = "reject_detail", length = 200)
    private String rejectDetail;

    public Proposal(User sender, User receiver, ReasonType reasonType, String message) {
        this.sender = sender;
        this.receiver = receiver;
        this.reasonType = reasonType;
        this.message = message;
        this.status = RequestStatus.PENDING;
    }

    @Override
    public void accept() {
        ensurePending();
        this.status = RequestStatus.ACCEPTED;
    }

    @Override
    public void reject(RejectReasonType type, String detail) {
        ensurePending();
        this.status = RequestStatus.REJECTED;
        this.rejectType = type;
        this.rejectDetail = detail;
    }
}
