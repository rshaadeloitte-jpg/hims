package com.hims.equipment.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.hims.equipment.application.WarrantyService;
import com.hims.equipment.domain.model.EquipmentWarranty;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Warranty / AMC API (MIG-023 Phase 4) — covers per asset, updates, expiry
 * alerting and the AMC/warranty count summary. Timestamps in payloads bind as
 * ISO-8601 ({@code 2026-01-15T00:00:00Z}).
 */
@RestController
@RequestMapping("/api/v1/equipment")
@Tag(name = "Equipment Warranty & AMC (MIG-023)",
        description = "Warranty / AMC cover records per asset with expiry "
                + "alerting and count summaries")
public class WarrantyController {

    private final WarrantyService warrantyService;

    public WarrantyController(WarrantyService warrantyService) {
        this.warrantyService = warrantyService;
    }

    @PostMapping("/{equipmentId}/warranties")
    @Operation(summary = "Add a warranty / AMC cover to an asset",
            description = "Body is the cover record: {warrantyType? (default WARRANTY; free dictionary "
                    + "e.g. WARRANTY, AMC, CMC), startDate?, endDate?, vendor?, terms?}. Server assigns id "
                    + "from equipment_warranty_seq and pairs equipmentId from the path. Rejected for unknown, "
                    + "inactive or CONDEMNED assets. Timestamp fields bind as ISO-8601.")
    public ResponseEntity<EquipmentWarranty> add(@PathVariable long equipmentId,
                                                 @RequestBody(required = false) JsonNode body,
                                                 @RequestParam(required = false) Long unitId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(warrantyService.add(equipmentId, body, unitId));
    }

    @GetMapping("/{equipmentId}/warranties")
    @Operation(summary = "List warranties of an asset",
            description = "Active covers of one asset, newest first.")
    public List<EquipmentWarranty> listByEquipment(@PathVariable long equipmentId) {
        return warrantyService.listByEquipment(equipmentId);
    }

    @PutMapping("/warranties/{id}")
    @Operation(summary = "Update a warranty / AMC cover",
            description = "Null patch fields are ignored; id/equipmentId/unit/audit columns are server-owned. "
                    + "A supplied warrantyType replaces the old one (trimmed).")
    public EquipmentWarranty update(@PathVariable long id,
                                    @RequestBody(required = false) JsonNode body) {
        return warrantyService.update(id, body);
    }

    @GetMapping("/warranties/expiring")
    @Operation(summary = "List soon-to-expire warranties",
            description = "Active covers whose end_date falls within the next N days (days defaults to 30, "
                    + "must be positive; window starts today so a cover lapsing later today still alerts). "
                    + "Covers without an end_date never expire and are excluded. Optional unitId/equipmentId "
                    + "scoping. Ordered by end_date ascending — most urgent first.")
    public List<EquipmentWarranty> expiring(@RequestParam(required = false) Long days,
                                            @RequestParam(required = false) Long unitId,
                                            @RequestParam(required = false) Long equipmentId) {
        return warrantyService.expiring(days, unitId, equipmentId);
    }

    @GetMapping("/warranties/counts")
    @Operation(summary = "AMC / warranty count summary",
            description = "Active-cover summary: totalActive, expired (end_date already lapsed), "
                    + "expiringIn30Days and byType breakdown per warrantyType dictionary value. "
                    + "Optional unitId scoping.")
    public WarrantyCounts counts(@RequestParam(required = false) Long unitId) {
        return warrantyService.counts(unitId);
    }
}
