package com.hims.govt.application;

import com.hims.govt.application.exceptions.GovtNotFoundException;
import com.hims.govt.application.exceptions.InvalidGovtRequestException;
import com.hims.govt.domain.model.AasmaanCase;
import com.hims.govt.domain.model.AasmaanCaseNote;
import com.hims.govt.domain.repository.AasmaanCaseNoteRepository;
import com.hims.govt.domain.repository.AasmaanCaseRepository;
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

/**
 * Aasmaan case management (GAP-REGISTER-6 WP-T4).
 *
 * Forward-only status machine (SMALLINT): 1=OPEN -&gt; 2=IN_PROGRESS -&gt; 3=CLOSED.
 * Adjacent steps only (1-&gt;2, 2-&gt;3); any repeat/backward/skip move is
 * 400 GOVT_ILLEGAL_TRANSITION (interpretation disclosed in WP-T4-govt.md:
 * the register pins "forward-only 1-&gt;2-&gt;3", so OPEN cannot jump straight to
 * CLOSED). Closing stamps closed_on. Notes on CLOSED cases are rejected
 * (400 GOVT_CASE_CLOSED).
 *
 * EVENT POLICY (register WP-T4): CaseUpdated is DEFERRED — this service emits
 * NOTHING to the outbox on ANY transition (asserted by the integration test).
 * Full Aasmaan module-78 endpoint parity + ANDROID/media/profile tables are
 * OUT OF SCOPE (comment-only).
 */
@Service
public class AasmaanService {

    private static final Logger log = LoggerFactory.getLogger(AasmaanService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    private final AasmaanCaseRepository caseRepository;
    private final AasmaanCaseNoteRepository noteRepository;

    @PersistenceContext
    private EntityManager em;

    public AasmaanService(AasmaanCaseRepository caseRepository,
                          AasmaanCaseNoteRepository noteRepository) {
        this.caseRepository = caseRepository;
        this.noteRepository = noteRepository;
    }

    // ------------------------------------------------------------------ create

    /**
     * Open a new case {patientId, caseType}. Guards: patientId required;
     * caseType required (<= 40 chars). New case is OPEN with opened_on = today.
     * Emits NOTHING to the outbox.
     */
    @Transactional
    public AasmaanCase create(Long patientId, String caseType) {
        if (patientId == null) {
            throw new InvalidGovtRequestException("BEXA003", "patientId is required");
        }
        if (caseType == null || caseType.isBlank()) {
            throw new InvalidGovtRequestException("BEXA003", "caseType is required");
        }
        if (caseType.length() > 40) {
            throw new InvalidGovtRequestException("BEXA003",
                    "caseType must be at most 40 characters");
        }

        Timestamp nowTs = now();
        AasmaanCase c = new AasmaanCase();
        c.setCaseId(nextId("t_aasmaan_case_seq"));
        c.setPatientId(patientId);
        c.setCaseType(caseType);
        c.setStatus(AasmaanCase.STATUS_OPEN);
        c.setOpenedOn(LocalDate.now());
        c.setAddedBy(AUDIT_ACTOR_ID);
        c.setAddedDatetime(nowTs);
        caseRepository.save(c);
        log.info("Opened Aasmaan case {} for patient {} ({})", c.getCaseId(), patientId, caseType);
        return c;
    }

    // ------------------------------------------------------------------ notes

    /**
     * Append a note {note, author} to a case. Guards: case must exist (404);
     * note required (BEXA003); case must NOT be CLOSED (400 GOVT_CASE_CLOSED —
     * the closed-case note guard). Emits NOTHING to the outbox.
     */
    @Transactional
    public AasmaanCaseNote addNote(long caseId, String note, String author) {
        AasmaanCase c = load(caseId);
        if (note == null || note.isBlank()) {
            throw new InvalidGovtRequestException("BEXA003", "note is required");
        }
        if (c.isClosed()) {
            throw new InvalidGovtRequestException("GOVT_CASE_CLOSED",
                    "Cannot add a note to CLOSED case " + caseId);
        }

        Timestamp nowTs = now();
        AasmaanCaseNote n = new AasmaanCaseNote();
        n.setNoteId(nextId("t_aasmaan_case_note_seq"));
        n.setCaseId(caseId);
        n.setNote(note);
        n.setAuthor(author);
        n.setAddedBy(AUDIT_ACTOR_ID);
        n.setAddedDatetime(nowTs);
        noteRepository.save(n);
        log.info("Added note {} to case {}", n.getNoteId(), caseId);
        return n;
    }

    // ------------------------------------------------------------------ status

    /**
     * Forward-only status move {status}: 1=OPEN -&gt; 2=IN_PROGRESS -&gt; 3=CLOSED.
     * Guards: case must exist (404); target must parse to 1..3 (BEXA003);
     * only the adjacent forward step is legal (400 GOVT_ILLEGAL_TRANSITION for
     * repeats, backwards moves and skips). Moving to CLOSED stamps closed_on.
     * Emits NOTHING to the outbox.
     */
    @Transactional
    public AasmaanCase updateStatus(long caseId, String rawStatus) {
        AasmaanCase c = load(caseId);
        short target = parseStatus(rawStatus);
        if (!c.canTransitionTo(target)) {
            throw new InvalidGovtRequestException("GOVT_ILLEGAL_TRANSITION",
                    "Illegal status move for case " + caseId + ": "
                            + statusName(c.getStatus()) + " -> " + statusName(target)
                            + " — forward-only 1=OPEN -> 2=IN_PROGRESS -> 3=CLOSED");
        }
        c.setStatus(target);
        if (target == AasmaanCase.STATUS_CLOSED) {
            c.setClosedOn(LocalDate.now());
        }
        c.setUpdatedBy(AUDIT_ACTOR_ID);
        c.setUpdatedDatetime(now());
        caseRepository.save(c);
        log.info("Case {} moved to {}", caseId, statusName(target));
        return c;
    }

    // ------------------------------------------------------------------ queries

    @Transactional(readOnly = true)
    public AasmaanCase get(long caseId) {
        return load(caseId);
    }

    @Transactional(readOnly = true)
    public List<AasmaanCaseNote> notes(long caseId) {
        load(caseId);   // 404 for unknown case
        return noteRepository.findByCaseIdOrderByNoteIdAsc(caseId);
    }

    @Transactional(readOnly = true)
    public List<AasmaanCase> list(Short status, Long patientId) {
        return caseRepository.search(status, patientId);
    }

    // ------------------------------------------------------------------ helpers

    private AasmaanCase load(long caseId) {
        return caseRepository.findById(caseId)
                .orElseThrow(() -> new GovtNotFoundException("NOT_FOUND",
                        "Aasmaan case not found: " + caseId));
    }

    /** Status filter/parse: numeric (1..3) or name (OPEN, IN_PROGRESS, CLOSED). */
    public static short parseStatus(String raw) {
        if (raw == null) {
            throw new InvalidGovtRequestException("BEXA003", "status is required");
        }
        try {
            short v = Short.parseShort(raw.trim());
            if (v >= AasmaanCase.STATUS_OPEN && v <= AasmaanCase.STATUS_CLOSED) {
                return v;
            }
            throw new InvalidGovtRequestException("BEXA003", "Unknown status: " + raw);
        } catch (NumberFormatException ignored) {
            // fall through to name mapping
        }
        return switch (raw.trim().toUpperCase()) {
            case "OPEN" -> AasmaanCase.STATUS_OPEN;
            case "IN_PROGRESS" -> AasmaanCase.STATUS_IN_PROGRESS;
            case "CLOSED" -> AasmaanCase.STATUS_CLOSED;
            default -> throw new InvalidGovtRequestException("BEXA003", "Unknown status: " + raw);
        };
    }

    public static String statusName(short s) {
        return switch (s) {
            case AasmaanCase.STATUS_OPEN -> "OPEN";
            case AasmaanCase.STATUS_IN_PROGRESS -> "IN_PROGRESS";
            case AasmaanCase.STATUS_CLOSED -> "CLOSED";
            default -> String.valueOf(s);
        };
    }

    private Long nextId(String sequenceName) {
        Number n = (Number) em.createNativeQuery("SELECT nextval('ihms_govt." + sequenceName + "')")
                .getSingleResult();
        return n.longValue();
    }

    private static Timestamp now() {
        return Timestamp.from(Instant.now());
    }
}
