package com.gdgoc.office.domain.user.repository;

import com.gdgoc.office.domain.user.entity.Profile;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProfileRepository extends JpaRepository<Profile, Long> {

    Optional<Profile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    @Query("select p from Profile p where p.user.id in :userIds")
    List<Profile> findAllByUserIdIn(Collection<Long> userIds);
}
