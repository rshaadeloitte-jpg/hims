package com.hims.nursing.application;

import com.hims.nursing.application.exceptions.InvalidNursingRequestException;
import com.hims.nursing.application.exceptions.NursingResourceNotFoundException;
import com.hims.nursing.domain.model.CarePlan;
import com.hims.nursing.domain.repository.CarePlanRepository;
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
public class NursingCarePlanService {

    private static final Logger log = LoggerFactory.getLogger(NursingCarePlanService.class);
    public static final long AUDIT_ACTOR_ID = 1L;

    private final CarePlanRepository repo;

    @PersistenceContext
    private EntityManager em;

    public NursingCarePlanService(CarePlanRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public CarePlan create(Long admissionId, String nursingDiagnosis, String goal, Long createdBy) {
        if (admissionId == null) throw new InvalidNursingRequestException("BEXA003", "admissionId is required");
        if (nursingDiagnosis == null || nursingDiagnosis.isBlank()) throw new InvalidNursingRequestException("BEXA003", "nursingDiagnosis is required");
        CarePlan cp = new CarePlan();
        cp.setCarePlanId(nextId("t_care_plan_seq"));
        cp.setAdmissionId(admissionId);
        cp.setNursingDiagnosis(nursingDiagnosis);
        cp.setGoal(goal);
        cp.setInterventions("[]");
        cp.setOutcomes("[]");
        cp.setStatusCode(CarePlan.STATUS_ACTIVE);
        cp.setAddedBy(createdBy != null ? createdBy : AUDIT_ACTOR_ID);
        cp.setAddedDatetime(now());
        repo.save(cp);
        log.info("Care plan created for admission {}", admissionId);
        return cp;
    }

    @Transactional
    public CarePlan updateStatus(Long carePlanId, Short statusCode) {
        CarePlan cp = repo.findById(carePlanId)
                .orElseThrow(() -> new NursingResourceNotFoundException("NOT_FOUND", "CarePlan not found: " + carePlanId));
        if (statusCode == null || (statusCode != CarePlan.STATUS_ACTIVE && statusCode != CarePlan.STATUS_RESOLVED && statusCode != CarePlan.STATUS_CANCELLED))
            throw new InvalidNursingRequestException("BEXA003", "Invalid status code");
        cp.setStatusCode(statusCode);
        cp.setUpdatedBy(AUDIT_ACTOR_ID);
        cp.setUpdatedDatetime(now());
        return repo.save(cp);
    }

    @Transactional(readOnly = true)
    public List<CarePlan> getByAdmission(Long admissionId) {
        if (admissionId == null) throw new InvalidNursingRequestException("BEXA003", "admissionId is required");
        return repo.findByAdmissionIdOrderByAddedDatetimeDesc(admissionId);
    }

    @Transactional
    public CarePlan addIntervention(Long carePlanId, String intervention) {
        CarePlan cp = repo.findById(carePlanId)
                .orElseThrow(() -> new NursingResourceNotFoundException("NOT_FOUND", "CarePlan not found: " + carePlanId));
        String current = cp.getInterventions();
        if (current == null || current.isBlank()) current = "[]";
        // Simple JSON array append simulation
        String updated = current.replace("]", "\"" + intervention + "\"]");
        cp.setInterventions(updated);
        cp.setUpdatedBy(AUDIT_ACTOR_ID);
        cp.setUpdatedDatetime(now());
        return repo.save(cp);
    }

    @Transactional
    public CarePlan addOutcome(Long carePlanId, String outcome) {
        CarePlan cp = repo.findById(carePlanId)
                .orElseThrow(() -> new NursingResourceNotFoundException("NOT_FOUND", "CarePlan not found: " + carePlanId));
        String current = cp.getOutcomes();
        if (current == null || current.isBlank()) current = "[]";
        String updated = current.replace("]", "\"" + outcome + "\"]");
        cp.setOutcomes(updated);
        cp.setUpdatedBy(AUDIT_ACTOR_ID);
        cp.setUpdatedDatetime(now());
        return repo.save(cp);
    }

    private Long nextId(String seqName) {
        Number n = (Number) em.createNativeQuery("SELECT nextval('ihms_nursing." + seqName + "')").getSingleResult();
        return n.longValue();
    }

    private static Timestamp now() {
        return Timestamp.from(Instant.now());
    }
}
