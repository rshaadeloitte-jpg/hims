package com.hims.equipment.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.hims.equipment.application.EquipmentService;
import com.hims.equipment.domain.model.Equipment;
import com.hims.equipment.domain.model.EquipmentTransfer;
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
 * Equipment asset-register API (MIG-023 sub-slice, S18) — register, search,
 * update, per-unit accession lookup, per-item listing and the guarded
 * department-transfer lifecycle.
 */
@RestController
@RequestMapping("/api/v1/equipment")
@Tag(name = "Equipment Asset Register (MIG-023)",
        description = "Medical equipment asset register: register, search/filter, update, "
                + "accession + item lookups, department transfers")
public class EquipmentController {

    private final EquipmentService service;

    public EquipmentController(EquipmentService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Register new equipment",
            description = "Body is the equipment record (itemId required). Server assigns id from "
                    + "equipment_seq, defaults workingStatus to WORKING(1), validates workingStatus against "
                    + "the dictionary and accessionNo uniqueness per unit (case-insensitive). "
                    + "unitId stamps the tenant/audit columns.")
    public ResponseEntity<Equipment> register(@RequestBody(required = false) JsonNode body,
                                              @RequestParam(required = false) Long unitId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.register(body, unitId));
    }

    @GetMapping
    @Operation(summary = "List / search the asset register",
            description = "Optional filters: unitId, workingStatus (1 WORKING, 2 NOT_WORKING, 3 UNDER_REPAIR, "
                    + "4 CONDEMNED), search (free text over accessionNo, serialNo, model, make, location, "
                    + "custodianName). Active assets only, newest first.")
    public List<Equipment> list(@RequestParam(required = false) Long unitId,
                                @RequestParam(required = false) Long workingStatus,
                                @RequestParam(required = false) String search) {
        return service.search(unitId, workingStatus, search);
    }

    @GetMapping("/by-accession/{accessionNo}")
    @Operation(summary = "Lookup equipment by accession number",
            description = "Case-insensitive. Pass unitId to scope the lookup (accession numbers are unique "
                    + "per unit); without it the newest matching asset wins.")
    public Equipment byAccession(@PathVariable String accessionNo,
                                 @RequestParam(required = false) Long unitId) {
        return service.findByAccession(accessionNo, unitId);
    }

    @GetMapping("/by-item/{itemId}")
    @Operation(summary = "List equipment for an item", description = "Active assets of one item, newest first.")
    public List<Equipment> byItem(@PathVariable long itemId) {
        return service.findByItem(itemId);
    }

    @GetMapping("/{id}")
    public Equipment get(@PathVariable long id) {
        return service.getEquipment(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update equipment details",
            description = "Null patch fields are ignored; id/unit/audit columns cannot be changed. A changed "
                    + "accessionNo is re-validated for per-unit uniqueness; workingStatus must be a valid "
                    + "dictionary id.")
    public Equipment update(@PathVariable long id, @RequestBody(required = false) JsonNode body) {
        return service.update(id, body);
    }

    @PostMapping("/{id}/transfer")
    @Operation(summary = "Transfer equipment to another department",
            description = "Body {toDepartmentId (required), reason?, transferDate? (ISO instant), toLocation?, "
                    + "toStoreId?}. Writes an equipment_transfer log row and moves department_id atomically. "
                    + "Rejected for inactive or CONDEMNED assets.")
    public ResponseEntity<EquipmentTransfer> transfer(@PathVariable long id,
                                                      @RequestBody(required = false) JsonNode body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.transfer(id, body));
    }

    @GetMapping("/{id}/transfers")
    @Operation(summary = "Transfer history of an asset", description = "Chronological (oldest first).")
    public List<EquipmentTransfer> transfers(@PathVariable long id) {
        return service.transferHistory(id);
    }
}
