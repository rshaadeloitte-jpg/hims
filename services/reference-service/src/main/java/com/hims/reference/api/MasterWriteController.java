package com.hims.reference.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.hims.reference.application.MasterWriteUseCase;
import com.hims.reference.domain.model.MasterEntityBase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Master WRITE endpoints (MIG-010, Wave 1). Legacy parity targets:
 * CrudOperationsFilter POST/{entity} (crudadd), POST/{entity}/{id}/__cufl or
 * PUT/{entity}/{id} (crudupdate with nonNull/updFlds), DELETE/{entity}/{id} (crudremove).
 * <p>
 * Every write emits a transactional {@code MasterDataChanged} outbox event.
 * Unit scope: non-Unitmaster entities require {@code unitId} (legacy derived it from
 * the session token; MIG-004 IAM will supply it from the principal — IMP-DEC-006/020).
 */
@RestController
@RequestMapping("/api/v1/masters")
@Tag(name = "Master Write (MIG-010)", description = "Master entity write endpoints (crudadd/crudupdate/crudremove parity + transactional outbox)")
public class MasterWriteController {

    private final MasterWriteUseCase useCase;

    public MasterWriteController(MasterWriteUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping("/{entity}")
    @Operation(summary = "crudadd parity: create a master record (id from per-table sequence unless supplied)")
    public ResponseEntity<MasterEntityBase> create(
            @PathVariable String entity,
            @RequestBody(required = false) JsonNode body,
            @RequestParam(required = false) Long unitId) {
        MasterEntityBase saved = useCase.create(entity, body, unitId);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{entity}/{id}")
    @Operation(summary = "crudupdate parity: partial/full update with MBase.update field-copy semantics",
            description = "nonNull=true (default): null fields are left untouched; nonNull=false replicates legacy full-overwrite. "
                    + "updFlds (comma-separated) restricts the copied fields. id/unitId are never overwritten.")
    public MasterEntityBase update(
            @PathVariable String entity,
            @PathVariable long id,
            @RequestBody(required = false) JsonNode body,
            @RequestParam(required = false) Long unitId,
            @RequestParam(defaultValue = "true") boolean nonNull,
            @RequestParam(required = false) String updFlds) {
        Set<String> fields = parseUpdFlds(updFlds);
        return useCase.update(entity, id, body, nonNull, fields, unitId);
    }

    @DeleteMapping("/{entity}/{id}")
    @Operation(summary = "crudremove parity: delete a master record (emits MasterDataChanged op=remove)")
    public MasterEntityBase remove(
            @PathVariable String entity,
            @PathVariable long id,
            @RequestParam(required = false) Long unitId) {
        return useCase.remove(entity, id, unitId);
    }

    private Set<String> parseUpdFlds(String updFlds) {
        if (updFlds == null || updFlds.isBlank()) {
            return null;
        }
        return new LinkedHashSet<>(Arrays.asList(updFlds.split(",")));
    }
}
