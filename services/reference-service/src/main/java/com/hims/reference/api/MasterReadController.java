package com.hims.reference.api;

import com.hims.reference.application.MasterEntityRegistry;
import com.hims.reference.application.MasterReadUseCase;
import com.hims.reference.domain.model.MasterEntityBase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Master read endpoints — contract parity target for the legacy generic CRUD filter
 * GET surface ({@code {context}/api/rest/<entity>/(all|{id})}, CrudOperationsFilter.java:495-565).
 * <p>
 * S0 scope: read path only (IMP-DEC-002). Write endpoints (crudadd/crudupdate/crudremove
 * with MBase.validate + REQUIRES_NEW batch + cache invalidation) are MIG-010/W1.
 * The gateway (MIG-005) maps legacy routes onto these paths.
 */
@RestController
@RequestMapping("/api/v1/masters")
@Tag(name = "Master Read (S0)", description = "Master entity read endpoints (crudget/crudgetAll/crudgetAllAll parity)")
public class MasterReadController {

    private final MasterReadUseCase useCase;
    private final MasterEntityRegistry registry;

    public MasterReadController(MasterReadUseCase useCase, MasterEntityRegistry registry) {
        this.useCase = useCase;
        this.registry = registry;
    }

    @GetMapping("/{entity}/all")
    @Operation(summary = "crudgetAllAll parity: unscoped list with filters, search, pagination",
            description = "Legacy GET {entity}/all (GenericServiceImpl.crudgetAllAll). JSON array of legacy-shaped objects (field names 1:1, NON_NULL).")
    public List<? extends MasterEntityBase> all(
            @PathVariable String entity,
            @RequestParam(required = false) Map<String, String> filters,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String searchFld,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer count) {
        return useCase.getAll(entity, filters, search, searchFld,
                page == null ? 0 : page, count == null ? 0 : count);
    }

    @GetMapping("/{entity}/{id}")
    @Operation(summary = "crudget parity: single record by id (unit-scoped; Unitmaster id-only)")
    public MasterEntityBase byId(
            @PathVariable String entity,
            @PathVariable Long id,
            @Parameter(description = "Unit scope (legacy derives from session token; IAM supplies it from MIG-004 onward)") 
            @RequestParam(required = false) Long unitId,
            @RequestParam(required = false) Map<String, String> filters) {
        return useCase.getById(entity, id, unitId, filters);
    }

    @GetMapping("/{entity}")
    @Operation(summary = "crudgetAll parity: unit-scoped list with filters, search, pagination")
    public List<? extends MasterEntityBase> scoped(
            @PathVariable String entity,
            @Parameter(description = "Unit scope (legacy derives from session token)") 
            @RequestParam(required = false) Long unitId,
            @RequestParam(required = false) Map<String, String> filters,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String searchFld,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer count) {
        return useCase.getScoped(entity, unitId, filters, search, searchFld,
                page == null ? 0 : page, count == null ? 0 : count);
    }

    @GetMapping
    @Operation(summary = "Supported master entities (new contract, not part of legacy surface)")
    public java.util.Set<String> entities() {
        return registry.names();
    }
}
