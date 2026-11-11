package com.hims.queue.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.queue.application.exceptions.InvalidQueueRequestException;
import com.hims.queue.application.exceptions.QueueNotFoundException;
import com.hims.queue.domain.model.Counter;
import com.hims.queue.domain.model.DailyCounter;
import com.hims.queue.domain.model.QueueToken;
import com.hims.queue.domain.repository.CounterRepository;
import com.hims.queue.domain.repository.DailyCounterRepository;
import com.hims.queue.domain.repository.QueueTokenRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Queue & Kiosk token lifecycle (MIG-022 / S29) — clean target of the legacy
 * M27 cluster (QueueManagmentServiceImpl /queueManagment: manageTokenSchema,
 * callGetPatientListSchema, assignNextPatientToDoctor, stopAndStartCounterQueue).
 *
 * State machine (forward-only, documented improvement over legacy raw UPDATEs):
 *   WAITING(1) -> CALLED(2) -> COMPLETED(3) | NO_SHOW(4)
 *   transfer allowed from WAITING/CALLED -> back to WAITING at target counter.
 *
 * Priority: EMERGENCY(2) tokens jump ahead of NORMAL(1) on call-next (FIFO within
 * priority). Daily reset via daily_counter row per queue_date, bumped inside the
 * issuance transaction so rollback never burns a number (IMP-DEC-033 pattern;
 * legacy Redis <qName>_S counters leaked on rollback and were never source-of-truth).
 *
 * REST-only: no queue-produced events in KAFKA-EVENT-CATALOG phase 1.
 *
 * Deferred cross-domain integrations:
 *   - getLastVisitStatus split-brain read (UNKNOWN-026) — appointment/encounter domain
 *   - questionnaire attach (QuestionAnswerDto), patient photo, doctor batch slots
 *   - Redis/kiosk live display projection (optional; PG dashboard endpoint covers it)
 */
@Service
public class QueueService {

    private static final Logger log = LoggerFactory.getLogger(QueueService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    private final QueueTokenRepository tokenRepository;
    private final CounterRepository counterRepository;
    private final DailyCounterRepository dailyCounterRepository;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager em;

    public QueueService(QueueTokenRepository tokenRepository,
                        CounterRepository counterRepository,
                        DailyCounterRepository dailyCounterRepository,
                        ObjectMapper objectMapper) {
        this.tokenRepository = tokenRepository;
        this.counterRepository = counterRepository;
        this.dailyCounterRepository = dailyCounterRepository;
        this.objectMapper = objectMapper;
    }

    // ------------------------------------------------------------------ issue

    /**
     * Issue a token (legacy manageTokenSchema add). Server stamps queueDate,
     * daily tokenNo + display no. Starts WAITING; counter assigned at call-next
     * or explicit transfer.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public QueueToken issue(JsonNode payload, Long unitId) {
        QueueToken t = bind(payload, QueueToken.class);
        if (t.getPatientId() == null || t.getDepartmentId() == null) {
            throw new InvalidQueueRequestException("BEXA003", "patientId and departmentId are required");
        }
        if (t.getPriority() != null && t.getPriority() != QueueToken.PRIORITY_NORMAL
                && t.getPriority() != QueueToken.PRIORITY_EMERGENCY) {
            throw new InvalidQueueRequestException("BEXA003", "Invalid priority: " + t.getPriority());
        }
        if (t.getQueueStatus() != null && t.getQueueStatus() != QueueToken.WAITING) {
            throw new InvalidQueueRequestException("BEXA003", "queueStatus is server-owned");
        }

        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        t.setId(nextId("queue_token_seq"));
        t.setQueueDate(today);
        t.setTokenNo(nextDailyNo(today));
        if (unitId != null) {
            t.setUnitId(unitId);
        }
        if (t.getStatus() == null) {
            t.setStatus(true);
        }
        t.setSynchronizedd(false);
        t.setAddedBy(AUDIT_ACTOR_ID);
        t.setAddedDatetime(Timestamp.from(Instant.now()));
        QueueToken saved = tokenRepository.save(t);
        log.info("Token issued id={} no={} patient={} dept={} emergency={}",
                saved.getId(), saved.getTokenNo(), saved.getPatientId(), saved.getDepartmentId(),
                saved.getPriority() == QueueToken.PRIORITY_EMERGENCY);
        return saved;
    }

    // -------------------------------------------------------------- call next

    /** Call the next waiting token for a counter's department (legacy assignNextPatientToDoctor). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public QueueToken callNext(Long counterId) {
        Counter counter = counterRepository.findById(counterId)
                .orElseThrow(() -> new QueueNotFoundException("NOT_FOUND", "Counter not found: " + counterId));
        if (!Boolean.TRUE.equals(counter.getStatus())) {
            throw new InvalidQueueRequestException("BEXA003", "Counter is deactivated");
        }
        Long dept = counter.getDepartmentId();
        if (dept == null) {
            throw new InvalidQueueRequestException("BEXA003", "Counter has no department mapping");
        }
        Pageable one = PageRequest.of(0, 1);
        List<QueueToken> next = tokenRepository.findNext(LocalDate.now(ZoneId.systemDefault()), dept,
                QueueToken.WAITING, one);
        if (next.isEmpty()) {
            throw new InvalidQueueRequestException("BEXA003", "Queue empty for counter " + counter.getCode());
        }
        QueueToken t = next.getFirst();
        t.setCounterId(counter.getId());
        t.setQueueStatus(QueueToken.CALLED);
        t.setCalledAt(Timestamp.from(Instant.now()));
        t.setUpdatedBy(AUDIT_ACTOR_ID);
        log.info("Token {} called at counter {} ({})", t.getTokenNo(), counter.getCode(), counter.getId());
        return t;
    }

    // ---------------------------------------------------------------- actions

    @Transactional(readOnly = true)
    public List<QueueToken> listTokens(Long counterId) {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        return counterId == null
                ? tokenRepository.findByQueueDateAndStatusTrueOrderByIdAsc(today)
                : tokenRepository.findByQueueDateAndCounterIdAndStatusTrueOrderByIdAsc(today, counterId);
    }

    @Transactional(readOnly = true)
    public List<QueueToken> listTokensByDepartment(Long departmentId) {
        if (departmentId == null) throw new InvalidQueueRequestException("BEXA003", "departmentId is required");
        return tokenRepository.findByQueueDateAndDepartmentIdAndStatusTrueOrderByIdAsc(
                LocalDate.now(ZoneId.systemDefault()), departmentId);
    }

    @Transactional(readOnly = true)
    public List<Counter> listCountersByDepartment(Long departmentId) {
        if (departmentId == null) throw new InvalidQueueRequestException("BEXA003", "departmentId is required");
        return counterRepository.findByDepartmentIdAndStatusTrueOrderByIdAsc(departmentId);
    }

    /** Transfer a token to another counter (legacy stopAndStartCounterQueue parity). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public QueueToken transfer(Long tokenId, JsonNode payload) {
        Long toCounterId = fieldAsLong(payload, "toCounterId");
        if (toCounterId == null) {
            throw new InvalidQueueRequestException("BEXA003", "toCounterId is required");
        }
        Counter target = counterRepository.findById(toCounterId)
                .orElseThrow(() -> new QueueNotFoundException("NOT_FOUND", "Counter not found: " + toCounterId));
        QueueToken t = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new QueueNotFoundException("NOT_FOUND", "Token not found"));
        if (!Boolean.TRUE.equals(t.getStatus())) {
            throw new InvalidQueueRequestException("BEXA003", "Invalid input data !");
        }
        if (t.isTerminal()) {
            throw new InvalidQueueRequestException("BEXA003",
                    "Cannot transfer a terminal token (status " + t.getQueueStatus() + ")");
        }
        t.setCounterId(target.getId());
        if (target.getDepartmentId() != null) {
            // token joins the target counter's department pool (stopAndStartCounterQueue
            // parity: moving a patient to another counter moves them into that queue)
            t.setDepartmentId(target.getDepartmentId());
        }
        // re-queue semantics: must be called again at the target counter
        t.setQueueStatus(QueueToken.WAITING);
        t.setCalledAt(null);
        t.setUpdatedBy(AUDIT_ACTOR_ID);
        log.info("Token {} transferred to counter {}", t.getTokenNo(), target.getCode());
        return t;
    }

    /** Mark a CALLED token as no-show (fetchAbsentToken parity). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public QueueToken markNoShow(Long tokenId) {
        QueueToken t = requireTransition(tokenId, QueueToken.CALLED, "no-show");
        t.setQueueStatus(QueueToken.NO_SHOW);
        t.setCompletedAt(Timestamp.from(Instant.now()));
        t.setUpdatedBy(AUDIT_ACTOR_ID);
        log.info("Token {} marked NO_SHOW", t.getTokenNo());
        return t;
    }

    /** Complete a CALLED token (consultation done). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public QueueToken complete(Long tokenId) {
        QueueToken t = requireTransition(tokenId, QueueToken.CALLED, "complete");
        t.setQueueStatus(QueueToken.COMPLETED);
        t.setCompletedAt(Timestamp.from(Instant.now()));
        t.setUpdatedBy(AUDIT_ACTOR_ID);
        log.info("Token {} completed", t.getTokenNo());
        return t;
    }

    // -------------------------------------------------------------- dashboard

    /** Display projection for kiosk/TV/nursing desk: counts + now-serving + waiting list. */
    @Transactional(readOnly = true)
    public Map<String, Object> dashboard(Long unitId) {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("queueDate", today.toString());
        out.put("waiting", tokenRepository.countByQueueDateAndQueueStatus(today, QueueToken.WAITING));
        out.put("called", tokenRepository.countByQueueDateAndQueueStatus(today, QueueToken.CALLED));
        out.put("completed", tokenRepository.countByQueueDateAndQueueStatus(today, QueueToken.COMPLETED));
        out.put("noShow", tokenRepository.countByQueueDateAndQueueStatus(today, QueueToken.NO_SHOW));

        List<QueueToken> calledNow = tokenRepository.findByDateAndStatus(today, QueueToken.CALLED, PageRequest.of(0, 200));
        Map<String, Object> nowServing = new LinkedHashMap<>();
        for (QueueToken t : calledNow) {
            if (t.getCounterId() != null) {
                nowServing.put(String.valueOf(t.getCounterId()), t.getTokenNo());   // last call wins per counter
            }
        }
        out.put("nowServing", nowServing);

        List<Map<String, Object>> waitingList = tokenRepository
                .findByDateAndStatus(today, QueueToken.WAITING, PageRequest.of(0, 20)).stream()
                .map(this::toSummary)
                .toList();
        out.put("waitingList", waitingList);
        return out;
    }

    /** Department-specific dashboard (M27 rad/patho): filtered by departmentId. */
    @Transactional(readOnly = true)
    public Map<String, Object> dashboardByDepartment(Long departmentId) {
        if (departmentId == null) throw new InvalidQueueRequestException("BEXA003", "departmentId is required");
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("queueDate", today.toString());
        out.put("departmentId", departmentId);
        out.put("waiting", tokenRepository.countByQueueDateAndDepartmentIdAndQueueStatus(today, departmentId, QueueToken.WAITING));
        out.put("called", tokenRepository.countByQueueDateAndDepartmentIdAndQueueStatus(today, departmentId, QueueToken.CALLED));
        out.put("completed", tokenRepository.countByQueueDateAndDepartmentIdAndQueueStatus(today, departmentId, QueueToken.COMPLETED));
        out.put("noShow", tokenRepository.countByQueueDateAndDepartmentIdAndQueueStatus(today, departmentId, QueueToken.NO_SHOW));

        List<QueueToken> calledNow = tokenRepository.findByDateDeptAndStatus(today, departmentId, QueueToken.CALLED, PageRequest.of(0, 200));
        Map<String, Object> nowServing = new LinkedHashMap<>();
        for (QueueToken t : calledNow) {
            if (t.getCounterId() != null) nowServing.put(String.valueOf(t.getCounterId()), t.getTokenNo());
        }
        out.put("nowServing", nowServing);
        List<Map<String, Object>> waitingList = tokenRepository
                .findByDateDeptAndStatus(today, departmentId, QueueToken.WAITING, PageRequest.of(0, 20)).stream()
                .map(this::toSummary).toList();
        out.put("waitingList", waitingList);
        // counters in this department (cabin board)
        out.put("counters", counterRepository.findByDepartmentIdAndStatusTrueOrderByIdAsc(departmentId).stream()
                .map(c -> Map.of("id", c.getId(), "code", c.getCode(), "name", c.getName())).toList());
        return out;
    }

    /** Cabin board: single counter's queue (M27 cabin queue board). */
    @Transactional(readOnly = true)
    public Map<String, Object> cabinBoard(Long counterId) {
        Counter c = counterRepository.findById(counterId)
                .orElseThrow(() -> new QueueNotFoundException("NOT_FOUND", "Counter not found: " + counterId));
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("counterId", c.getId());
        out.put("counterCode", c.getCode());
        out.put("departmentId", c.getDepartmentId());
        out.put("queueDate", today.toString());
        out.put("tokens", tokenRepository.findByQueueDateAndCounterIdAndStatusTrueOrderByIdAsc(today, counterId).stream()
                .map(this::toSummary).toList());
        // if counter has department, include dept waiting as well
        if (c.getDepartmentId() != null) {
            out.put("departmentWaiting", tokenRepository.countByQueueDateAndDepartmentIdAndQueueStatus(today, c.getDepartmentId(), QueueToken.WAITING));
        }
        return out;
    }

    // ---------------------------------------------------------------- masters

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Counter createCounter(JsonNode payload, Long unitId) {
        Counter c = bind(payload, Counter.class);
        if (c.getCode() == null || c.getCode().isBlank() || c.getName() == null || c.getName().isBlank()) {
            throw new InvalidQueueRequestException("BEXA003", "code and name are required");
        }
        c.setId(nextId("counter_seq"));
        if (c.getStatus() == null) {
            c.setStatus(true);
        }
        if (unitId != null) {
            c.setUnitId(unitId);
        }
        c.setSynchronizedd(false);
        c.setAddedBy(AUDIT_ACTOR_ID);
        c.setAddedDatetime(Timestamp.from(Instant.now()));
        return counterRepository.save(c);
    }

    @Transactional(readOnly = true)
    public List<Counter> listCounters(Boolean activeOnly) {
        return Boolean.TRUE.equals(activeOnly)
                ? counterRepository.findByStatusTrueOrderByIdAsc()
                : counterRepository.findAll();
    }

    // ---------------------------------------------------------------- helpers

    private QueueToken requireTransition(Long tokenId, short expected, String action) {
        QueueToken t = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new QueueNotFoundException("NOT_FOUND", "Token not found"));
        if (!Boolean.TRUE.equals(t.getStatus())) {
            throw new InvalidQueueRequestException("BEXA003", "Invalid input data !");
        }
        if (t.getQueueStatus() != expected) {
            throw new InvalidQueueRequestException("BEXA003",
                    "Cannot " + action + " token in status " + t.getQueueStatus());
        }
        return t;
    }

    /** Daily-reset counter bump inside the caller's transaction (IMP-DEC-033 pattern). */
    private long nextDailyNo(LocalDate date) {
        em.createNativeQuery("""
                INSERT INTO ihms_queue.daily_counter (counter_date, last_no) VALUES (:d, 0)
                ON CONFLICT (counter_date) DO NOTHING
                """)
                .setParameter("d", date)
                .executeUpdate();
        Object r = em.createNativeQuery(
                        "UPDATE ihms_queue.daily_counter SET last_no = last_no + 1 WHERE counter_date = :d RETURNING last_no")
                .setParameter("d", date)
                .getSingleResult();
        return ((Number) r).longValue();
    }

    private long nextId(String sequenceName) {
        return ((Number) em.createNativeQuery("SELECT nextval('ihms_queue.\"" + sequenceName + "\"')")
                .getSingleResult()).longValue();
    }

    private Map<String, Object> toSummary(QueueToken t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("tokenNo", t.getTokenNo());
        m.put("patientId", t.getPatientId());
        m.put("departmentId", t.getDepartmentId());
        m.put("counterId", t.getCounterId());
        m.put("priority", t.getPriority());
        m.put("queueStatus", t.getQueueStatus());
        return m;
    }

    private <T> T bind(JsonNode payload, Class<T> type) {
        if (payload == null || payload.isNull() || payload.isMissingNode()) {
            throw new InvalidQueueRequestException("BEXA003", "Invalid input data !");
        }
        try {
            return objectMapper.treeToValue(payload, type);
        } catch (Exception e) {
            throw new InvalidQueueRequestException("BEXA003", "Invalid input data !");
        }
    }

    private Long fieldAsLong(JsonNode payload, String field) {
        if (payload == null || payload.isNull() || !payload.hasNonNull(field)) {
            return null;
        }
        try {
            return payload.get(field).asLong();
        } catch (Exception e) {
            throw new InvalidQueueRequestException("BEXA003", "Invalid input data !");
        }
    }
}
