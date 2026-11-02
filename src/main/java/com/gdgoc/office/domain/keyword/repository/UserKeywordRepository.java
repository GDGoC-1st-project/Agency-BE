package com.gdgoc.office.domain.keyword.repository;

import com.gdgoc.office.domain.keyword.entity.UserKeyword;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface UserKeywordRepository extends JpaRepository<UserKeyword, Long> {

    @Query("""
            select uk from UserKeyword uk join fetch uk.keyword
            where uk.user.id = :userId
            order by uk.id asc
            """)
    List<UserKeyword> findAllByUserIdWithKeyword(Long userId);

    @Query("""
            select uk from UserKeyword uk join fetch uk.keyword
            where uk.user.id in :userIds
            order by uk.id asc
            """)
    List<UserKeyword> findAllByUserIdInWithKeyword(Collection<Long> userIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from UserKeyword uk where uk.user.id = :userId")
    void deleteAllByUserId(Long userId);
}
