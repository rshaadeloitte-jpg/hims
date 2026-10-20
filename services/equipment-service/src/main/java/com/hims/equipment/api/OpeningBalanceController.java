package com.hims.equipment.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.hims.equipment.application.OpeningBalanceService;
import com.hims.equipment.domain.model.EquipmentOpeningBalance;
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
 * Opening-balance API (MIG-023 Phase 6) — per-item store stock counts as of
 * go-live: add rows (single or bulk file), read the register and correct rows.
 */
@RestController
@RequestMapping("/api/v1/equipment")
@Tag(name = "Equipment Opening Balance (MIG-023)",
        description = "Per-item opening balances (stock take at go-live): quantity + rate/amount per store, "
                + "register listing and corrections")
public class OpeningBalanceController {

    private final OpeningBalanceService balanceService;

    public OpeningBalanceController(OpeningBalanceService balanceService) {
        this.balanceService = balanceService;
    }

    @PostMapping("/opening-balance")
    @Operation(summary = "Add an opening balance (object body) or bulk import (array body)",
            description = "Object body adds one row: {itemId (required, > 0), openingBalance? (>= 0, "
                    + "defaults to 0), rate?, amount? (NUMERIC(19,2), >= 0), accessionNo?, serialNo?, model?, "
                    + "storeId?, supplierId?, remarks?}. A JSON ARRAY body imports many rows atomically — one "
                    + "invalid row rolls back the whole file. Server assigns id from "
                    + "equipment_opening_balance_seq; unit comes from the unitId query parameter.")
    public ResponseEntity<Object> add(@RequestBody(required = false) JsonNode body,
                                      @RequestParam(required = false) Long unitId) {
        if (body != null && body.isArray()) {
            List<EquipmentOpeningBalance> imported = balanceService.importBulk(body, unitId);
            return ResponseEntity.status(HttpStatus.CREATED).body(imported);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(balanceService.add(body, unitId));
    }

    @GetMapping("/opening-balance")
    @Operation(summary = "List opening balances",
            description = "Active register rows, optionally scoped by unitId and/or itemId, grouped per item "
                    + "(itemId ASC then insertion order). Both filters optional — omit both for the full register.")
    public List<EquipmentOpeningBalance> list(@RequestParam(required = false) Long unitId,
                                              @RequestParam(required = false) Long itemId) {
        return balanceService.list(unitId, itemId);
    }

    @PutMapping("/opening-balance/{id}")
    @Operation(summary = "Update an opening balance",
            description = "Null-safe patch: only present keys change. id, itemId (the row's pairing key), "
                    + "unit and audit columns are server-owned and never patchable.")
    public EquipmentOpeningBalance update(@PathVariable long id,
                                          @RequestBody(required = false) JsonNode body) {
        return balanceService.update(id, body);
    }
}
