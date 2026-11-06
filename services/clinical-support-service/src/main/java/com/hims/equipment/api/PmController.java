package com.hims.equipment.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.hims.equipment.application.PmService;
import com.hims.equipment.domain.model.EquipmentPreventiveChecklist;
import com.hims.equipment.domain.model.EqpPmSchedule;
import com.hims.equipment.domain.model.EqpPreventiveMaintenance;
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
import java.time.ZoneId;
import java.util.List;

/**
 * Preventive-maintenance API (MIG-023 Phase 3) — PM schedules per asset, the
 * due list, completion recording (with automatic next-due roll-forward),
 * execution history and the PM checklist templates. Timestamps in payloads
 * bind as ISO-8601 ({@code 2026-01-15T00:00:00Z}).
 */
@RestController
@RequestMapping("/api/v1/equipment")
@Tag(name = "Equipment Preventive Maintenance (MIG-023)",
        description = "Recurring PM plans per asset, due listing, maintenance "
                + "completion records and the preventive checklist templates")
public class PmController {

    private final PmService pmService;

    public PmController(PmService pmService) {
        this.pmService = pmService;
    }

    @PostMapping("/pm-schedules")
    @Operation(summary = "Create a PM schedule",
            description = "Body is the schedule record (equipmentId required). Server assigns id from "
                    + "eqp_pm_schedule_seq, generates scheduleCode IHMS/EQ/PM/{yyyy}/{%05d} unless a unique "
                    + "code is supplied, defaults pmType to PREVENTIVE(1) (1 PREVENTIVE, 2 CORRECTIVE, "
                    + "3 CALIBRATION), pmDate to now, nextDueDate to pmDate and flags the plan-start month "
                    + "when no jan..dec flag is set. frequency is months between services. Optional details[] "
                    + "array saves planned occurrence lines. Rejected for unknown, inactive or CONDEMNED assets. "
                    + "Timestamp fields bind as ISO-8601.")
    public ResponseEntity<EqpPmSchedule> createSchedule(@RequestBody(required = false) JsonNode body,
                                                        @RequestParam(required = false) Long unitId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pmService.createSchedule(body, unitId));
    }

    @GetMapping("/pm-schedules")
    @Operation(summary = "List / search PM schedules",
            description = "Optional filters: unitId, equipmentId, pmType (1 PREVENTIVE, 2 CORRECTIVE, "
                    + "3 CALIBRATION), dueFrom/dueTo on next_due_date (ISO dates; dueTo is inclusive — "
                    + "everything already fallen due is dueTo <= today), search (free text over scheduleCode). "
                    + "Active schedules only, newest first.")
    public List<EqpPmSchedule> list(@RequestParam(required = false) Long unitId,
                                    @RequestParam(required = false) Long equipmentId,
                                    @RequestParam(required = false) Long pmType,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueFrom,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueTo,
                                    @RequestParam(required = false) String search) {
        return pmService.search(unitId, equipmentId, pmType,
                startOfDay(dueFrom), dayAfter(dueTo), search);
    }

    @GetMapping("/{equipmentId}/pm-schedules")
    @Operation(summary = "PM schedules of an asset", description = "Active schedules of one asset, newest first.")
    public List<EqpPmSchedule> schedulesOf(@PathVariable long equipmentId) {
        return pmService.schedulesOf(equipmentId);
    }

    @PostMapping("/pm-schedules/{id}/complete")
    @Operation(summary = "Record a completed maintenance execution",
            description = "Body is the execution record. Server owns equipmentId/pmScheduleId from the schedule, "
                    + "assigns id from eqp_preventive_maintenance_seq, generates maintenanceNo PM-{yyyy}-{%05d} "
                    + "unless supplied, defaults pmStatus to COMPLETED(3) (1 SCHEDULED, 2 IN_PROGRESS, 3 COMPLETED, "
                    + "4 CANCELLED) and maintenanceDate to now. Checklist results ride as TEXT columns "
                    + "(functionalCheckRemark, preventiveMaintenanceRemark, electricalSafetyRemark, calibration); "
                    + "amountIncurred is NUMERIC(19,2); optional pmScheduleDetailId must belong to the schedule. "
                    + "Atomically stamps lastCompletedDate and rolls nextDueDate forward by frequency months.")
    public ResponseEntity<EqpPreventiveMaintenance> complete(@PathVariable long id,
                                                             @RequestBody(required = false) JsonNode body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pmService.complete(id, body));
    }

    @GetMapping("/pm-schedules/{id}/history")
    @Operation(summary = "PM execution history of a schedule", description = "Chronological (oldest first).")
    public List<EqpPreventiveMaintenance> history(@PathVariable long id) {
        return pmService.history(id);
    }

    @GetMapping("/pm/checklists")
    @Operation(summary = "List PM checklist templates",
            description = "All active equipment_preventive_checklist templates (code + description), template order.")
    public List<EquipmentPreventiveChecklist> checklists() {
        return pmService.listChecklists();
    }

    @PostMapping("/pm/checklists")
    @Operation(summary = "Create a PM checklist template",
            description = "Body: {code (required, unique case-insensitively), description (required)}. "
                    + "Server assigns id from equipment_preventive_checklist_seq.")
    public ResponseEntity<EquipmentPreventiveChecklist> createChecklist(
            @RequestBody(required = false) JsonNode body,
            @RequestParam(required = false) Long unitId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pmService.createChecklist(body, unitId));
    }

    // --------------------------------------------------------------- helpers

    private static Timestamp startOfDay(LocalDate date) {
        return date == null ? null
                : Timestamp.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    /** Inclusive end-of-day bound expressed as the exclusive start of the next day. */
    private static Timestamp dayAfter(LocalDate date) {
        return date == null ? null
                : Timestamp.from(date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
