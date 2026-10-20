package com.hims.ot.api;

import com.hims.ot.application.OtBillingEstimationService;
import com.hims.ot.domain.model.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/ot/billing")
public class OtBillingEstimationController {

    private final OtBillingEstimationService service;

    public OtBillingEstimationController(OtBillingEstimationService service) {
        this.service = service;
    }

    @PostMapping("/estimates")
    public ResponseEntity<OtBillingEstimate> create(@RequestParam Long scheduleId, @RequestParam String procedureCode) {
        return ResponseEntity.ok(service.createEstimate(scheduleId, procedureCode));
    }

    @PostMapping("/estimates/{estimateId}/costs")
    public ResponseEntity<OtProcedureCost> addCost(@PathVariable Long estimateId,
                                                   @RequestParam String itemName, @RequestParam String itemType,
                                                   @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(service.addCost(estimateId, itemName, itemType, amount));
    }

    @PostMapping("/estimates/{estimateId}/finalize")
    public ResponseEntity<OtBillingEstimate> finalize(@PathVariable Long estimateId) {
        return ResponseEntity.ok(service.finalizeEstimate(estimateId));
    }
}
