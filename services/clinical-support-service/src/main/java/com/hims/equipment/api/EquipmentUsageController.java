package com.hims.equipment.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.hims.equipment.application.EquipmentUsageService;
import com.hims.equipment.domain.model.EquipmentUsageDetails;
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

import java.time.LocalDate;
import java.util.List;

/**
 * Equipment usage API (MIG-023 Phase 5) — machine-hours utilization log per
 * asset: log sessions, read an asset's history and build date-range reports.
 */
@RestController
@RequestMapping("/api/v1/equipment")
@Tag(name = "Equipment Usage (MIG-023)",
        description = "Usage tracking per asset: log sessions (machine-hours), history and date-range reports")
public class EquipmentUsageController {

    private final EquipmentUsageService usageService;

    public EquipmentUsageController(EquipmentUsageService usageService) {
        this.usageService = usageService;
    }

    @PostMapping("/{equipmentId}/usage")
    @Operation(summary = "Log a usage session for an asset",
            description = "Body is the usage record: {hoursUsed (required, > 0, fits NUMERIC(8,2)), "
                    + "usageDate? (ISO-8601 timestamp, defaults to now), operator?, remarks?}. Server assigns id "
                    + "from equipment_usage_details_seq and pairs equipmentId from the path. Rejected for unknown "
                    + "(404), inactive or CONDEMNED assets — a condemned machine no longer operates, so fresh "
                    + "usage rows would be data errors.")
    public ResponseEntity<EquipmentUsageDetails> log(@PathVariable long equipmentId,
                                                     @RequestBody(required = false) JsonNode body,
                                                     @RequestParam(required = false) Long unitId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(usageService.log(equipmentId, body, unitId));
    }

    @GetMapping("/{equipmentId}/usage")
    @Operation(summary = "Usage history of an asset",
            description = "Active usage entries of one asset, newest first.")
    public List<EquipmentUsageDetails> history(@PathVariable long equipmentId) {
        return usageService.history(equipmentId);
    }

    @GetMapping("/usage/report")
    @Operation(summary = "Usage report by date range",
            description = "Aggregates active usage entries whose usage_date falls inside the window. from/to are "
                    + "inclusive ISO dates (2026-08-01); each may be omitted — missing 'from' falls back to "
                    + "today-29 and missing 'to' to today (trailing 30 days default). Optional unitId / "
                    + "equipmentId scope the report. Entries are chronological (oldest first) with totalHours "
                    + "summed over exactly the returned rows.")
    public UsageReport report(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long unitId,
            @RequestParam(required = false) Long equipmentId) {
        return usageService.report(from, to, unitId, equipmentId);
    }
}
