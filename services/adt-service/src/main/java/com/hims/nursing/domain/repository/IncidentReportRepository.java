package com.hims.nursing.domain.repository;

import com.hims.nursing.domain.model.IncidentReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.sql.Timestamp;
import java.util.List;

public interface IncidentReportRepository extends JpaRepository<IncidentReport, Long> {
    List<IncidentReport> findByPatientIdOrderByOccurredAtDesc(Long patientId);
    List<IncidentReport> findByReviewStatusOrderByOccurredAtDesc(Short reviewStatus);
    List<IncidentReport> findByOccurredAtBetween(Timestamp from, Timestamp to);
}
