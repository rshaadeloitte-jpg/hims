package com.hims.nursing.application;

import com.hims.nursing.application.exceptions.InvalidNursingRequestException;
import com.hims.nursing.application.exceptions.NursingResourceNotFoundException;
import com.hims.nursing.domain.model.CarePlan;
import com.hims.nursing.domain.model.IncidentReport;
import com.hims.nursing.domain.model.NurseAllocation;
import com.hims.nursing.domain.model.ShiftHandover;
import com.hims.nursing.domain.repository.CarePlanRepository;
import com.hims.nursing.domain.repository.IncidentReportRepository;
import com.hims.nursing.domain.repository.NurseAllocationRepository;
import com.hims.nursing.domain.repository.ShiftHandoverRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class NursingCareService {

    private static final Logger log = LoggerFactory.getLogger(NursingCareService.class);
    public static final long AUDIT_ACTOR_ID = 1L;

    private final CarePlanRepository carePlanRepo;
    private final ShiftHandoverRepository handoverRepo;
    private final NurseAllocationRepository allocationRepo;
    private final IncidentReportRepository incidentRepo;

    @PersistenceContext
    private EntityManager em;

    public NursingCareService(CarePlanRepository carePlanRepo,
                              ShiftHandoverRepository handoverRepo,
                              NurseAllocationRepository allocationRepo,
                              IncidentReportRepository incidentRepo) {
        this.carePlanRepo = carePlanRepo;
        this.handoverRepo = handoverRepo;
        this.allocationRepo = allocationRepo;
        this.incidentRepo = incidentRepo;
    }

    // ------------------------------------------------------- care plans

    @Transactional
    public CarePlan createCarePlan(Long admissionId, String diagnosis, String goal,
                                   String interventions, String outcomes) {
        if (admissionId == null) throw new InvalidNursingRequestException("BEXA003", "admissionId is required");
        if (diagnosis == null || diagnosis.isBlank()) throw new InvalidNursingRequestException("BEXA003", "nursingDiagnosis is required");
        CarePlan cp = new CarePlan();
        cp.setCarePlanId(nextId("t_care_plan_seq"));
        cp.setAdmissionId(admissionId);
        cp.setNursingDiagnosis(diagnosis.trim());
        cp.setGoal(goal);
        cp.setInterventions(interventions != null ? interventions : "[]");
        cp.setOutcomes(outcomes != null ? outcomes : "[]");
        cp.setStatusCode(CarePlan.STATUS_ACTIVE);
        cp.setAddedBy(AUDIT_ACTOR_ID);
        cp.setAddedDatetime(now());
        carePlanRepo.save(cp);
        log.info("Care plan {} created for admission {}", cp.getCarePlanId(), admissionId);
        return cp;
    }

    @Transactional(readOnly = true)
    public List<CarePlan> listCarePlans(Long admissionId) {
        return carePlanRepo.findByAdmissionIdOrderByAddedDatetimeDesc(admissionId);
    }

    @Transactional
    public CarePlan resolveCarePlan(long id) {
        CarePlan cp = carePlanRepo.findById(id)
                .orElseThrow(() -> new NursingResourceNotFoundException("NOT_FOUND", "Care plan not found: " + id));
        if (cp.getStatusCode() != CarePlan.STATUS_ACTIVE)
            throw new InvalidNursingRequestException("CARE_PLAN_NOT_ACTIVE", "Care plan is not active");
        cp.setStatusCode(CarePlan.STATUS_RESOLVED);
        cp.setUpdatedBy(AUDIT_ACTOR_ID);
        cp.setUpdatedDatetime(now());
        return carePlanRepo.save(cp);
    }

    // ------------------------------------------------------- shift handover

    @Transactional
    public ShiftHandover createHandover(Long admissionId, Long wardId, String shift,
                                        String situation, String background,
                                        String assessment, String recommendation,
                                        String pendingTasks, Long handedOverBy) {
        if (admissionId == null) throw new InvalidNursingRequestException("BEXA003", "admissionId is required");
        if (shift == null || shift.isBlank()) throw new InvalidNursingRequestException("BEXA003", "shift is required (MORNING/EVENING/NIGHT)");
        if (handedOverBy == null) throw new InvalidNursingRequestException("BEXA003", "handedOverBy is required");
        ShiftHandover h = new ShiftHandover();
        h.setHandoverId(nextId("t_shift_handover_seq"));
        h.setAdmissionId(admissionId);
        h.setWardId(wardId);
        h.setShift(shift.trim().toUpperCase());
        h.setSituation(situation);
        h.setBackground(background);
        h.setAssessment(assessment);
        h.setRecommendation(recommendation);
        h.setPendingTasks(pendingTasks);
        h.setHandedOverBy(handedOverBy);
        h.setAddedBy(AUDIT_ACTOR_ID);
        h.setAddedDatetime(now());
        handoverRepo.save(h);
        log.info("Shift handover {} created for admission {} shift {}", h.getHandoverId(), admissionId, shift);
        return h;
    }

    @Transactional
    public ShiftHandover signHandover(long id, Long receivedBy) {
        ShiftHandover h = handoverRepo.findById(id)
                .orElseThrow(() -> new NursingResourceNotFoundException("NOT_FOUND", "Handover not found: " + id));
        if (h.getSignedAt() != null)
            throw new InvalidNursingRequestException("ALREADY_SIGNED", "Handover already signed");
        h.setReceivedBy(receivedBy != null ? receivedBy : AUDIT_ACTOR_ID);
        h.setSignedAt(now());
        h.setUpdatedBy(AUDIT_ACTOR_ID);
        h.setUpdatedDatetime(now());
        return handoverRepo.save(h);
    }

    @Transactional(readOnly = true)
    public List<ShiftHandover> listHandoversByAdmission(Long admissionId) {
        return handoverRepo.findByAdmissionIdOrderByAddedDatetimeDesc(admissionId);
    }

    // ------------------------------------------------------- nurse allocation

    @Transactional
    public NurseAllocation allocate(Long wardId, Long nurseId, Long admissionId, String shift) {
        if (wardId == null) throw new InvalidNursingRequestException("BEXA003", "wardId is required");
        if (nurseId == null) throw new InvalidNursingRequestException("BEXA003", "nurseId is required");
        if (admissionId == null) throw new InvalidNursingRequestException("BEXA003", "admissionId is required");
        // Guard: one active allocation per nurse-admission pair
        List<NurseAllocation> existing = allocationRepo.findByAdmissionIdAndReleasedAtIsNull(admissionId);
        for (NurseAllocation a : existing) {
            if (a.getNurseId().equals(nurseId))
                throw new InvalidNursingRequestException("ALREADY_ALLOCATED", "Nurse already allocated to this admission");
        }
        NurseAllocation a = new NurseAllocation();
        a.setAllocationId(nextId("t_nurse_allocation_seq"));
        a.setWardId(wardId);
        a.setNurseId(nurseId);
        a.setAdmissionId(admissionId);
        a.setShift(shift);
        a.setAllocatedAt(now());
        a.setAddedBy(AUDIT_ACTOR_ID);
        a.setAddedDatetime(now());
        allocationRepo.save(a);
        log.info("Nurse {} allocated to admission {} in ward {}", nurseId, admissionId, wardId);
        return a;
    }

    @Transactional
    public NurseAllocation release(long id) {
        NurseAllocation a = allocationRepo.findById(id)
                .orElseThrow(() -> new NursingResourceNotFoundException("NOT_FOUND", "Allocation not found: " + id));
        if (a.getReleasedAt() != null)
            throw new InvalidNursingRequestException("ALREADY_RELEASED", "Allocation already released");
        a.setReleasedAt(now());
        a.setUpdatedBy(AUDIT_ACTOR_ID);
        a.setUpdatedDatetime(now());
        return allocationRepo.save(a);
    }

    @Transactional(readOnly = true)
    public List<NurseAllocation> activeAllocationsForWard(Long wardId) {
        return allocationRepo.findByWardIdAndReleasedAtIsNull(wardId);
    }

    // ------------------------------------------------------- incident reporting

    @Transactional
    public IncidentReport reportIncident(Long patientId, Long admissionId, String incidentType,
                                         String severity, String description, String location,
                                         Timestamp occurredAt, Long reportedBy) {
        if (patientId == null) throw new InvalidNursingRequestException("BEXA003", "patientId is required");
        if (incidentType == null || incidentType.isBlank()) throw new InvalidNursingRequestException("BEXA003", "incidentType is required");
        if (description == null || description.isBlank()) throw new InvalidNursingRequestException("BEXA003", "description is required");
        IncidentReport inc = new IncidentReport();
        inc.setIncidentId(nextId("t_incident_report_seq"));
        inc.setPatientId(patientId);
        inc.setAdmissionId(admissionId);
        inc.setIncidentType(incidentType.trim());
        inc.setSeverity(severity != null ? severity.trim().toUpperCase() : "LOW");
        inc.setDescription(description.trim());
        inc.setLocation(location);
        inc.setOccurredAt(occurredAt != null ? occurredAt : now());
        inc.setReportedBy(reportedBy != null ? reportedBy : AUDIT_ACTOR_ID);
        inc.setReviewStatus(IncidentReport.REVIEW_PENDING);
        inc.setAddedBy(AUDIT_ACTOR_ID);
        inc.setAddedDatetime(now());
        incidentRepo.save(inc);
        log.info("Incident {} reported for patient {} type={}", inc.getIncidentId(), patientId, incidentType);
        return inc;
    }

    @Transactional
    public IncidentReport reviewIncident(long id, Long reviewerId, String correctiveAction) {
        IncidentReport inc = incidentRepo.findById(id)
                .orElseThrow(() -> new NursingResourceNotFoundException("NOT_FOUND", "Incident not found: " + id));
        if (inc.getReviewStatus() != IncidentReport.REVIEW_PENDING)
            throw new InvalidNursingRequestException("INCIDENT_ALREADY_REVIEWED", "Incident already reviewed/closed");
        inc.setReviewStatus(IncidentReport.REVIEW_REVIEWED);
        inc.setReviewerId(reviewerId != null ? reviewerId : AUDIT_ACTOR_ID);
        inc.setReviewedAt(now());
        inc.setCorrectiveAction(correctiveAction);
        inc.setUpdatedBy(AUDIT_ACTOR_ID);
        inc.setUpdatedDatetime(now());
        return incidentRepo.save(inc);
    }

    @Transactional(readOnly = true)
    public List<IncidentReport> incidentsByPatient(Long patientId) {
        return incidentRepo.findByPatientIdOrderByOccurredAtDesc(patientId);
    }

    // ------------------------------------------------------- helpers

    private Long nextId(String seqName) {
        Number n = (Number) em.createNativeQuery(
                "SELECT nextval('ihms_nursing." + seqName + "')").getSingleResult();
        return n.longValue();
    }

    private static Timestamp now() {
        return Timestamp.from(Instant.now());
    }
}
