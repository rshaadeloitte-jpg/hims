package com.hims.rcm.application;

import com.hims.rcm.application.exceptions.InvalidRcmRequestException;
import com.hims.rcm.application.exceptions.RcmNotFoundException;
import com.hims.rcm.application.exceptions.TidConflictException;
import com.hims.rcm.domain.model.AdmissionTid;
import com.hims.rcm.domain.repository.AdmissionTidRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * Admission -> NHCX transaction id registry (register WP-R4). One TID per
 * admission (UNIQUE column): PUT on an admission that already carries a TID is
 * rejected 409 TID_EXISTS; GET of an unknown admission maps to a 404 envelope.
 */
@Service
public class TidService {

    private static final Logger log = LoggerFactory.getLogger(TidService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    private final AdmissionTidRepository repository;

    @PersistenceContext
    private EntityManager em;

    public TidService(AdmissionTidRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public AdmissionTid get(long admissionId) {
        return repository.findByAdmissionId(admissionId)
                .orElseThrow(() -> new RcmNotFoundException("NOT_FOUND",
                        "No NHCX transaction id registered for admission " + admissionId));
    }

    /**
     * PUT /api/v1/admissions/{admissionId}/tid {tid} — registers the TID.
     * Guards: tid required, max 60 chars (400 BEXA003); existing row for the
     * admission -> 409 TID_EXISTS (DB unique constraint backs this at schema level).
     */
    @Transactional
    public AdmissionTid assign(long admissionId, String tid) {
        if (tid == null || tid.isBlank()) {
            throw new InvalidRcmRequestException("BEXA003", "tid is required");
        }
        if (tid.length() > NhcxService.MAX_TID_LENGTH) {
            throw new InvalidRcmRequestException("BEXA003",
                    "tid must be at most " + NhcxService.MAX_TID_LENGTH + " characters");
        }
        if (repository.existsByAdmissionId(admissionId)) {
            throw new TidConflictException("TID_EXISTS",
                    "Admission " + admissionId + " already has an NHCX transaction id — reassignment rejected");
        }
        Timestamp nowTs = Timestamp.from(Instant.now());
        AdmissionTid row = new AdmissionTid();
        row.setTidId(nextId());
        row.setAdmissionId(admissionId);
        row.setTid(tid);
        row.setStatus(true);
        row.setSynchronizedd(false);
        row.setAddedBy(AUDIT_ACTOR_ID);
        row.setAddedDatetime(nowTs);
        repository.save(row);
        log.info("Registered NHCX transaction id for admission {}", admissionId);
        return row;
    }

    private Long nextId() {
        Number n = (Number) em.createNativeQuery("SELECT nextval('ihms_rcm.t_admission_tid_seq')")
                .getSingleResult();
        return n.longValue();
    }
}
