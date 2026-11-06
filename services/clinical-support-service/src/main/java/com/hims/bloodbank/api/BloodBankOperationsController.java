package com.hims.bloodbank.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.hims.bloodbank.application.BloodBankOperationsService;
import com.hims.bloodbank.domain.model.BloodBagDiscard;
import com.hims.bloodbank.domain.model.BloodBagReturn;
import com.hims.bloodbank.domain.model.BloodBagTrail;
import com.hims.bloodbank.domain.model.BloodBankCrossUnit;
import com.hims.bloodbank.domain.model.BloodBankIndent;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/bloodbank")
public class BloodBankOperationsController {
    private final BloodBankOperationsService service;
    public BloodBankOperationsController(BloodBankOperationsService service) { this.service = service; }

    // --------------------------------------------------------------- trail
    @PostMapping("/bags/{bagId}/trail")
    public ResponseEntity<BloodBagTrail> recordTrail(@PathVariable long bagId, @RequestBody JsonNode body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.recordTrail(
                bagId, shortOrThrow(body, "eventType"), longOrNull(body, "actorId"),
                textOrNull(body, "refDocType"), longOrNull(body, "refDocId"),
                textOrNull(body, "remarks")));
    }
    @GetMapping("/bags/{bagId}/trail")
    public List<BloodBagTrail> trail(@PathVariable long bagId) { return service.trail(bagId); }

    // --------------------------------------------------------------- return
    @PostMapping("/bag-returns")
    public ResponseEntity<BloodBagReturn> raiseReturn(@RequestBody JsonNode body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.raiseReturn(
                longOrThrow(body, "bagId"), textOrNull(body, "reason"), textOrNull(body, "returnedTo")));
    }
    @PostMapping("/bag-returns/{id}/complete")
    public BloodBagReturn completeReturn(@PathVariable long id) { return service.completeReturn(id, null); }
    @PostMapping("/bag-returns/{id}/reject")
    public BloodBagReturn rejectReturn(@PathVariable long id, @RequestBody(required = false) JsonNode body) {
        return service.rejectReturn(id, body == null ? null : textOrNull(body, "remarks"));
    }
    @GetMapping("/bag-returns")
    public List<BloodBagReturn> listReturns(@RequestParam(required = false) Long bagId,
                                            @RequestParam(required = false) Short status) {
        return service.listReturns(bagId, status);
    }
    @GetMapping("/bag-returns/{id}")
    public BloodBagReturn getReturn(@PathVariable long id) { return service.getReturn(id); }

    // --------------------------------------------------------------- discard
    @PostMapping("/bag-discards")
    public ResponseEntity<BloodBagDiscard> raiseDiscard(@RequestBody JsonNode body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.raiseDiscard(
                longOrThrow(body, "bagId"), textOrNull(body, "reason")));
    }
    @PostMapping("/bag-discards/{id}/complete")
    public BloodBagDiscard completeDiscard(@PathVariable long id) { return service.completeDiscard(id, null); }
    @GetMapping("/bag-discards")
    public List<BloodBagDiscard> listDiscards(@RequestParam(required = false) Long bagId,
                                              @RequestParam(required = false) Short status) {
        return service.listDiscards(bagId, status);
    }
    @GetMapping("/bag-discards/{id}")
    public BloodBagDiscard getDiscard(@PathVariable long id) { return service.getDiscard(id); }

    // --------------------------------------------------------------- indent
    @PostMapping("/indents")
    public ResponseEntity<BloodBankIndent> raiseIndent(@RequestBody JsonNode body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.raiseIndent(
                longOrThrow(body, "fromStoreId"), longOrThrow(body, "toStoreId"),
                textOrNull(body, "bloodGroup"), textOrNull(body, "component"),
                intOrThrow(body, "quantity"), textOrNull(body, "requestedDate"),
                textOrNull(body, "remarks")));
    }
    @PostMapping("/indents/{id}/approve")
    public BloodBankIndent approveIndent(@PathVariable long id) { return service.approveIndent(id, null); }
    @PostMapping("/indents/{id}/issue")
    public BloodBankIndent issueIndent(@PathVariable long id) { return service.issueIndent(id); }
    @PostMapping("/indents/{id}/cancel")
    public BloodBankIndent cancelIndent(@PathVariable long id) { return service.cancelIndent(id); }
    @GetMapping("/indents")
    public List<BloodBankIndent> listIndents(@RequestParam(required = false) Long fromStoreId,
                                            @RequestParam(required = false) Long toStoreId,
                                            @RequestParam(required = false) Short status) {
        return service.listIndents(fromStoreId, toStoreId, status);
    }
    @GetMapping("/indents/{id}")
    public BloodBankIndent getIndent(@PathVariable long id) { return service.getIndent(id); }

    // --------------------------------------------------------------- cross-unit
    @PostMapping("/cross-unit-orders")
    public ResponseEntity<BloodBankCrossUnit> raiseCrossUnit(@RequestBody JsonNode body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.raiseCrossUnit(
                longOrThrow(body, "fromBloodbankId"), longOrThrow(body, "toBloodbankId"),
                textOrNull(body, "bloodGroup"), textOrNull(body, "component"),
                intOrThrow(body, "quantity"), textOrNull(body, "remarks")));
    }
    @PostMapping("/cross-unit-orders/{id}/acknowledge")
    public BloodBankCrossUnit acknowledge(@PathVariable long id) { return service.acknowledgeCrossUnit(id, null); }
    @PostMapping("/cross-unit-orders/{id}/receive")
    public BloodBankCrossUnit receive(@PathVariable long id) { return service.receiveCrossUnit(id, null); }
    @PostMapping("/cross-unit-orders/{id}/cancel")
    public BloodBankCrossUnit cancelCrossUnit(@PathVariable long id) { return service.cancelCrossUnit(id); }
    @GetMapping("/cross-unit-orders")
    public List<BloodBankCrossUnit> listCrossUnits(
            @RequestParam(required = false) Long fromBloodbankId,
            @RequestParam(required = false) Long toBloodbankId,
            @RequestParam(required = false) Short status) {
        return service.listCrossUnits(fromBloodbankId, toBloodbankId, status);
    }
    @GetMapping("/cross-unit-orders/{id}")
    public BloodBankCrossUnit getCrossUnit(@PathVariable long id) { return service.getCrossUnit(id); }

    private static String textOrNull(JsonNode n, String f) { JsonNode v = n.path(f); return v.isMissingNode() || v.isNull() ? null : v.asText(); }
    private static Long longOrNull(JsonNode n, String f) { JsonNode v = n.path(f); return v.isMissingNode() || v.isNull() || v.asLong(0) == 0 ? null : v.asLong(); }
    private static long longOrThrow(JsonNode n, String f) { JsonNode v = n.path(f); if (v.isMissingNode() || v.isNull() || v.asLong(0) == 0) throw new com.hims.bloodbank.application.exceptions.InvalidBloodBankRequestException("BEXA003", f + " is required"); return v.asLong(); }
    private static int intOrThrow(JsonNode n, String f) { JsonNode v = n.path(f); if (v.isMissingNode() || v.isNull()) throw new com.hims.bloodbank.application.exceptions.InvalidBloodBankRequestException("BEXA003", f + " is required"); return v.asInt(); }
    private static short shortOrThrow(JsonNode n, String f) { JsonNode v = n.path(f); if (v.isMissingNode() || v.isNull()) throw new com.hims.bloodbank.application.exceptions.InvalidBloodBankRequestException("BEXA003", f + " is required"); return (short) v.asInt(); }
}
