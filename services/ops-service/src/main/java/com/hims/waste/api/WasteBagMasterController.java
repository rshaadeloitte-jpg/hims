package com.hims.waste.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.hims.waste.application.WasteBagMasterService;
import com.hims.waste.domain.model.WasteBagMaster;
import io.swagger.v3.oas.annotations.Operation;
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

/**
 * Waste bag category master (MIG-023 sub-slice, S19) — clean target of legacy
 * M_WASTEBAGMASTER (@CrudApi allowedFilters=status). Server-owned ids
 * (waste_bag_master_seq) + audit fields.
 */
@RestController
@RequestMapping("/api/v1/waste-bag-masters")
@Tag(name = "Waste Bag Masters (MIG-023)", description = "Bio-medical waste bag category master CRUD")
public class WasteBagMasterController {

    private final WasteBagMasterService service;

    public WasteBagMasterController(WasteBagMasterService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Create a waste bag category",
            description = "Server stamps id (waste_bag_master_seq) + audit fields; code+name required.")
    public ResponseEntity<WasteBagMaster> create(@RequestBody(required = false) JsonNode body,
                                                 @RequestParam(required = false) Long unitId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createBag(body, unitId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a waste bag category (patch semantics: null fields untouched)")
    public WasteBagMaster update(@PathVariable long id, @RequestBody(required = false) JsonNode body) {
        return service.updateBag(id, body);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a waste bag category (hard delete)")
    public WasteBagMaster remove(@PathVariable long id) {
        return service.removeBag(id);
    }

    @GetMapping
    @Operation(summary = "List active waste bag categories",
            description = "Optional unitId scope; default all active categories, id ascending.")
    public List<WasteBagMaster> list(@RequestParam(required = false) Long unitId) {
        return service.listBags(unitId);
    }

    @GetMapping("/{id}")
    public WasteBagMaster get(@PathVariable long id) {
        return service.getBag(id);
    }
}
