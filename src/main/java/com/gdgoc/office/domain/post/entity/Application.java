package com.gdgoc.office.domain.post.entity;

import com.gdgoc.office.domain.request.entity.RejectReasonType;
import com.gdgoc.office.domain.request.entity.Rejectable;
import com.gdgoc.office.domain.request.entity.RequestStatus;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 모집글 지원 */
@Getter
@Entity
@Table(name = "applications", uniqueConstraints = @UniqueConstraint(
        name = "uk_applications_post_applicant", columnNames = {"post_id", "applicant_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Application extends BaseEntity implements Rejectable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "applicant_id", nullable = false)
    private User applicant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "reject_type", length = 20)
    private RejectReasonType rejectType;

    @Column(name = "reject_detail", length = 200)
    private String rejectDetail;

    public Application(Post post, User applicant) {
        this.post = post;
        this.applicant = applicant;
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
