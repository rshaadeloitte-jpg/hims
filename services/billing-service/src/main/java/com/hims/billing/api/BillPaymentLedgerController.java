package com.hims.billing.api;

import com.hims.billing.application.BillPaymentLedgerService;
import com.hims.billing.domain.model.BillPaymentLedger;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/bills/{billId}/payment-ledger")
@Tag(name = "Bill Payment Ledger (M04 partial payment)", description = "Running payment-state mirror for the billing UI. Source-of-truth lives in payments-service; this view is a read model for partial-payment visualization.")
public class BillPaymentLedgerController {

    private final BillPaymentLedgerService service;

    public BillPaymentLedgerController(BillPaymentLedgerService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Record a payment-state event (PAYMENT | REFUND | ADVANCE_USE | CANCEL)",
            description = "Called by payments-service / billing-service settlement hook. One row per recomputation.")
    public ResponseEntity<BillPaymentLedger> record(@PathVariable long billId, @RequestBody RecordRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.record(billId, body.eventType(), body.paidSelf(), body.paidNonSelf(),
                        body.sourceRef(), body.sourceService(), body.notes()));
    }

    @GetMapping
    @Operation(summary = "List payment-ledger entries for a bill (newest first)")
    public List<BillPaymentLedger> list(@PathVariable long billId) {
        return service.listForBill(billId);
    }

    @GetMapping("/status")
    @Operation(summary = "Partial-payment status summary (running balance, event count)",
            description = "Composite view for the billing UI's partial-payment status widget. Combines the latest ledger row with the bill header.")
    public BillPaymentLedgerService.PaymentStatus status(@PathVariable long billId) {
        return service.paymentStatus(billId);
    }

    public record RecordRequest(String eventType, BigDecimal paidSelf, BigDecimal paidNonSelf,
                                 String sourceRef, String sourceService, String notes) {}
}
