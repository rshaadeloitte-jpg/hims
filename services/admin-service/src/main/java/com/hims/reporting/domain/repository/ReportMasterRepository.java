package com.hims.reporting.domain.repository;

import com.hims.reporting.domain.model.ReportMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReportMasterRepository extends JpaRepository<ReportMaster, Long> {

    boolean existsByReportCode(String reportCode);

    Optional<ReportMaster> findByReportCode(String reportCode);

    /** List: optional active-only filter, newest first. */
    @Query("""
            SELECT r FROM ReportMaster r
            WHERE (:active IS NULL OR r.isActive = :active)
            ORDER BY r.reportId DESC
            """)
    List<ReportMaster> search(@Param("active") Boolean active);
}
