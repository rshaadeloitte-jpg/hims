package com.hims.adt.api;

import com.hims.adt.application.MasterDataService;
import com.hims.adt.domain.model.Ward;
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

/** Ward master CRUD (GAP-REGISTER WP-5): POST/GET/PUT /api/v1/wards. */
@RestController
@RequestMapping("/api/v1/wards")
@Tag(name = "Wards (WP-5)", description = "Ward master (m_ward) — ownership moved to the ADT service per Catalog #3")
public class WardController {

    private final MasterDataService service;

    public WardController(MasterDataService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Create a ward")
    public ResponseEntity<Ward> create(@RequestBody Map<String, Object> body) {
        Boolean censusFlag = boolOf(body, "iscensus");
        if (censusFlag == null) {
            censusFlag = boolOf(body, "censusFlag");
        }
        Ward ward = service.createWard(longOf(body, "unitId"), strOf(body, "name"),
                strOf(body, "wardType"), censusFlag);
        return ResponseEntity.status(HttpStatus.CREATED).body(ward);
    }

    @GetMapping
    @Operation(summary = "List wards", description = "?unitId= filters by unit.")
    public List<Ward> list(@RequestParam(required = false) Long unitId) {
        return service.listWards(unitId);
    }

    @GetMapping("/{id}")
    public Ward get(@PathVariable long id) {
        return service.getWard(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a ward")
    public Ward update(@PathVariable long id, @RequestBody Map<String, Object> body) {
        return service.updateWard(id, strOf(body, "name"), strOf(body, "wardType"),
                boolOf(body, "iscensus") != null ? boolOf(body, "iscensus") : boolOf(body, "censusFlag"));
    }

    static Long longOf(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try {
            return Long.valueOf(String.valueOf(v));
        } catch (NumberFormatException e) {
            throw new com.hims.adt.application.exceptions.InvalidAdtRequestException(
                    "ADT_REQ_INVALID", key + " must be numeric");
        }
    }

    static String strOf(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v == null ? null : String.valueOf(v);
    }

    static Boolean boolOf(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null) return null;
        if (v instanceof Boolean b) return b;
        return Boolean.valueOf(String.valueOf(v));
    }
}
