package com.gdgoc.office.domain.report.repository;

import com.gdgoc.office.domain.report.entity.Report;
import com.gdgoc.office.domain.report.entity.ReportStatus;
import com.gdgoc.office.domain.report.entity.ReportTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByReporterIdAndTargetTypeAndTargetId(Long reporterId, ReportTargetType targetType, Long targetId);

    long countByStatus(ReportStatus status);

    @Query(value = """
            select r from Report r join fetch r.reporter
            where r.status = :status
            order by r.createdAt desc, r.id desc
            """,
            countQuery = "select count(r) from Report r where r.status = :status")
    Page<Report> findByStatus(ReportStatus status, Pageable pageable);
}
