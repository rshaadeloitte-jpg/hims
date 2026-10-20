package com.hims.nursing.api;

import com.hims.nursing.application.NursingChartingService;
import com.hims.nursing.application.exceptions.InvalidNursingRequestException;
import com.hims.nursing.domain.model.NurseNote;
import com.hims.nursing.domain.model.NurseTask;
import com.hims.nursing.domain.model.PatientVitals;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/**
 * Phase-1 nursing API (GAP-REGISTER-2 WP-6): vitals record/list, nursing notes,
 * task planning + completion. patientId/admissionId are OPAQUE IDS in phase 1 —
 * cross-service validation against adt-service is deferred to the inter-service
 * phase (documented on {@link NursingChartingService}).
 */
@RestController
@Tag(name = "Nursing Care (WP-6)",
        description = "Phase-1 slice of the nursing module: vitals charting, nursing notes, "
                + "task lifecycle (1=PENDING 2=COMPLETED 3=CANCELLED). "
                + "Emits PROPOSED events VitalsRecorded / NurseTaskCompleted via the outbox.")
public class NursingController {

    private final NursingChartingService service;

    public NursingController(NursingChartingService service) {
        this.service = service;
    }

    // ------------------------------------------------------------------ vitals

    @PostMapping("/api/v1/patients/{patientId}/vitals")
    @Operation(summary = "Record a vital for a patient",
            description = "Required: vitalsType, value (numeric) — BEXA003-style 400 otherwise. "
                    + "admissionId optional and treated as an opaque id in phase 1. "
                    + "Writes the PROPOSED event VitalsRecorded to the outbox in the same transaction.")
    public ResponseEntity<PatientVitals> recordVital(@PathVariable long patientId,
                                                     @RequestBody Map<String, Object> body) {
        PatientVitals saved = service.recordVital(patientId,
                longOf(body, "admissionId"),
                strOf(body, "vitalsType"),
                decimalOf(body, "value"),
                strOf(body, "unit"),
                timestampOf(body, "recordedAt", false),
                longOf(body, "recordedBy"));
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/api/v1/patients/{patientId}/vitals")
    @Operation(summary = "List vitals for a patient",
            description = "Optional ?from=&to= (ISO date-time) range filter on recorded_at.")
    public List<PatientVitals> listVitals(@PathVariable long patientId,
                                          @RequestParam(required = false) String from,
                                          @RequestParam(required = false) String to) {
        return service.listVitals(patientId, parseTimestamp(from, "from"), parseTimestamp(to, "to"));
    }

    // ------------------------------------------------------------------ notes

    @PostMapping("/api/v1/admissions/{admissionId}/notes")
    @Operation(summary = "Add a nursing note to an admission",
            description = "admissionId is an opaque id in phase 1; note text is required.")
    public ResponseEntity<NurseNote> addNote(@PathVariable long admissionId,
                                             @RequestBody Map<String, Object> body) {
        NurseNote saved = service.addNote(admissionId, strOf(body, "note"), longOf(body, "nurseId"));
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/api/v1/admissions/{admissionId}/notes")
    @Operation(summary = "List nursing notes for an admission")
    public List<NurseNote> listNotes(@PathVariable long admissionId) {
        return service.listNotes(admissionId);
    }

    // ------------------------------------------------------------------ tasks

    @PostMapping("/api/v1/tasks")
    @Operation(summary = "Create a nursing task",
            description = "Status starts PENDING(1). Required: admissionId (opaque id in phase 1), taskDesc.")
    public ResponseEntity<NurseTask> createTask(@RequestBody Map<String, Object> body) {
        NurseTask saved = service.createTask(longOf(body, "admissionId"),
                strOf(body, "taskDesc"), strOf(body, "frequency"),
                timestampOf(body, "dueAt", true));
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/api/v1/tasks")
    @Operation(summary = "List nursing tasks",
            description = "Optional ?status= (1=PENDING|2=COMPLETED|3=CANCELLED) and/or ?admissionId=. "
                    + "Invalid status values are rejected with BEXA003.")
    public List<NurseTask> listTasks(@RequestParam(required = false) Short status,
                                     @RequestParam(required = false) Long admissionId) {
        return service.listTasks(status, admissionId);
    }

    @PostMapping("/api/v1/tasks/{id}/complete")
    @Operation(summary = "Complete a nursing task (PENDING -> COMPLETED only)",
            description = "Illegal transition from any other status -> 400 NURSING_ILLEGAL_TRANSITION. "
                    + "Writes the PROPOSED event NurseTaskCompleted to the outbox in the same transaction.")
    public NurseTask completeTask(@PathVariable long id,
                                  @RequestBody(required = false) Map<String, Object> body) {
        return service.completeTask(id, body == null ? null : longOf(body, "completedBy"));
    }

    @PostMapping("/api/v1/tasks/{id}/cancel")
    @Operation(summary = "Cancel a pending nursing task (PENDING -> CANCELLED)")
    public NurseTask cancelTask(@PathVariable long id) {
        return service.cancelTask(id);
    }

    // ------------------------------------------------------------------ helpers

    private static Long longOf(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try {
            return Long.valueOf(String.valueOf(v));
        } catch (NumberFormatException e) {
            throw new InvalidNursingRequestException("BEXA003", key + " must be numeric");
        }
    }

    private static String strOf(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v == null ? null : String.valueOf(v);
    }

    private static BigDecimal decimalOf(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null) return null;
        if (v instanceof Number n) return new BigDecimal(n.toString());
        try {
            return new BigDecimal(String.valueOf(v));
        } catch (NumberFormatException e) {
            throw new InvalidNursingRequestException("BEXA003", key + " must be numeric");
        }
    }

    /** Accept ISO date-time (yyyy-MM-dd'T'HH:mm[:ss]); recordedAt defaults to now when absent. */
    private static Timestamp timestampOf(Map<String, Object> body, String key, boolean nullable) {
        String s = strOf(body, key);
        if (s == null || s.isBlank()) return nullable ? null : Timestamp.from(Instant.now());
        try {
            return Timestamp.valueOf(LocalDateTime.parse(s));
        } catch (DateTimeParseException e) {
            throw new InvalidNursingRequestException("BEXA003",
                    key + " must be an ISO date-time (yyyy-MM-dd'T'HH:mm[:ss])");
        }
    }

    private static Timestamp parseTimestamp(String s, String name) {
        if (s == null || s.isBlank()) return null;
        try {
            return Timestamp.valueOf(LocalDateTime.parse(s));
        } catch (DateTimeParseException e) {
            throw new InvalidNursingRequestException("BEXA003",
                    name + " must be an ISO date-time (yyyy-MM-dd'T'HH:mm[:ss])");
        }
    }
}
