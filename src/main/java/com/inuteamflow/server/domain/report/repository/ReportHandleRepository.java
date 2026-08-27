package com.inuteamflow.server.domain.report.repository;

import com.inuteamflow.server.domain.report.entity.Report;
import com.inuteamflow.server.domain.report.entity.ReportHandle;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportHandleRepository extends JpaRepository<ReportHandle, Long> {

    Optional<ReportHandle> findByReport(Report report);
}
