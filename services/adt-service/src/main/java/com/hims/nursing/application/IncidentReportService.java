package com.hims.nursing.application;

import com.hims.nursing.application.exceptions.InvalidNursingRequestException;
import com.hims.nursing.application.exceptions.NursingResourceNotFoundException;
import com.hims.nursing.domain.model.IncidentReport;
import com.hims.nursing.domain.repository.IncidentReportRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Service
public class IncidentReportService {

    private static final Logger log = LoggerFactory.getLogger(IncidentReportService.class);
    public static final long AUDIT_ACTOR_ID = 1L;

    private final IncidentReportRepository repo;

    @PersistenceContext
    private EntityManager em;

    public IncidentReportService(IncidentReportRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public IncidentReport report(Long patientId, String incidentType, String severity, String description,
                                 String location, Timestamp occurredAt, Long reportedBy) {
        if (patientId == null) throw new InvalidNursingRequestException("BEXA003", "patientId is required");
        if (incidentType == null || incidentType.isBlank()) throw new InvalidNursingRequestException("BEXA003", "incidentType is required");
        IncidentReport ir = new IncidentReport();
        ir.setIncidentId(nextId("t_incident_report_seq"));
        ir.setPatientId(patientId);
        ir.setIncidentType(incidentType.trim());
        ir.setSeverity(severity != null ? severity.trim() : "LOW");
        ir.setDescription(description);
        ir.setLocation(location);
        ir.setOccurredAt(occurredAt != null ? occurredAt : now());
        ir.setReportedBy(reportedBy != null ? reportedBy : AUDIT_ACTOR_ID);
        ir.setReviewStatus(IncidentReport.REVIEW_PENDING);
        ir.setAddedBy(AUDIT_ACTOR_ID);
        ir.setAddedDatetime(now());
        repo.save(ir);
        log.info("Incident reported for patient {} type {}", patientId, incidentType);
        return ir;
    }

    @Transactional
    public IncidentReport reviewStatus(Long incidentId, Short reviewStatus, Long reviewerId, String correctiveAction) {
        IncidentReport ir = repo.findById(incidentId)
                .orElseThrow(() -> new NursingResourceNotFoundException("NOT_FOUND", "IncidentReport not found: " + incidentId));
        if (reviewStatus == null) throw new InvalidNursingRequestException("BEXA003", "reviewStatus is required");
        ir.setReviewStatus(reviewStatus);
        ir.setReviewerId(reviewerId);
        ir.setReviewedAt(now());
        ir.setCorrectiveAction(correctiveAction);
        ir.setUpdatedBy(AUDIT_ACTOR_ID);
        ir.setUpdatedDatetime(now());
        return repo.save(ir);
    }

    @Transactional(readOnly = true)
    public List<IncidentReport> getByDateRange(Timestamp from, Timestamp to) {
        return repo.findByOccurredAtBetween(from, to);
    }

    private Long nextId(String seqName) {
        Number n = (Number) em.createNativeQuery("SELECT nextval('ihms_nursing." + seqName + "')").getSingleResult();
        return n.longValue();
    }

    private static Timestamp now() {
        return Timestamp.from(Instant.now());
    }
}
