package com.inuteamflow.server.domain.report.repository;

import com.inuteamflow.server.domain.report.entity.Report;
import com.inuteamflow.server.domain.report.entity.ReportHandle;
import com.inuteamflow.server.domain.report.enums.UserActionType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportHandleRepository extends JpaRepository<ReportHandle, Long> {

    Optional<ReportHandle> findByReport(Report report);

    Optional<ReportHandle> findFirstByReport_TargetUserIdAndUserActionInAndReleasedAtIsNullOrderByCreatedAtDesc(
            Long targetUserId, List<UserActionType> userActions);
}
