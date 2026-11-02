package com.gdgoc.office.domain.connection.repository;

import com.gdgoc.office.domain.connection.entity.Connection;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ConnectionRepository extends JpaRepository<Connection, Long> {

    Optional<Connection> findByUserAIdAndUserBId(Long userAId, Long userBId);

    default Optional<Connection> findBetween(Long userX, Long userY) {
        return findByUserAIdAndUserBId(Math.min(userX, userY), Math.max(userX, userY));
    }

    default boolean existsBetween(Long userX, Long userY) {
        return findBetween(userX, userY).isPresent();
    }

    @Query("""
            select c from Connection c join fetch c.userA join fetch c.userB
            where c.userA.id = :me or c.userB.id = :me
            order by c.createdAt desc, c.id desc
            """)
    Slice<Connection> findMyConnections(Long me, Pageable pageable);
}
