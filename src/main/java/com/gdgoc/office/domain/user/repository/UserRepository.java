package com.gdgoc.office.domain.user.repository;

import com.gdgoc.office.domain.user.entity.User;
import com.gdgoc.office.domain.user.entity.UserStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);

    Optional<User> findByIdAndStatus(Long id, UserStatus status);

    /** 프로필 작성과 키워드 등록을 마친 승인 대기 사용자 */
    @Query(value = """
            select u from User u
            where u.status = com.gdgoc.office.domain.user.entity.UserStatus.PENDING
              and u.keywordRegistered = true
              and exists (select 1 from Profile p where p.user = u)
            order by u.id asc
            """,
            countQuery = """
            select count(u) from User u
            where u.status = com.gdgoc.office.domain.user.entity.UserStatus.PENDING
              and u.keywordRegistered = true
              and exists (select 1 from Profile p where p.user = u)
            """)
    Page<User> findPendingSignups(Pageable pageable);

    @Query("""
            select count(u) from User u
            where u.status = com.gdgoc.office.domain.user.entity.UserStatus.PENDING
              and u.keywordRegistered = true
              and exists (select 1 from Profile p where p.user = u)
            """)
    long countPendingSignups();
}
