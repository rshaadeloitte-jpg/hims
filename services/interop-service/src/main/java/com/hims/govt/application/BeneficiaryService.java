package com.hims.govt.application;

import com.hims.govt.application.exceptions.GovtConflictException;
import com.hims.govt.application.exceptions.GovtNotFoundException;
import com.hims.govt.application.exceptions.InvalidGovtRequestException;
import com.hims.govt.domain.model.BeneficiaryEnrolment;
import com.hims.govt.domain.model.ReferenceTid;
import com.hims.govt.domain.repository.BeneficiaryEnrolmentRepository;
import com.hims.govt.domain.repository.ReferenceTidRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Chiranjeevi beneficiary enrolment + TID linkage (GAP-REGISTER-6 WP-T4).
 *
 * Guards pinned by tests:
 *   - enrol: patientId required; scheme must be exactly CHIRANJEEVI (BEXA003);
 *     enrolmentNo required (<= 40 chars); enrolledOn required;
 *     duplicate enrolmentNo -> 409 GOVT_DUPLICATE_ENROLMENT_NO
 *   - linkTid: unknown enrolmentNo -> 404; tid required (BEXA003);
 *     duplicate tid -> 409 GOVT_DUPLICATE_TID
 *
 * EVENT POLICY (register WP-T4): this service EMITS NOTHING — external
 * Chiranjeevi-Yojana scheme-side pushes are OUT OF SCOPE (comment-only); the
 * outbox stays empty through every flow (asserted by the integration test).
 */
@Service
public class BeneficiaryService {

    private static final Logger log = LoggerFactory.getLogger(BeneficiaryService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    private final BeneficiaryEnrolmentRepository enrolmentRepository;
    private final ReferenceTidRepository tidRepository;

    @PersistenceContext
    private EntityManager em;

    public BeneficiaryService(BeneficiaryEnrolmentRepository enrolmentRepository,
                              ReferenceTidRepository tidRepository) {
        this.enrolmentRepository = enrolmentRepository;
        this.tidRepository = tidRepository;
    }

    // ------------------------------------------------------------------ enrol

    /**
     * Enrol a patient into the Chiranjeevi scheme. Guards: patientId required;
     * scheme must be CHIRANJEEVI (the CHECK constraint admits nothing else);
     * enrolmentNo required (<= 40 chars); enrolledOn required. Duplicate
     * enrolmentNo -> 409 GOVT_DUPLICATE_ENROLMENT_NO. Emits NOTHING to the outbox.
     */
    @Transactional
    public BeneficiaryEnrolment enrol(Long patientId, String scheme, String enrolmentNo, LocalDate enrolledOn) {
        if (patientId == null) {
            throw new InvalidGovtRequestException("BEXA003", "patientId is required");
        }
        if (scheme == null || scheme.isBlank()) {
            throw new InvalidGovtRequestException("BEXA003", "scheme is required");
        }
        if (!BeneficiaryEnrolment.SCHEME_CHIRANJEEVI.equals(scheme.trim().toUpperCase())) {
            throw new InvalidGovtRequestException("BEXA003",
                    "scheme must be CHIRANJEEVI — got: " + scheme);
        }
        if (enrolmentNo == null || enrolmentNo.isBlank()) {
            throw new InvalidGovtRequestException("BEXA003", "enrolmentNo is required");
        }
        if (enrolmentNo.length() > 40) {
            throw new InvalidGovtRequestException("BEXA003",
                    "enrolmentNo must be at most 40 characters");
        }
        if (enrolledOn == null) {
            throw new InvalidGovtRequestException("BEXA003", "enrolledOn is required");
        }
        if (enrolmentRepository.existsByEnrolmentNo(enrolmentNo)) {
            throw new GovtConflictException("GOVT_DUPLICATE_ENROLMENT_NO",
                    "enrolmentNo already exists: " + enrolmentNo);
        }

        Timestamp nowTs = now();
        BeneficiaryEnrolment e = new BeneficiaryEnrolment();
        e.setEnrolmentId(nextId("t_beneficiary_enrolment_seq"));
        e.setPatientId(patientId);
        e.setScheme(BeneficiaryEnrolment.SCHEME_CHIRANJEEVI);
        e.setEnrolmentNo(enrolmentNo);
        e.setEnrolledOn(enrolledOn);
        e.setIsActive(true);
        e.setAddedBy(AUDIT_ACTOR_ID);
        e.setAddedDatetime(nowTs);
        try {
            enrolmentRepository.save(e);
        } catch (DataIntegrityViolationException ex) {
            // race with a concurrent enrol of the same enrolmentNo — same 409 contract
            throw new GovtConflictException("GOVT_DUPLICATE_ENROLMENT_NO",
                    "enrolmentNo already exists: " + enrolmentNo);
        }
        log.info("Enrolled patient {} into CHIRANJEEVI as {}", patientId, enrolmentNo);
        return e;
    }

    // ------------------------------------------------------------------ tid

    /**
     * Link a scheme-side TID to an enrolment {enrolmentNo}. Guards: unknown
     * enrolmentNo -> 404; tid required (BEXA003); duplicate tid (already linked,
     * globally unique) -> 409 GOVT_DUPLICATE_TID. Emits NOTHING to the outbox.
     */
    @Transactional
    public ReferenceTid linkTid(String enrolmentNo, String tid) {
        BeneficiaryEnrolment enrolment = enrolmentRepository.findByEnrolmentNo(enrolmentNo)
                .orElseThrow(() -> new GovtNotFoundException("NOT_FOUND",
                        "Enrolment not found: " + enrolmentNo));
        if (tid == null || tid.isBlank()) {
            throw new InvalidGovtRequestException("BEXA003", "tid is required");
        }
        if (tidRepository.existsByTid(tid)) {
            throw new GovtConflictException("GOVT_DUPLICATE_TID",
                    "tid already linked: " + tid);
        }

        Timestamp nowTs = now();
        ReferenceTid link = new ReferenceTid();
        link.setTidId(nextId("t_reference_tid_seq"));
        link.setEnrolmentId(enrolment.getEnrolmentId());
        link.setTid(tid);
        link.setAddedBy(AUDIT_ACTOR_ID);
        link.setAddedDatetime(nowTs);
        try {
            tidRepository.save(link);
        } catch (DataIntegrityViolationException ex) {
            // race with a concurrent link of the same tid — same 409 contract
            throw new GovtConflictException("GOVT_DUPLICATE_TID", "tid already linked: " + tid);
        }
        log.info("Linked TID {} to enrolment {}", tid, enrolmentNo);
        return link;
    }

    // ------------------------------------------------------------------ helpers

    private Long nextId(String sequenceName) {
        Number n = (Number) em.createNativeQuery("SELECT nextval('ihms_govt." + sequenceName + "')")
                .getSingleResult();
        return n.longValue();
    }

    private static Timestamp now() {
        return Timestamp.from(Instant.now());
    }
}
