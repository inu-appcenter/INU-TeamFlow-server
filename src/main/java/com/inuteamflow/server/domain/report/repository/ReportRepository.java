package com.inuteamflow.server.domain.report.repository;

import com.inuteamflow.server.domain.report.entity.Report;
import com.inuteamflow.server.domain.report.enums.ReportStatus;
import com.inuteamflow.server.domain.report.enums.ReportTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportRepository extends JpaRepository<Report, Long> {

    long countByStatus(ReportStatus status);

    @Query(
            value = "SELECT r FROM Report r WHERE :keyword IS NULL "
                    + "OR (r.targetType = :userType AND r.targetUserName LIKE %:keyword%) "
                    + "OR (r.targetType <> :userType AND r.targetPostTitle LIKE %:keyword%) "
                    + "OR r.reporterName LIKE %:keyword%",
            countQuery = "SELECT COUNT(r) FROM Report r WHERE :keyword IS NULL "
                    + "OR (r.targetType = :userType AND r.targetUserName LIKE %:keyword%) "
                    + "OR (r.targetType <> :userType AND r.targetPostTitle LIKE %:keyword%) "
                    + "OR r.reporterName LIKE %:keyword%")
    Page<Report> search(
            @Param("keyword") String keyword, @Param("userType") ReportTargetType userType, Pageable pageable);
}
