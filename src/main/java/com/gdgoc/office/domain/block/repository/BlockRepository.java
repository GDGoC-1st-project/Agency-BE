package com.gdgoc.office.domain.block.repository;

import com.gdgoc.office.domain.block.entity.Block;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BlockRepository extends JpaRepository<Block, Long> {

    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    Optional<Block> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    @Query("select b.blocked.id from Block b where b.blocker.id = :blockerId")
    List<Long> findBlockedIdsByBlockerId(Long blockerId);

    /** 두 사용자 중 한 명이라도 상대를 차단했는지 */
    @Query("""
            select count(b) > 0 from Block b
            where (b.blocker.id = :userA and b.blocked.id = :userB)
               or (b.blocker.id = :userB and b.blocked.id = :userA)
            """)
    boolean existsBetween(Long userA, Long userB);
}
