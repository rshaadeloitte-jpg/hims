package com.hims.cssd.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.hims.cssd.application.CssdKitService;
import com.hims.cssd.application.exceptions.InvalidKitRequestException;
import com.hims.cssd.domain.model.Kit;
import com.hims.cssd.domain.model.KitIssue;
import com.hims.cssd.domain.model.SterilizationCycle;
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

/**
 * CSSD kit API (GAP-REGISTER-5 WP-R3, Catalog #15): assemble, cycle
 * (start/complete PASS|FAIL), issue (STERILE-only, single active issue), queries.
 * Emits NOTHING to the outbox — KitIssued / SterilizationCompleted are REJECTED
 * phase 1 per KAFKA-EVENT-CATALOG.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "CSSD Kits (WP-R3)", description = "CSSD (Catalog #15): kit lifecycle 1=ASSEMBLED 2=STERILIZING 3=STERILE 4=ISSUED 5=REJECTED — cycle from ASSEMBLED/STERILE only; PASS->STERILE, FAIL->REJECTED (unusable); issue STERILE-only, single active issue. No events in phase 1.")
public class KitController {

    private final CssdKitService service;

    public KitController(CssdKitService service) {
        this.service = service;
    }

    @PostMapping("/kits")
    @Operation(summary = "Assemble a kit",
            description = "kitCode required + unique; >=1 item {itemId,qty} with qty > 0; "
                    + "expiryDays optional (>0, default 14). New kit status ASSEMBLED.")
    public ResponseEntity<Kit> create(@RequestBody CreateKitRequest body) {
        List<CssdKitService.ItemInput> items = body.items() == null ? null : body.items().stream()
                .map(i -> new CssdKitService.ItemInput(
                        lng(i.get("itemId")), intVal(i.get("qty"))))
                .toList();
        Kit created = service.create(body.kitCode(), body.procedureType(), items, body.expiryDays());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/kits/{id}")
    @Operation(summary = "Full kit with contents")
    public Kit get(@PathVariable long id) {
        return service.get(id);
    }

    @GetMapping("/kits")
    @Operation(summary = "List kits", description = "?status= (1..5 or name) and/or ?procedureType= filter.")
    public List<Kit> list(@RequestParam(required = false) String status,
                          @RequestParam(required = false) String procedureType) {
        return service.list(status == null ? null : CssdKitService.parseStatus(status), procedureType);
    }

    @PostMapping("/kits/{id}/cycles")
    @Operation(summary = "Start a sterilization cycle",
            description = "Guard: only from ASSEMBLED or STERILE (reprocess); method required. "
                    + "Moves kit to STERILIZING. Emits nothing.")
    public SterilizationCycle startCycle(@PathVariable long id,
                                         @RequestBody StartCycleRequest body) {
        return service.startCycle(id, body.method());
    }

    @PutMapping("/cycles/{id}/complete")
    @Operation(summary = "Complete a sterilization cycle",
            description = "{result: PASS|FAIL}; once-only. PASS -> kit STERILE (+sterilized_at); "
                    + "FAIL -> kit REJECTED (unusable). Emits nothing.")
    public SterilizationCycle completeCycle(@PathVariable long id,
                                            @RequestBody CompleteCycleRequest body) {
        return service.completeCycle(id, body.result());
    }

    @PostMapping("/kits/{id}/issues")
    @Operation(summary = "Issue a kit to an OT schedule / procedure ref",
            description = "Guard: STERILE only (REJECTED kits unusable); issuedToRef required; "
                    + "single active issue per kit (issue flips kit to ISSUED). Emits nothing.")
    public ResponseEntity<KitIssue> issue(@PathVariable long id,
                                          @RequestBody CreateIssueRequest body) {
        KitIssue issue = service.issue(id, body.issuedToRef());
        return ResponseEntity.status(HttpStatus.CREATED).body(issue);
    }

    @GetMapping("/kits/{id}/issues")
    @Operation(summary = "Issue history of a kit")
    public List<KitIssue> issues(@PathVariable long id) {
        return service.issues(id);
    }

    // ------------------------------------------------------------------ helpers

    private static Long lng(Object v) {
        return v == null ? null : Long.valueOf(String.valueOf(v));
    }

    private static Integer intVal(Object v) {
        return v == null ? null : Integer.valueOf(String.valueOf(v));
    }

    public record CreateKitRequest(String kitCode, String procedureType,
                                   List<Map<String, Object>> items, Integer expiryDays) {}

    public record StartCycleRequest(String method) {}

    public record CompleteCycleRequest(String result) {}

    public record CreateIssueRequest(String issuedToRef) {}

    @PostMapping("/issues/{id}/dispatch")
    public KitIssue dispatchIssue(@PathVariable long id, @RequestBody(required = false) JsonNode body) {
        Long to = body == null ? null : (body.path("toStoreId").isMissingNode() || body.path("toStoreId").isNull() || body.path("toStoreId").asLong(0) == 0 ? null : body.path("toStoreId").asLong());
        return service.dispatchIssue(id, to);
    }

    @PostMapping("/issues/{id}/receive")
    public KitIssue receiveIssue(@PathVariable long id, @RequestBody(required = false) JsonNode body) {
        return service.receiveIssue(id, body == null ? null : body.path("receivedBy").asText());
    }

    @PostMapping("/cycles/{id}/quality-complete")
    public SterilizationCycle qualityComplete(@PathVariable long id, @RequestBody JsonNode body) {
        short result = ((short) body.path("result").asInt(1) == SterilizationCycle.RESULT_FAIL) ? SterilizationCycle.RESULT_FAIL : SterilizationCycle.RESULT_PASS;
        return service.completeCycleWithQualityTest(id, result, body.path("biologicalIndicator").asText(), body.path("chemicalIndicator").asText());
    }
}
