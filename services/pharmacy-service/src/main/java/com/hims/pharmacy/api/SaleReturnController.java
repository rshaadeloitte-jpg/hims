package com.hims.pharmacy.api;

import com.hims.pharmacy.application.SaleReturnService;
import com.hims.pharmacy.domain.model.SaleReturn;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Pharmacy sale-return API (M07 gap; legacy PharmacySaleReturnBill parity).
 * Initiate -> Complete (stock-ADD) | Reject.
 */
@RestController
@RequestMapping("/api/v1/sale-returns")
@Tag(name = "Sale Returns (M07 gap)", description = "Pharmacy sale returns. Status: 1=INITIATED 2=APPROVED 3=COMPLETED 4=REJECTED. BR-PHAR-003: COMPLETE POSTs an ADD movement to item-service (single writer D-1); on failure the whole tx rolls back. Stock NOT touched on REJECTED.")
public class SaleReturnController {

    private final SaleReturnService service;

    public SaleReturnController(SaleReturnService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Initiate a return for a previous sale",
            description = "Body: originalSaleId, patientId, returnReason (DAMAGED|WRONG_ITEM|PATIENT_REFUSED|EXPIRED|OTHER), remarks?, lines[{itemId,batchId?,returnQty,unitPrice?,lineReason?}]. Status starts INITIATED.")
    public ResponseEntity<SaleReturn> initiate(@RequestBody InitiateRequest body) {
        List<SaleReturnService.LineInput> lines = body.lines() == null ? null : body.lines().stream()
                .map(l -> new SaleReturnService.LineInput(
                        lng(l.get("itemId")),
                        str(l.get("batchId")),
                        intVal(l.get("returnQty")),
                        l.get("unitPrice") == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(l.get("unitPrice"))),
                        str(l.get("lineReason"))))
                .toList();
        SaleReturn ret = service.initiate(body.originalSaleId(), body.patientId(),
                body.returnReason(), body.remarks(), lines);
        return ResponseEntity.status(HttpStatus.CREATED).body(ret);
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Approve and complete a return (stock ADD)",
            description = "INITIATED -> COMPLETED; posts an ADD movement per line to item-service. On any stock failure the whole tx rolls back. approvedBy optional.")
    public SaleReturn complete(@PathVariable long id,
                                @RequestParam(required = false) Long approvedBy,
                                @RequestParam(required = false) String remarks) {
        return service.complete(id, approvedBy, remarks);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a return (no stock movement)",
            description = "INITIATED -> REJECTED (terminal). No stock changes; remarks appended.")
    public SaleReturn reject(@PathVariable long id, @RequestParam(required = false) String remarks) {
        return service.reject(id, remarks);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a return with its lines")
    public SaleReturn get(@PathVariable long id) {
        return service.get(id);
    }

    @GetMapping
    @Operation(summary = "List returns; filter ?status=1..4 and/or ?patientId=")
    public List<SaleReturn> list(@RequestParam(required = false) Short status,
                                  @RequestParam(required = false) Long patientId) {
        return service.list(status, patientId);
    }

    private static String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }
    private static Long lng(Object v) {
        return v == null ? null : Long.valueOf(String.valueOf(v));
    }
    private static Integer intVal(Object v) {
        return v == null ? null : Integer.valueOf(String.valueOf(v));
    }

    public record InitiateRequest(Long originalSaleId, Long patientId, String returnReason,
                                  String remarks, List<Map<String, Object>> lines) {}
}
