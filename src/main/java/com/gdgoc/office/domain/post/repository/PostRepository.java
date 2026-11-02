package com.gdgoc.office.domain.post.repository;

import com.gdgoc.office.domain.post.entity.Post;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("select p from Post p join fetch p.author where p.id = :postId and p.deleted = false")
    Optional<Post> findActiveById(Long postId);

    /** 삭제되지 않은 글 중 내가 차단한 멤버의 글을 제외하고 최신순 */
    @Query("""
            select p from Post p join fetch p.author a
            where p.deleted = false
              and not exists (select 1 from Block b where b.blocker.id = :me and b.blocked = a)
            order by p.createdAt desc, p.id desc
            """)
    Slice<Post> findLatest(Long me, Pageable pageable);
}
