package com.hims.nursing.application;

import com.hims.nursing.application.exceptions.InvalidNursingRequestException;
import com.hims.nursing.application.exceptions.NursingResourceNotFoundException;
import com.hims.nursing.domain.model.ShiftHandover;
import com.hims.nursing.domain.repository.ShiftHandoverRepository;
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
public class ShiftHandoverService {

    private static final Logger log = LoggerFactory.getLogger(ShiftHandoverService.class);
    public static final long AUDIT_ACTOR_ID = 1L;

    private final ShiftHandoverRepository repo;

    @PersistenceContext
    private EntityManager em;

    public ShiftHandoverService(ShiftHandoverRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public ShiftHandover create(Long admissionId, Long wardId, String shift, String situation, String background,
                                String assessment, String recommendation, String pendingTasks, Long handedOverBy) {
        if (admissionId == null) throw new InvalidNursingRequestException("BEXA003", "admissionId is required");
        if (shift == null || shift.isBlank()) throw new InvalidNursingRequestException("BEXA003", "shift is required");
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
        h.setHandedOverBy(handedOverBy != null ? handedOverBy : AUDIT_ACTOR_ID);
        h.setAddedBy(AUDIT_ACTOR_ID);
        h.setAddedDatetime(now());
        repo.save(h);
        log.info("Handover created for admission {} shift {}", admissionId, shift);
        return h;
    }

    @Transactional
    public ShiftHandover completeSignOff(Long handoverId, Long receivedBy) {
        ShiftHandover h = repo.findById(handoverId)
                .orElseThrow(() -> new NursingResourceNotFoundException("NOT_FOUND", "Handover not found: " + handoverId));
        h.setReceivedBy(receivedBy != null ? receivedBy : AUDIT_ACTOR_ID);
        h.setSignedAt(now());
        h.setUpdatedBy(AUDIT_ACTOR_ID);
        h.setUpdatedDatetime(now());
        return repo.save(h);
    }

    @Transactional(readOnly = true)
    public List<ShiftHandover> getByAdmission(Long admissionId) {
        if (admissionId == null) throw new InvalidNursingRequestException("BEXA003", "admissionId is required");
        return repo.findByAdmissionIdOrderByAddedDatetimeDesc(admissionId);
    }

    @Transactional(readOnly = true)
    public List<ShiftHandover> getByWardAndDate(Long wardId, String shift, Timestamp from, Timestamp to) {
        return repo.findByWardIdAndShiftAndAddedDatetimeBetween(
                wardId, shift != null ? shift.toUpperCase() : null, from, to);
    }

    private Long nextId(String seqName) {
        Number n = (Number) em.createNativeQuery("SELECT nextval('ihms_nursing." + seqName + "')").getSingleResult();
        return n.longValue();
    }

    private static Timestamp now() {
        return Timestamp.from(Instant.now());
    }
}
