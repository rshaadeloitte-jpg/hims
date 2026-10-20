package com.hims.billing.api;

import com.hims.billing.application.PaymentService;
import com.hims.billing.domain.model.AdvanceLedger;
import com.hims.billing.domain.model.Bill;
import com.hims.billing.domain.model.Receipt;
import com.hims.billing.domain.model.Refund;
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
import java.util.Map;

/**
 * Payments &amp; Refunds API (MIG-041 catalog #6).
 * <p>
 * Endpoints (all /api/v1):
 *   POST /patients/{id}/advances         apply an advance (RECEIPT)
 *   GET  /patients/{id}/advances/balance live running balance
 *   POST /bills/{id}/payments/advance-adjust  FIFO advance consumption (BR-BILL-007)
 *   POST /bills/{id}/payments            record a receipt (E-08 PaymentReceived)
 *   POST /bills/{id}/refunds             issue advance refund (tx=3) or bill-cancel refund (tx=4)
 *   GET  /bills/{id}/receipts            receipt history
 *   GET  /bills/{id}/refunds             refund history
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Payments (MIG-041)", description = "Catalog #6: advances (BR-BILL-007 FIFO) + receipts (BR-BILL-006) + refunds (BR-BILL-008); E-08 PaymentReceived on every receipt")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @PostMapping("/patients/{id}/advances")
    @Operation(summary = "Apply an advance (RECEIPT entry, balance +)")
    public ResponseEntity<AdvanceBalanceResponse> applyAdvance(@PathVariable long id,
                                                              @RequestBody ApplyAdvanceRequest body) {
        BigDecimal balance = service.applyAdvance(id, dec(body.amount()), body.modeCode(),
                body.receiptNo(), body.remarks());
        return ResponseEntity.status(HttpStatus.CREATED).body(new AdvanceBalanceResponse(id, balance));
    }

    @GetMapping("/patients/{id}/advances/balance")
    @Operation(summary = "Live advance balance for a patient")
    public AdvanceBalanceResponse balance(@PathVariable long id) {
        return new AdvanceBalanceResponse(id, service.getAdvanceBalance(id));
    }

    @PostMapping("/bills/{id}/payments/advance-adjust")
    @Operation(summary = "Apply BR-BILL-007 FIFO advance consumption against a bill",
            description = "Consumes unconsumed advance RECEIPT rows for the patient in advance_id ASC order against the bill's balance_self.")
    public Map<String, Object> applyAdvanceAdjustment(@PathVariable long id) {
        BigDecimal consumed = service.consumeAdvanceFifo(id);
        return Map.of("billId", id, "advanceConsumed", consumed);
    }

    @PostMapping("/bills/{id}/payments")
    @Operation(summary = "Record a payment receipt (BR-BILL-006) and emit E-08 PaymentReceived",
            description = "selfAmount + nonSelfAmount are the amounts in THIS receipt. Settlement is recomputed from the cumulative receipts (idempotent replay-safe).")
    public Bill recordPayment(@PathVariable long id, @RequestBody RecordPaymentRequest body) {
        return service.recordPayment(id, dec(body.selfAmount()), dec(body.nonSelfAmount()),
                body.modeCode(), body.bankId(), body.bankRefNo(),
                Boolean.TRUE.equals(body.applyAdvance()));
    }

    @PostMapping("/bills/{id}/refunds")
    @Operation(summary = "Issue a refund (BR-BILL-008): transactionType=3 advance refund or 4 bill-cancel refund")
    public Bill refund(@PathVariable long id, @RequestBody RefundRequest body) {
        Short tx = body.transactionType() == null ? null
                : Short.valueOf(String.valueOf(body.transactionType()));
        return service.issueRefund(id, dec(body.amount()), body.modeCode(), body.reason(), tx);
    }

    @GetMapping("/bills/{id}/receipts")
    @Operation(summary = "Receipt history for a bill (BR-BILL-006)")
    public List<Receipt> receipts(@PathVariable long id) {
        return service.getReceipts(id);
    }

    @GetMapping("/bills/{id}/refunds")
    @Operation(summary = "Refund history for a bill (BR-BILL-008)")
    public List<Refund> refunds(@PathVariable long id) {
        return service.getRefunds(id);
    }

    // ------------------------------------------------------------------ helpers

    private static BigDecimal dec(Object v) {
        return v == null ? null : new BigDecimal(String.valueOf(v));
    }

    public record ApplyAdvanceRequest(Object amount, String modeCode, String receiptNo, String remarks) {}
    public record AdvanceBalanceResponse(long patientId, BigDecimal balance) {}
    public record RecordPaymentRequest(Object selfAmount, Object nonSelfAmount, String modeCode,
                                       Long bankId, String bankRefNo, Boolean applyAdvance) {}
    public record RefundRequest(Object amount, String modeCode, String reason, Integer transactionType) {}
}
