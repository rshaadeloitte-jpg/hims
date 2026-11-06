package com.hims.reporting.domain.repository;

import com.hims.reporting.domain.model.ReportExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReportExecutionRepository extends JpaRepository<ReportExecution, Long> {

    /** List: optional report-code and status filters, newest first. */
    @Query("""
            SELECT e FROM ReportExecution e
            WHERE (:reportId IS NULL OR e.reportId = :reportId)
              AND (:status IS NULL OR e.status = :status)
            ORDER BY e.executionId DESC
            """)
    List<ReportExecution> search(@Param("reportId") Long reportId, @Param("status") Short status);
}
