package com.gdgoc.office.domain.keyword.repository;

import com.gdgoc.office.domain.keyword.entity.Keyword;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface KeywordRepository extends JpaRepository<Keyword, Long> {

    Optional<Keyword> findByName(String name);

    /** 공식 키워드 중 이름 부분 일치(대소문자 무시), 앞부분 일치 우선 정렬 */
    @Query("""
            select k from Keyword k
            where k.status = com.gdgoc.office.domain.keyword.entity.KeywordStatus.APPROVED
              and lower(k.name) like :contains escape '!'
            order by case when lower(k.name) like :prefix escape '!' then 0 else 1 end, k.name asc, k.id asc
            """)
    List<Keyword> search(String contains, String prefix, Pageable pageable);
}
