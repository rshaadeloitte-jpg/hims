package com.hims.interop.application;

import com.hims.interop.application.exceptions.InteropNotFoundException;
import com.hims.interop.application.exceptions.InvalidRequestException;
import com.hims.interop.domain.model.EAushadhiSyncQueue;
import com.hims.interop.domain.model.EAushadhiSyncStatus;
import com.hims.interop.domain.repository.EAushadhiSyncQueueRepository;
import com.hims.interop.domain.repository.EAushadhiSyncStatusRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * e-Aushadhi drug catalogue sync queue (register WP-T1 extension).
 * FORWARD-ONLY status machine, pinned by unit + integration tests:
 *
 *   0=PENDING -> 1=SYNCED -> 2=FAILED
 *   FAILED (2) allowed ONLY from PENDING
 *   everything else (backward, skip-ahead, FAILED after SYNCED) -> 400
 *   INTEROP_ILLEGAL_TRANSITION.
 *
 * Every ACCEPTED transition appends a t_eaushadhi_sync_status history row.
 * OUT OF SCOPE (comment-only): the actual e-Aushadhi API call; POST .../status
 * is the sync emulation surface.
 */
@Service
public class EAushadhiService {

    private static final Logger log = LoggerFactory.getLogger(EAushadhiService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    private final EAushadhiSyncQueueRepository queueRepository;
    private final EAushadhiSyncStatusRepository statusRepository;

    @PersistenceContext
    private EntityManager em;

    public EAushadhiService(EAushadhiSyncQueueRepository queueRepository,
                            EAushadhiSyncStatusRepository statusRepository) {
        this.queueRepository = queueRepository;
        this.statusRepository = statusRepository;
    }

    /**
     * Enqueue a drug for e-Aushadhi sync {drugCode, drugName, manufacturer?, category?}.
     * Guards: drugCode required (<= 64); drugName required (<= 255).
     * New rows start PENDING (0). Emits NOTHING to the outbox.
     */
    @Transactional
    public EAushadhiSyncQueue enqueue(String drugCode, String drugName, String manufacturer, String category) {
        if (drugCode == null || drugCode.isBlank()) {
            throw new InvalidRequestException("BEXA003", "drugCode is required");
        }
        if (drugCode.length() > 64) {
            throw new InvalidRequestException("BEXA003", "drugCode must be at most 64 characters");
        }
        if (drugName == null || drugName.isBlank()) {
            throw new InvalidRequestException("BEXA003", "drugName is required");
        }
        if (drugName.length() > 255) {
            throw new InvalidRequestException("BEXA003", "drugName must be at most 255 characters");
        }
        if (manufacturer != null && manufacturer.length() > 255) {
            throw new InvalidRequestException("BEXA003", "manufacturer must be at most 255 characters");
        }
        if (category != null && category.length() > 64) {
            throw new InvalidRequestException("BEXA003", "category must be at most 64 characters");
        }

        Timestamp nowTs = now();
        EAushadhiSyncQueue row = new EAushadhiSyncQueue();
        row.setId(nextId("t_eaushadhi_sync_queue_seq"));
        row.setDrugCode(drugCode);
        row.setDrugName(drugName);
        row.setManufacturer(manufacturer);
        row.setCategory(category);
        row.setSyncStatus(EAushadhiSyncQueue.STATUS_PENDING);
        row.setSyncAttempts(0);
        row.setAddedBy(AUDIT_ACTOR_ID);
        row.setAddedDatetime(nowTs);
        queueRepository.save(row);
        log.info("Enqueued e-Aushadhi sync for drug {} ({}) as row {}", drugCode, drugName, row.getId());
        return row;
    }

    /**
     * Apply a sync status update {statusCode} — sync emulation. Guards:
     * unknown id -> 404; statusCode must be 1..2 (0 is the initial state,
     * never a transition TARGET) else 400 BEXA003; machine rules above else
     * 400 INTEROP_ILLEGAL_TRANSITION. On success the row moves to the new
     * status, syncAttempts is incremented, and a history row is appended.
     * Emits NOTHING to the outbox.
     */
    @Transactional
    public EAushadhiSyncQueue applyStatus(long queueId, Short statusCode, String detail) {
        EAushadhiSyncQueue row = queueRepository.findById(queueId)
                .orElseThrow(() -> new InteropNotFoundException("NOT_FOUND",
                        "e-Aushadhi sync queue row not found: " + queueId));
        short target = parseTarget(statusCode);

        Short nextForwardStep = EAushadhiSyncQueue.nextForward(row.getSyncStatus());
        boolean legal = target == EAushadhiSyncQueue.STATUS_FAILED
                ? row.canFail()
                : (nextForwardStep != null && target == nextForwardStep);
        if (!legal) {
            throw new InvalidRequestException("INTEROP_ILLEGAL_TRANSITION",
                    "Illegal e-Aushadhi transition " + row.getSyncStatus() + " -> " + target
                            + " — only forward 0->1, and FAILED (2) only from 0");
        }

        Timestamp nowTs = now();
        short previous = row.getSyncStatus();
        row.setSyncStatus(target);
        row.setSyncAttempts(row.getSyncAttempts() + 1);
        row.setLastSyncAt(nowTs);
        if (target == EAushadhiSyncQueue.STATUS_FAILED && detail != null) {
            row.setLastError(detail);
        }
        row.setUpdatedBy(AUDIT_ACTOR_ID);
        row.setUpdatedDatetime(nowTs);
        queueRepository.save(row);

        EAushadhiSyncStatus hist = new EAushadhiSyncStatus();
        hist.setId(nextId("t_eaushadhi_sync_status_seq"));
        hist.setSyncQueueId(queueId);
        hist.setStatusCode(target);
        hist.setTs(nowTs);
        hist.setDetail(detail == null ? ("transition " + previous + " -> " + target) : detail);
        hist.setAddedBy(AUDIT_ACTOR_ID);
        hist.setAddedDatetime(nowTs);
        statusRepository.save(hist);

        log.info("e-Aushadhi sync row {}: {} -> {}", queueId, previous, target);
        return row;
    }

    // ------------------------------------------------------------------ queries

    @Transactional(readOnly = true)
    public EAushadhiSyncQueue get(long queueId) {
        EAushadhiSyncQueue row = load(queueId);
        row.setHistory(statusRepository.findBySyncQueueIdOrderByTsAsc(queueId));
        return row;
    }

    @Transactional(readOnly = true)
    public java.util.List<EAushadhiSyncQueue> list(Short status) {
        return status == null
                ? queueRepository.findAllByOrderByIdDesc()
                : queueRepository.findBySyncStatusOrderByIdDesc(status);
    }

    // ------------------------------------------------------------------ helpers

    /** Transition targets are 1..2 only — 0 (PENDING) is never a transition target. */
    public static short parseTarget(Short raw) {
        if (raw == null || raw < EAushadhiSyncQueue.STATUS_SYNCED || raw > EAushadhiSyncQueue.STATUS_FAILED) {
            throw new InvalidRequestException("BEXA003",
                    "statusCode must be one of 1=SYNCED, 2=FAILED");
        }
        return raw;
    }

    private EAushadhiSyncQueue load(long queueId) {
        return queueRepository.findById(queueId)
                .orElseThrow(() -> new InteropNotFoundException("NOT_FOUND",
                        "e-Aushadhi sync queue row not found: " + queueId));
    }

    private Long nextId(String sequenceName) {
        Number n = (Number) em.createNativeQuery("SELECT nextval('ihms_interop." + sequenceName + "')")
                .getSingleResult();
        return n.longValue();
    }

    private static Timestamp now() {
        return Timestamp.from(Instant.now());
    }
}
