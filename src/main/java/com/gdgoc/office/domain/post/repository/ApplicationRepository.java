package com.gdgoc.office.domain.post.repository;

import com.gdgoc.office.domain.post.entity.Application;
import com.gdgoc.office.domain.request.entity.RequestStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    boolean existsByPostIdAndApplicantId(Long postId, Long applicantId);

    @Query("""
            select a from Application a join fetch a.post p join fetch p.author join fetch a.applicant
            where a.id = :id
            """)
    Optional<Application> findWithPostById(Long id);

    /** 받은 지원: 내 모집글(삭제되지 않은 글)에 들어온 지원, 내가 차단한 지원자 제외 */
    @Query("""
            select a from Application a join fetch a.post p join fetch a.applicant ap
            where p.author.id = :me
              and p.deleted = false
              and (:status is null or a.status = :status)
              and not exists (select 1 from Block b where b.blocker.id = :me and b.blocked = ap)
            order by a.createdAt desc, a.id desc
            """)
    List<Application> findReceived(Long me, RequestStatus status, Pageable pageable);

    /** 보낸 지원: 내가 지원한 건, 내가 차단한 작성자 제외, 삭제된 글의 대기 중 지원 제외 */
    @Query("""
            select a from Application a join fetch a.post p join fetch p.author au
            where a.applicant.id = :me
              and (:status is null or a.status = :status)
              and (p.deleted = false or a.status <> com.gdgoc.office.domain.request.entity.RequestStatus.PENDING)
              and not exists (select 1 from Block b where b.blocker.id = :me and b.blocked = au)
            order by a.createdAt desc, a.id desc
            """)
    List<Application> findSent(Long me, RequestStatus status, Pageable pageable);
}
