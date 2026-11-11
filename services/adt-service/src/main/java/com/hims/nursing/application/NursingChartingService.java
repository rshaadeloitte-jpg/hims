package com.hims.nursing.application;

import com.hims.nursing.application.exceptions.InvalidNursingRequestException;
import com.hims.nursing.application.exceptions.NursingResourceNotFoundException;
import com.hims.nursing.domain.model.NurseNote;
import com.hims.nursing.domain.model.NurseTask;
import com.hims.nursing.domain.model.PatientVitals;
import com.hims.nursing.domain.repository.NurseNoteRepository;
import com.hims.nursing.domain.repository.NurseTaskRepository;
import com.hims.nursing.domain.repository.PatientVitalsRepository;
import com.hims.nursing.infrastructure.outbox.OutboxService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Phase-1 nursing slice (GAP-REGISTER-2 WP-6, MICROSERVICE-CATALOG #12):
 * vitals charting, nursing notes, task planning + completion.
 *
 * Business rules:
 *  - Vitals: value must be numeric and required fields present -> BEXA003-style 400.
 *    Recording a vital writes the PROPOSED event VitalsRecorded to the outbox in the
 *    same transaction. NOTE: VitalsRecorded is a PROPOSED event per KAFKA-EVENT-CATALOG §3 /
 *    MICROSERVICE-CATALOG #12 ("E VitalsRecorded/NurseTaskCompleted (proposed, no consumers)");
 *    the contract may still change before any consumer ships.
 *  - Notes: free-text note per admission; no event in phase 1.
 *  - Tasks: create -> PENDING(1); complete allowed ONLY from PENDING(1) -> COMPLETED(2),
 *    emitting the PROPOSED event NurseTaskCompleted via the outbox in the same transaction;
 *    cancel 1->3. Illegal transition -> 400 NURSING_ILLEGAL_TRANSITION.
 *
 * BOUNDARY NOTE (phase 1): patientId / admissionId are accepted as OPAQUE IDS.
 * Cross-service validation against adt-service (admission exists / belongs to the
 * patient / admission not discharged) is deferred to the inter-service phase — see
 * GAP-REGISTER-2 WP-6. Until then no FK constraints are placed on these columns.
 *
 * Deferred slices of the 300-endpoint legacy nursing module: I/O charting
 * (T_INTAKEOUTPUT), nurse-patient assignment (T_NURSEPATIENTDETAILS), station
 * roster / IpdRoster, nursing station masters.
 */
@Service
public class NursingChartingService {

    private static final Logger log = LoggerFactory.getLogger(NursingChartingService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    /**
     * PROPOSED event (KAFKA-EVENT-CATALOG §3 — proposed, no consumers yet).
     * Type string 'VitalsRecorded'; treat as unstable until ratified.
     */
    public static final String EVENT_VITALS_RECORDED = "VitalsRecorded";

    /**
     * PROPOSED event (KAFKA-EVENT-CATALOG §3 — proposed, no consumers yet).
     * Type string 'NurseTaskCompleted'; treat as unstable until ratified.
     */
    public static final String EVENT_NURSE_TASK_COMPLETED = "NurseTaskCompleted";

    private final PatientVitalsRepository vitalsRepository;
    private final NurseNoteRepository noteRepository;
    private final NurseTaskRepository taskRepository;
    private final OutboxService outboxService;

    @PersistenceContext
    private EntityManager em;

    public NursingChartingService(PatientVitalsRepository vitalsRepository,
                                  NurseNoteRepository noteRepository,
                                  NurseTaskRepository taskRepository,
                                  OutboxService outboxService) {
        this.vitalsRepository = vitalsRepository;
        this.noteRepository = noteRepository;
        this.taskRepository = taskRepository;
        this.outboxService = outboxService;
    }

    // ------------------------------------------------------------------ vitals

    /**
     * Record a vital for a patient. Required: vitalsType, value (numeric);
     * BEXA003-style 400 otherwise. Emits PROPOSED event VitalsRecorded via the outbox.
     */
    @Transactional
    public PatientVitals recordVital(Long patientId, Long admissionId, String vitalsType,
                                     BigDecimal value, String unit, Timestamp recordedAt,
                                     Long recordedBy) {
        if (patientId == null) {
            throw new InvalidNursingRequestException("BEXA003", "patientId is required");
        }
        if (vitalsType == null || vitalsType.isBlank()) {
            throw new InvalidNursingRequestException("BEXA003", "vitalsType is required");
        }
        if (value == null) {
            throw new InvalidNursingRequestException("BEXA003", "value is required and must be numeric");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidNursingRequestException("BEXA003", "value must be non-negative");
        }
        if (recordedAt != null && recordedAt.after(Timestamp.from(Instant.now().plusSeconds(300)))) {
            throw new InvalidNursingRequestException("BEXA003", "recordedAt cannot be in the future");
        }
        PatientVitals v = new PatientVitals();
        v.setId(nextId("t_patientvitals_seq"));
        v.setPatientId(patientId);
        v.setAdmissionId(admissionId);   // opaque id — validated cross-service in later phase
        v.setVitalsType(vitalsType.trim());
        v.setValue(value);
        v.setUnit(unit);
        v.setRecordedAt(recordedAt != null ? recordedAt : Timestamp.from(Instant.now()));
        v.setRecordedBy(recordedBy != null ? recordedBy : AUDIT_ACTOR_ID);
        stampCreate(v);
        PatientVitals saved = vitalsRepository.save(v);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("vitalId", saved.getId());
        payload.put("patientId", saved.getPatientId());
        payload.put("admissionId", saved.getAdmissionId());
        payload.put("vitalsType", saved.getVitalsType());
        payload.put("value", saved.getValue());
        payload.put("unit", saved.getUnit());
        payload.put("recordedAt", saved.getRecordedAt().toString());
        payload.put("recordedBy", saved.getRecordedBy());
        outboxService.append(EVENT_VITALS_RECORDED, "vitals", saved.getId(),
                String.valueOf(saved.getPatientId()), toJson(payload),
                "vitals-recorded:" + saved.getId());
        log.info("Recorded vital id={} patient={} type={}", saved.getId(), patientId, vitalsType);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<PatientVitals> listVitals(Long patientId, Timestamp fromTs, Timestamp toTs) {
        if (patientId == null) {
            throw new InvalidNursingRequestException("BEXA003", "patientId is required");
        }
        return vitalsRepository.findByPatientInRange(patientId, fromTs, toTs);
    }

    // ------------------------------------------------------------------ notes

    /** Add a nursing note to an admission (opaque admission_id in phase 1). */
    @Transactional
    public NurseNote addNote(Long admissionId, String note, Long nurseId) {
        if (admissionId == null) {
            throw new InvalidNursingRequestException("BEXA003", "admissionId is required");
        }
        if (note == null || note.isBlank()) {
            throw new InvalidNursingRequestException("BEXA003", "note is required");
        }
        NurseNote n = new NurseNote();
        n.setId(nextId("t_nursenote_seq"));
        n.setAdmissionId(admissionId);   // opaque id — validated cross-service in later phase
        n.setNote(note.trim());
        n.setNurseId(nurseId != null ? nurseId : AUDIT_ACTOR_ID);
        n.setNotedAt(Timestamp.from(Instant.now()));
        stampCreate(n);
        return noteRepository.save(n);
    }

    @Transactional(readOnly = true)
    public List<NurseNote> listNotes(Long admissionId) {
        if (admissionId == null) {
            throw new InvalidNursingRequestException("BEXA003", "admissionId is required");
        }
        return noteRepository.findByAdmissionIdAndStatusTrueOrderByNotedAtDescIdDesc(admissionId);
    }

    // ------------------------------------------------------------------ tasks

    /** Create a nursing task (status starts PENDING). */
    @Transactional
    public NurseTask createTask(Long admissionId, String taskDesc, String frequency, Timestamp dueAt) {
        if (admissionId == null) {
            throw new InvalidNursingRequestException("BEXA003", "admissionId is required");
        }
        if (taskDesc == null || taskDesc.isBlank()) {
            throw new InvalidNursingRequestException("BEXA003", "taskDesc is required");
        }
        NurseTask t = new NurseTask();
        t.setId(nextId("t_nursetaskdetails_seq"));
        t.setAdmissionId(admissionId);   // opaque id — validated cross-service in later phase
        t.setTaskDesc(taskDesc.trim());
        t.setFrequency(frequency);
        t.setDueAt(dueAt);
        t.setTaskStatus(NurseTask.STATUS_PENDING);
        stampCreate(t);
        return taskRepository.save(t);
    }

    /**
     * Complete a task: PENDING(1) -> COMPLETED(2) ONLY. Any other current status is an
     * illegal transition (400). Emits PROPOSED event NurseTaskCompleted via the outbox.
     */
    @Transactional
    public NurseTask completeTask(long taskId, Long completedBy) {
        NurseTask t = taskRepository.findById(taskId)
                .orElseThrow(() -> new NursingResourceNotFoundException("NOT_FOUND",
                        "Nursing task not found: " + taskId));
        if (t.getTaskStatus() != NurseTask.STATUS_PENDING) {
            throw new InvalidNursingRequestException("NURSING_ILLEGAL_TRANSITION",
                    "complete allowed only from PENDING(1), current=" + t.getStatus());
        }
        t.setTaskStatus(NurseTask.STATUS_COMPLETED);
        t.setCompletedAt(Timestamp.from(Instant.now()));
        t.setCompletedBy(completedBy != null ? completedBy : AUDIT_ACTOR_ID);
        t.setUpdatedBy(AUDIT_ACTOR_ID);
        t.setUpdatedDatetime(Timestamp.from(Instant.now()));
        NurseTask saved = taskRepository.save(t);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("taskId", saved.getId());
        payload.put("admissionId", saved.getAdmissionId());
        payload.put("taskDesc", saved.getTaskDesc());
        payload.put("frequency", saved.getFrequency());
        payload.put("dueAt", saved.getDueAt() == null ? null : saved.getDueAt().toString());
        payload.put("completedAt", saved.getCompletedAt().toString());
        payload.put("completedBy", saved.getCompletedBy());
        outboxService.append(EVENT_NURSE_TASK_COMPLETED, "task", saved.getId(),
                String.valueOf(saved.getAdmissionId()), toJson(payload),
                "nurse-task-completed:" + saved.getId());
        log.info("Completed nursing task id={} admission={}", saved.getId(), saved.getAdmissionId());
        return saved;
    }

    /** Cancel a pending task: PENDING(1) -> CANCELLED(3). */
    @Transactional
    public NurseTask cancelTask(long taskId) {
        NurseTask t = taskRepository.findById(taskId)
                .orElseThrow(() -> new NursingResourceNotFoundException("NOT_FOUND",
                        "Nursing task not found: " + taskId));
        if (t.getTaskStatus() != NurseTask.STATUS_PENDING) {
            throw new InvalidNursingRequestException("NURSING_ILLEGAL_TRANSITION",
                    "cancel allowed only from PENDING(1), current=" + t.getStatus());
        }
        t.setTaskStatus(NurseTask.STATUS_CANCELLED);
        t.setUpdatedBy(AUDIT_ACTOR_ID);
        t.setUpdatedDatetime(Timestamp.from(Instant.now()));
        return taskRepository.save(t);
    }

    @Transactional(readOnly = true)
    public List<NurseTask> listTasks(Short status, Long admissionId) {
        Short s = normalizeStatus(status);
        if (s != null && admissionId != null) {
            return taskRepository.findByAdmissionIdAndTaskStatusOrderByDueAtAscIdAsc(admissionId, s);
        }
        if (s != null) {
            return taskRepository.findByTaskStatus(s);
        }
        if (admissionId != null) {
            return taskRepository.findByAdmissionIdOrderByIdDesc(admissionId);
        }
        return taskRepository.findAll();
    }

    /** Accept only 1|2|3 as explicit status filters; anything else is a 400. */
    private Short normalizeStatus(Short status) {
        if (status == null) return null;
        if (status < NurseTask.STATUS_PENDING || status > NurseTask.STATUS_CANCELLED) {
            throw new InvalidNursingRequestException("BEXA003",
                    "status must be 1=PENDING, 2=COMPLETED or 3=CANCELLED");
        }
        return status;
    }

    // ------------------------------------------------------------------ helpers

    private void stampCreate(com.hims.nursing.domain.model.NursingBase e) {
        e.setAddedBy(AUDIT_ACTOR_ID);
        e.setAddedDatetime(Timestamp.from(Instant.now()));
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
    }

    private Long nextId(String sequenceName) {
        Number n = (Number) em.createNativeQuery("SELECT nextval('ihms_nursing." + sequenceName + "')")
                .getSingleResult();
        return n.longValue();
    }
}
