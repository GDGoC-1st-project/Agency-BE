package com.gdgoc.office.domain.request.repository;

import com.gdgoc.office.domain.request.entity.Proposal;
import com.gdgoc.office.domain.request.entity.RequestStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProposalRepository extends JpaRepository<Proposal, Long> {

    /** 두 사용자 사이(방향 무관)에 대기 중인 제안이 있는지 */
    @Query("""
            select count(p) > 0 from Proposal p
            where p.status = com.gdgoc.office.domain.request.entity.RequestStatus.PENDING
              and ((p.sender.id = :userA and p.receiver.id = :userB)
                or (p.sender.id = :userB and p.receiver.id = :userA))
            """)
    boolean existsPendingBetween(Long userA, Long userB);

    @Query("select p from Proposal p join fetch p.sender join fetch p.receiver where p.id = :id")
    Optional<Proposal> findWithUsersById(Long id);

    @Query("""
            select p from Proposal p join fetch p.sender s
            where p.receiver.id = :me
              and (:status is null or p.status = :status)
              and not exists (select 1 from Block b where b.blocker.id = :me and b.blocked = s)
            order by p.createdAt desc, p.id desc
            """)
    List<Proposal> findReceived(Long me, RequestStatus status, Pageable pageable);

    @Query("""
            select p from Proposal p join fetch p.receiver r
            where p.sender.id = :me
              and (:status is null or p.status = :status)
              and not exists (select 1 from Block b where b.blocker.id = :me and b.blocked = r)
            order by p.createdAt desc, p.id desc
            """)
    List<Proposal> findSent(Long me, RequestStatus status, Pageable pageable);
}
