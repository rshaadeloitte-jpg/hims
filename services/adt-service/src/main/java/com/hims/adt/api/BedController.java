package com.hims.adt.api;

import com.hims.adt.application.MasterDataService;
import com.hims.adt.domain.model.Bed;
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
import java.util.Map;

/** Bed master CRUD + availability query (GAP-REGISTER WP-5). */
@RestController
@RequestMapping("/api/v1/beds")
@Tag(name = "Beds (WP-5)",
        description = "Bed master (m_bed) and free-bed search per BR-IPD-002 "
                + "(occupied=false AND status=true AND isnoncensus/isot/ispreop/ispostop=false AND isrelease=true)")
public class BedController {

    private final MasterDataService service;
    private final com.hims.adt.application.AdmissionService admissionService;

    public BedController(MasterDataService service, com.hims.adt.application.AdmissionService admissionService) {
        this.service = service;
        this.admissionService = admissionService;
    }

    /** Housekeeping: CLEANING -> FREE so the bed re-enters allocation. */
    @PostMapping("/{id}/cleaning-complete")
    @Operation(summary = "Mark cleaning complete",
            description = "CLEANING -> FREE (400 ADT_BED_NOT_CLEANING when not in CLEANING). "
                    + "Emits BedStatusChanged (partition key bedId).")
    public Bed cleaningComplete(@PathVariable long id) {
        return admissionService.markCleaningComplete(id);
    }

    @PostMapping
    @Operation(summary = "Create a bed")
    public ResponseEntity<Bed> create(@RequestBody Map<String, Object> body) {
        Bed bed = service.createBed(
                WardController.strOf(body, "bedNo"),
                WardController.longOf(body, "wardId"),
                WardController.strOf(body, "category"),
                WardController.strOf(body, "building"),
                shortOf(body, "bedStatusId"),
                boolOf(body, "isnoncensus"), boolOf(body, "isot"),
                boolOf(body, "ispreop"), boolOf(body, "ispostop"),
                WardController.longOf(body, "unitId"));
        return ResponseEntity.status(HttpStatus.CREATED).body(bed);
    }

    @GetMapping
    @Operation(summary = "List beds", description = "?wardId= filters by ward.")
    public List<Bed> list(@RequestParam(required = false) Long wardId) {
        return service.listBeds(wardId);
    }

    @GetMapping("/available")
    @Operation(summary = "Free beds (BR-IPD-002 predicate)", description = "?wardId= optional filter.")
    public List<Bed> available(@RequestParam(required = false) Long wardId) {
        return service.availableBeds(wardId);
    }

    @GetMapping("/{id}")
    public Bed get(@PathVariable long id) {
        return service.getBed(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update bed attributes/status")
    public Bed update(@PathVariable long id, @RequestBody Map<String, Object> body) {
        return service.updateBed(id,
                WardController.strOf(body, "category"),
                WardController.strOf(body, "building"),
                shortOf(body, "bedStatusId"),
                boolOf(body, "isnoncensus"), boolOf(body, "isot"),
                boolOf(body, "ispreop"), boolOf(body, "ispostop"));
    }

    static Short shortOf(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null) return null;
        if (v instanceof Number n) return n.shortValue();
        try {
            return Short.valueOf(String.valueOf(v));
        } catch (NumberFormatException e) {
            throw new com.hims.adt.application.exceptions.InvalidAdtRequestException(
                    "ADT_REQ_INVALID", key + " must be a small integer");
        }
    }

    static Boolean boolOf(Map<String, Object> body, String key) {
        return WardController.boolOf(body, key);
    }
}
