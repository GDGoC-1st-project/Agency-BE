package com.gdgoc.office.domain.auth.repository;

import com.gdgoc.office.domain.auth.entity.InviteCode;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface InviteCodeRepository extends JpaRepository<InviteCode, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InviteCode i where i.code = :code")
    Optional<InviteCode> findByCodeForUpdate(String code);

    boolean existsByCode(String code);
}
