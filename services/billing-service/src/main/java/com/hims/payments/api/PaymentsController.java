package com.hims.payments.api;

import com.hims.payments.application.AdvanceService;
import com.hims.payments.application.PaymentService;
import com.hims.payments.application.RefundService;
import com.hims.payments.domain.model.Advance;
import com.hims.payments.domain.model.Payment;
import com.hims.payments.domain.model.Refund;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Payments & Refunds API (GAP-REGISTER WP-13): advances (generated balance),
 * payment capture with BR-BILL-007 advance FIFO + synchronous billing
 * settlement, refunds TransactionType 3/4.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Payments & Refunds (WP-13)", description = "Catalog #6: advances with generated balance column, payments with mode details (mode 7 = ADVANCE reserved), BR-BILL-007 advance FIFO consumption, synchronous settlement to billing-service (BR-BILL-006), refunds TransactionType 3/4; E-08 PaymentReceived + E-12 RefundIssued PRELIMINARY via transactional outbox")
public class PaymentsController {

    private final AdvanceService advanceService;
    private final PaymentService paymentService;
    private final RefundService refundService;

    public PaymentsController(AdvanceService advanceService,
                              PaymentService paymentService,
                              RefundService refundService) {
        this.advanceService = advanceService;
        this.paymentService = paymentService;
        this.refundService = refundService;
    }

    // ------------------------------------------------------------------ advances

    @PostMapping("/advances")
    @Operation(summary = "Create an advance",
            description = "patientId required; amount > 0. balance is GENERATED ALWAYS AS (amount - used) STORED.")
    public ResponseEntity<Advance> createAdvance(@RequestBody CreateAdvanceRequest body) {
        Advance created = advanceService.create(body.patientId(), body.amount());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/advances")
    @Operation(summary = "Advances of a patient", description = "?patientId= required; ordered by advance_id (BR-BILL-007 FIFO order).")
    public List<Advance> advances(@RequestParam(required = false) Long patientId) {
        return advanceService.byPatient(patientId);
    }

    // ------------------------------------------------------------------ payments

    @PostMapping("/payments")
    @Operation(summary = "Capture a payment for a bill",
            description = "modes must sum to amount unless advanceUse fills the gap from patient advances "
                    + "(BR-BILL-007 FIFO in id order against the self balance first; partial splits recorded as extra "
                    + "mode-7 rows + t_advance_consumption). Calls billing PUT /api/v1/internal/bills/{id}/settlement "
                    + "synchronously; E-08 PaymentReceived is outboxed only after a successful settlement.")
    public ResponseEntity<Payment> pay(@RequestBody CreatePaymentRequest body) {
        List<PaymentService.ModeLine> modes = body.modes() == null ? null : body.modes().stream()
                .map(m -> new PaymentService.ModeLine(intVal(m.get("mode")), dec(m.get("amount"))))
                .toList();
        Payment created = paymentService.pay(body.billId(), body.patientId(), body.amount(),
                modes, Boolean.TRUE.equals(body.advanceUse()));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/payments")
    @Operation(summary = "Payments of a bill", description = "?billId= required; each payment carries its modes and advance consumption rows.")
    public List<Payment> payments(@RequestParam(required = false) Long billId) {
        return paymentService.byBill(billId);
    }

    // ------------------------------------------------------------------ refunds

    @PostMapping("/refunds")
    @Operation(summary = "Record a refund",
            description = "transactionType 3 = advance refund (requires advanceId; balance must be > 0 else legacy code BEXAR002); "
                    + "4 = bill refund (requires billId). E-12 RefundIssued PRELIMINARY is emitted; restoredCharges stays [] "
                    + "until billing-side compensation lands.")
    public ResponseEntity<Refund> refund(@RequestBody CreateRefundRequest body) {
        Refund created = refundService.create(longVal(body.transactionType()), body.patientId(),
                body.billId(), body.advanceId(), body.amount(), body.reason());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/refunds")
    @Operation(summary = "Refunds of a bill", description = "?billId= required.")
    public List<Refund> refunds(@RequestParam(required = false) Long billId) {
        return refundService.byBill(billId);
    }

    // ------------------------------------------------------------------ helpers

    private static Integer intVal(Object v) {
        return v == null ? null : Integer.valueOf(String.valueOf(v));
    }

    private static Long longVal(Integer v) {
        return v == null ? null : v.longValue();
    }

    private static BigDecimal dec(Object v) {
        return v == null ? null : new BigDecimal(String.valueOf(v));
    }

    public record CreateAdvanceRequest(Long patientId, BigDecimal amount) {}

    public record CreatePaymentRequest(Long billId, Long patientId, BigDecimal amount,
                                       Boolean advanceUse, List<Map<String, Object>> modes) {}

    public record CreateRefundRequest(Integer transactionType, Long patientId, Long billId,
                                      Long advanceId, BigDecimal amount, String reason) {}
}
