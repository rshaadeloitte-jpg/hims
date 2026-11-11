package com.hims.ot.api;

import com.hims.ot.application.OtScheduleService;
import com.hims.ot.domain.model.OtSchedule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * OT schedule API (GAP-REGISTER-3 WP-8).
 * Create enforces BR-OT-001 (admissionId XOR encounterId prerequisite, else 400
 * BEOT004; dependent-surgery discounts); start/complete/cancel guard the
 * PLANNED->IN_PROGRESS->DONE / CANCELLED lifecycle; BR-OT-002 pre_billing is
 * persisted and echoed (actual charge creation deferred to Billing Wave 4).
 * Outbox emits PROPOSED ProcedureScheduled (create) and SurgeryCompleted
 * (complete) per KAFKA-EVENT-CATALOG §3.
 */
@RestController
@RequestMapping("/api/v1/ot/schedules")
@Tag(name = "OT Schedules (WP-8)",
        description = "OT scheduling with BR-OT-001 prerequisites (BEOT004), dependent-surgery "
                + "discounts and lifecycle guards; BR-OT-002 pre_billing flag; outbox events "
                + "ProcedureScheduled / SurgeryCompleted (both PROPOSED per catalog §3)")
public class OtScheduleController {

    private final OtScheduleService service;

    public OtScheduleController(OtScheduleService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Schedule an OT",
            description = "BR-OT-001: requires admissionId OR encounterId (400 code BEOT004 when neither "
                    + "is supplied); dependent-surgery rows carry discountPct; procedure rate defaults 0 "
                    + "(tariff lookup deferred to Reference). BR-OT-002: preBilling flag persisted and echoed "
                    + "(charge creation deferred to Billing Wave 4). Writes PROPOSED ProcedureScheduled to the "
                    + "outbox (topic ihms.ot.events, partition key patientId).")
    public ResponseEntity<Object> create(@RequestBody Map<String, Object> body) {
        OtSchedule saved = service.create(
                longOf(body, "patientId"),
                longOf(body, "encounterId"),
                longOf(body, "admissionId"),
                strOf(body, "otTable"),
                tsOf(body, "scheduledStart"),
                tsOf(body, "scheduledEnd"),
                longOf(body, "anesthetistId"),
                longOf(body, "surgeonId"),
                boolOf(body, "preBilling"),
                proceduresOf(body));
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    @Operation(summary = "Search OT schedules", description = "?date=YYYY-MM-DD&status=1|2|3|4 (both optional)")
    public List<OtSchedule> search(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer status) {
        return service.search(date, status);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one schedule incl. procedure details")
    public OtSchedule get(@PathVariable long id) {
        return service.get(id);
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "Start surgery", description = "Guard: only from status 1 PLANNED -> 2 IN_PROGRESS. Illegal transition -> 400.")
    public OtSchedule start(@PathVariable long id) {
        return service.start(id);
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete surgery", description = "Guard: only from status 2 IN_PROGRESS -> 3 DONE. Emits PROPOSED SurgeryCompleted (partition key patientId).")
    public OtSchedule complete(@PathVariable long id) {
        return service.complete(id);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel schedule", description = "Guard: from 1 PLANNED or 2 IN_PROGRESS -> 4 CANCELLED only; never from DONE.")
    public OtSchedule cancel(@PathVariable long id) {
        return service.cancel(id);
    }

    // ------------------------------------------------------------------ body mapping

    @SuppressWarnings("unchecked")
    private static List<OtScheduleService.ProcedureRequest> proceduresOf(Map<String, Object> body) {
        Object raw = body.get("procedures");
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<OtScheduleService.ProcedureRequest> out = new ArrayList<>();
        for (Object o : list) {
            if (o instanceof Map<?, ?> m) {
                Map<String, Object> pm = (Map<String, Object>) m;
                out.add(new OtScheduleService.ProcedureRequest(
                        strOf(pm, "procedureCode"),
                        strOf(pm, "procedureName"),
                        decimalOf(pm.get("rate"), "rate"),
                        intOf(pm, "dependentOfIndex"),
                        decimalOf(pm.get("discountPct"), "discountPct")));
            }
        }
        return out;
    }

    private static Long longOf(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        return Long.valueOf(String.valueOf(v));
    }

    private static Integer intOf(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        return Integer.valueOf(String.valueOf(v));
    }

    private static String strOf(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v == null ? null : String.valueOf(v);
    }

    private static Boolean boolOf(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null) return null;
        if (v instanceof Boolean b) return b;
        return Boolean.valueOf(String.valueOf(v));
    }

    private static java.math.BigDecimal decimalOf(Object v, String key) {
        if (v == null) return null;
        if (v instanceof Number n) return new java.math.BigDecimal(n.toString());
        try {
            return new java.math.BigDecimal(String.valueOf(v));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + " must be numeric");
        }
    }

    /** Accepts ISO-8601 ('2026-09-01T08:30:00[.SSS]' optionally with offset/Z) or 'yyyy-MM-dd[ T]HH:mm:ss'. */
    private static Timestamp tsOf(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null) return null;
        String s = String.valueOf(v).trim();
        // 1) full ISO instant / offset date-time
        try {
            return Timestamp.from(java.time.OffsetDateTime.parse(s).toInstant());
        } catch (DateTimeParseException ignored) { /* fall through */ }
        try {
            return Timestamp.from(java.time.Instant.parse(s));
        } catch (DateTimeParseException ignored) { /* fall through */ }
        // 2) local date-time without zone
        try {
            return Timestamp.valueOf(LocalDateTime.parse(s));
        } catch (DateTimeParseException ignored) { /* fall through */ }
        // 3) space-separated legacy form
        try {
            return Timestamp.valueOf(s.replace('T', ' '));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(key + " must be a valid timestamp");
        }
    }
}
