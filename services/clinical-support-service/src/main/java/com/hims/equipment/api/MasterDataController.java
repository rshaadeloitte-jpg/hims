package com.hims.equipment.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.hims.equipment.application.MasterDataService;
import com.hims.equipment.domain.model.EquipmentBase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

/**
 * Generic master-data API for all 13 V1 equipment master tables
 * (MIG-023 Task 17) — {@code /api/v1/equipment/masters/{masterType}} CRUD,
 * following the reference-service master pattern: one controller serves every
 * registered dictionary, the {@link com.hims.equipment.application.MasterEntityRegistry}
 * resolves the {@code masterType} path segment to its entity/sequence/whitelists.
 *
 * <p>Supported types (kebab-case): equipment-checklist, preventive-checklist,
 * functional-checklist, electrical-checklist, complaint-category,
 * complaint-status, complaint-type, compliance, maintenance-type,
 * working-status, amc-agent, service-agent, insurance-agent. An unknown type is
 * rejected 400 BEXA003 listing the supported set.</p>
 */
@RestController
@RequestMapping("/api/v1/equipment/masters")
@Tag(name = "Equipment Master Data (MIG-023)",
        description = "Generic CRUD over the 13 V1 master dictionaries (checklists, complaint "
                + "categories/types/statuses, compliance, maintenance types, working statuses, "
                + "AMC/service/insurance agents)")
public class MasterDataController {

    private final MasterDataService masterDataService;

    public MasterDataController(MasterDataService masterDataService) {
        this.masterDataService = masterDataService;
    }

    @GetMapping
    @Operation(summary = "List the supported masterType path values",
            description = "Discovery endpoint returning the kebab-case dictionary names accepted by "
                    + "/masters/{masterType}.")
    public Set<String> supportedTypes() {
        return masterDataService.supportedTypes();
    }

    @GetMapping("/{masterType}")
    @Operation(summary = "List a master dictionary",
            description = "Rows ordered by id (seed order). Optional filters: unitId scope, "
                    + "status (true = active only, false = deactivated only, omit = all) and a "
                    + "free-text search over the whitelisted code/description/name columns.")
    public List<EquipmentBase> list(
            @PathVariable String masterType,
            @Parameter(description = "Unit scope gate") @RequestParam(required = false) Long unitId,
            @Parameter(description = "true = active rows only, false = deactivated rows only, omit = all")
            @RequestParam(required = false) Boolean status,
            @RequestParam(required = false) String search) {
        return masterDataService.list(masterType, unitId, status, search);
    }

    @GetMapping("/{masterType}/{id}")
    @Operation(summary = "Get one master row by id",
            description = "Unknown id or unknown masterType -> 404 / 400 BEXA003 respectively.")
    public EquipmentBase get(@PathVariable String masterType, @PathVariable long id) {
        return masterDataService.get(masterType, id);
    }

    @PostMapping("/{masterType}")
    @Operation(summary = "Create a master row",
            description = "Payload binds straight onto the dictionary entity (e.g. {code, description} "
                    + "for category/status/type/compliance/checklist masters; {code, name, contactPerson?, ...} "
                    + "for agents). id is server-assigned from the table's own sequence — supplying one is "
                    + "rejected. status defaults to active when omitted; equipment_checklist.quantity "
                    + "defaults to the column default 1 when omitted. Required business fields are validated "
                    + "before any sequence burn.")
    public ResponseEntity<EquipmentBase> create(
            @PathVariable String masterType,
            @RequestBody(required = false) JsonNode body,
            @Parameter(description = "Owning unit stamped into unit_id/created_unit_id/updated_unit_id")
            @RequestParam(required = false) Long unitId) {
        EquipmentBase saved = masterDataService.create(masterType, body, unitId);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{masterType}/{id}")
    @Operation(summary = "Update a master row",
            description = "Null-safe patch: only present keys change. id, unitId and the audit block are "
                    + "server-owned and never patchable; status stays patchable so a row can be "
                    + "reactivated through PUT as well as re-created.")
    public EquipmentBase update(@PathVariable String masterType,
                                @PathVariable long id,
                                @RequestBody(required = false) JsonNode body) {
        return masterDataService.update(masterType, id, body);
    }

    @DeleteMapping("/{masterType}/{id}")
    @Operation(summary = "Delete a master row (soft)",
            description = "Dictionary rows are referenced by id across the domain (complaint_status, "
                    + "working_status, maintenance_type ids ARE API codes), so delete flips status=false "
                    + "+ update stamps and returns the tombstoned row instead of removing it.")
    public EquipmentBase delete(@PathVariable String masterType, @PathVariable long id) {
        return masterDataService.deactivate(masterType, id);
    }
}
