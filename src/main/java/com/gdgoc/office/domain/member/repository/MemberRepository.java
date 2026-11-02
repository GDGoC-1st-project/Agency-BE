package com.gdgoc.office.domain.member.repository;

import com.gdgoc.office.domain.keyword.entity.Relation;
import com.gdgoc.office.domain.member.dto.MemberRow;
import com.gdgoc.office.domain.user.entity.Department;
import com.gdgoc.office.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface MemberRepository extends Repository<User, Long> {

    /**
     * 승인된 멤버 검색. 본인/내가 차단한 멤버 제외, id 내림차순.
     * keywordPattern 이 있으면 해당 키워드(부분 일치)를 가진 멤버만, relation 이 있으면 그 관계로 등록한 경우만.
     */
    @Query("""
            select new com.gdgoc.office.domain.member.dto.MemberRow(u.id, u.name, p.department)
            from User u left join Profile p on p.user = u
            where u.status = com.gdgoc.office.domain.user.entity.UserStatus.APPROVED
              and u.id <> :me
              and not exists (select 1 from Block b where b.blocker.id = :me and b.blocked = u)
              and (:department is null or p.department = :department)
              and (:keywordPattern is null or exists (
                    select 1 from UserKeyword uk join uk.keyword k
                    where uk.user = u
                      and lower(k.name) like :keywordPattern escape '!'
                      and (:relation is null or uk.relation = :relation)))
            order by u.id desc
            """)
    Slice<MemberRow> search(Long me, String keywordPattern, Relation relation, Department department,
                            Pageable pageable);
}
