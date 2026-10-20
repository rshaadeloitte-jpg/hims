package com.hims.billing.api;

import com.hims.billing.application.BillingService;
import com.hims.billing.domain.model.Bill;
import com.hims.billing.domain.model.Charge;
import com.hims.billing.domain.model.CreditNote;
import com.hims.billing.domain.model.PackageSubscription;
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

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

/**
 * Billing API (GAP-REGISTER WP-11): temp charges, bill creation (placeholder billNo
 * per BR-BILL-001), finalize (E-07 InvoiceGenerated once via outbox), concession
 * pre-finalize, settlement (BR-BILL-006) and bill query.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Billing (WP-11)", description = "Catalog #5: charges -> bills -> finalize (BR-BILL-001/002/004/005) -> settlement (BR-BILL-006); E-07 InvoiceGenerated on finalize")
public class BillController {

    private final BillingService service;

    public BillController(BillingService service) {
        this.service = service;
    }

    @PostMapping("/charges")
    @Operation(summary = "Create a TEMP charge row (not yet on a bill)")
    public ResponseEntity<Charge> createCharge(@RequestBody CreateChargeRequest body) {
        Charge created = service.createCharge(new BillingService.NewCharge(
                body.serviceId(), body.serviceName(), dec(body.rate()), body.qty(),
                dec(body.taxPctA()), dec(body.taxPctB()), dec(body.taxPctC()),
                body.applyAfterConcession(), dec(body.coPayPct()),
                dec(body.concessionPlanPct()), dec(body.concessionBillPct()),
                dec(body.doctorSharePct()), shortOf(body.specialization()),
                body.isOtProcedure()));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/bills")
    @Operation(summary = "Create a bill header attaching existing charges",
            description = "BR-BILL-001: billNo placeholder 000000000000000 at creation; cashCounterId defaults to 1.")
    public ResponseEntity<Bill> createBill(@RequestBody CreateBillRequest body) {
        Bill created = service.createBill(body.patientId(), body.encounterId(), body.admissionId(),
                body.cashCounterId(), body.prescriberDoctorId(), body.isSelfCompany(),
                body.chargeIds());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/bills/{id}")
    @Operation(summary = "Full bill with attached charge lines")
    public Bill get(@PathVariable long id) {
        return service.get(id);
    }

    @PostMapping("/bills/{id}/finalize")
    @Operation(summary = "Finalize an interim bill",
            description = "BR-BILL-004 taxes + BR-BILL-005 asymmetric split + BR-BILL-002 doctor share; snapshots; final billNo OPDBILL/IPDBILL-<seq>; E-07 InvoiceGenerated emitted ONCE via outbox.")
    public Bill finalize(@PathVariable long id) {
        return service.finalizeBill(id);
    }

    @PostMapping("/bills/{id}/concession")
    @Operation(summary = "Apply plan/bill concession percentages to all charges",
            description = "Pre-finalize only — 400 BILL_ALREADY_FINALIZED after finalize.")
    public Bill applyConcession(@PathVariable long id, @RequestBody ConcessionRequest body) {
        return service.applyConcession(id, dec(body.concessionPlanPct()), dec(body.concessionBillPct()));
    }

    /**
     * Internal synchronous settlement called by payments-service (catalog #5 note).
     * Tagged 'internal' in openapi/billing-service.yaml. paidSelf/paidNonSelf are
     * CUMULATIVE paid amounts; balances are recomputed (idempotent replay-safe),
     * is_settled &lt;=&gt; both balances &lt;= 0 (BR-BILL-006).
     */
    @PutMapping("/internal/bills/{id}/settlement")
    @Operation(summary = "[internal] Recompute settlement state from cumulative payments",
            description = "BR-BILL-006: settled <=> balance_self <= 0 AND balance_non_self <= 0; idempotent by recomputation.")
    public Bill settle(@PathVariable long id, @RequestBody SettlementRequest body) {
        return service.settle(id, dec(body.paidSelf()), dec(body.paidNonSelf()));
    }

    @PostMapping("/bills/{id}/cancel-settled")
    @Operation(summary = "Cancel a SETTLED bill (BR-BILL-008)",
            description = "Restores charges from *BeforeFinalization snapshots, marks bill cancelled, "
                    + "unbills doctor shares. Called by payments-service refund flow (transactionType=4).")
    public Bill cancelSettled(@PathVariable long id, @RequestBody CancelSettledBillRequest body) {
        return service.cancelSettledBill(id, body.reason());
    }

    // --------------------------------------------------------------- credit notes

    @PostMapping("/bills/{id}/credit-notes")
    @Operation(summary = "Issue a credit note against a finalized bill (M04 gap)",
            description = "Credit note amount must not exceed remaining bill balance. Only for finalized, non-cancelled bills.")
    public ResponseEntity<CreditNote> issueCreditNote(@PathVariable long id,
                                                      @RequestBody CreditNoteRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.issueCreditNote(id, dec(body.amount()), body.reason()));
    }

    // singular alias per GAP-15 spec: POST /bills/{id}/credit-note
    @PostMapping("/bills/{id}/credit-note")
    @Operation(summary = "Issue a credit note (singular alias, M04 gap)")
    public ResponseEntity<CreditNote> issueCreditNoteSingular(@PathVariable long id,
                                                              @RequestBody CreditNoteRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.issueCreditNote(id, dec(body.amount()), body.reason()));
    }

    @GetMapping("/bills/{id}/credit-notes")
    @Operation(summary = "List credit notes for a bill")
    public List<CreditNote> creditNotes(@PathVariable long id) {
        return service.creditNotesForBill(id);
    }

    // --------------------------------------------------------------- estimation (M04 gap)

    @PostMapping("/bills/estimate")
    @Operation(summary = "Estimate bill totals for proposed items (M04 gap)",
            description = "Stateless — does not persist. Uses same BillingCalculator as finalize (BR-BILL-004/005).")
    public BillingService.EstimateResult estimatePost(@RequestBody EstimateRequest body) {
        return service.estimate(body.patientId(), body.isSelfCompany(),
                body.items() == null ? List.of() : body.items().stream().map(i ->
                        new BillingService.EstimateItem(i.serviceId(), i.serviceName(), dec(i.rate()), i.qty(),
                                dec(i.taxPctA()), dec(i.taxPctB()), dec(i.taxPctC()),
                                i.applyAfterConcession(), dec(i.coPayPct()),
                                dec(i.concessionPlanPct()), dec(i.concessionBillPct()),
                                dec(i.doctorSharePct()), shortOf(i.specialization()), i.isOtProcedure())
                ).toList());
    }

    // GET per spec: GET /bills/estimate — query-param shorthand (single-item for smoke checks)
    @GetMapping("/bills/estimate")
    @Operation(summary = "Estimate bill totals (GET alias, M04 gap)")
    public BillingService.EstimateResult estimateGet(@RequestParam Long patientId,
                                                     @RequestParam(required = false) Boolean isSelfCompany,
                                                     @RequestParam(required = false) Long serviceId,
                                                     @RequestParam(required = false) Object rate,
                                                     @RequestParam(required = false) Integer qty) {
        if (serviceId == null && rate == null) {
            // no items -> zero estimate (caller should use POST for itemized)
            return new BillingService.EstimateResult(patientId, isSelfCompany == null || isSelfCompany,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, List.of());
        }
        BillingService.EstimateItem single = new BillingService.EstimateItem(
                serviceId == null ? 900L : serviceId,
                "Estimated Service", dec(rate == null ? "100" : rate), qty == null ? 1 : qty,
                null, null, null, false, null, null, null, null, null, false);
        return service.estimate(patientId, isSelfCompany, List.of(single));
    }

    // --------------------------------------------------------------- sponsor change stub (M04 gap)

    @PutMapping("/bills/{id}/sponsor")
    @Operation(summary = "Change bill sponsor mid-bill (M04 gap stub)",
            description = "Ported from UpdatePatientSponsorNewDto. NULL/0 companyId = self-pay. Emits SponsorChanged.")
    public Bill changeSponsor(@PathVariable long id, @RequestBody SponsorChangeRequest body) {
        return service.changeSponsor(id, body.companyId(), body.planId());
    }

    // --------------------------------------------------------------- package subscription (M04 gap)

    @PostMapping("/bills/{id}/package-subscription")
    @Operation(summary = "Subscribe patient to a package tied to a bill (M04 gap)",
            description = "Ported from T_PATIENTPACKAGESUBSCRIPTION. Emits PackageSubscribed via outbox.")
    public ResponseEntity<PackageSubscription> subscribePackageForBill(@PathVariable long id,
                                                                      @RequestBody PackageSubscriptionRequest body) {
        PackageSubscription ps = service.subscribePackage(
                body.patientId(), body.packageId(), id, dec(body.coveredAmount()),
                body.fromDate() == null ? null : Timestamp.valueOf(body.fromDate()),
                body.toDate() == null ? null : Timestamp.valueOf(body.toDate()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ps);
    }

    @PostMapping("/package-subscriptions")
    @Operation(summary = "Subscribe patient to a package (no bill yet, M04 gap)")
    public ResponseEntity<PackageSubscription> subscribePackage(@RequestBody PackageSubscriptionRequest body) {
        PackageSubscription ps = service.subscribePackage(
                body.patientId(), body.packageId(), body.billId(), dec(body.coveredAmount()),
                body.fromDate() == null ? null : Timestamp.valueOf(body.fromDate()),
                body.toDate() == null ? null : Timestamp.valueOf(body.toDate()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ps);
    }

    @GetMapping("/package-subscriptions")
    @Operation(summary = "List package subscriptions by patient (M04 gap)")
    public List<PackageSubscription> packageSubscriptionsByPatient(@RequestParam Long patientId) {
        return service.packageSubscriptionsForPatient(patientId);
    }

    @GetMapping("/bills/{id}/package-subscriptions")
    @Operation(summary = "List package subscriptions for a bill")
    public List<PackageSubscription> packageSubscriptionsForBill(@PathVariable long id) {
        return service.packageSubscriptionsForBill(id);
    }

    // ------------------------------------------------------------------ helpers

    private static BigDecimal dec(Object v) {
        return v == null ? null : new BigDecimal(String.valueOf(v));
    }

    private static Short shortOf(Object v) {
        return v == null ? null : Short.valueOf(String.valueOf(v));
    }

    public record CreateChargeRequest(Long serviceId, String serviceName, Object rate, Integer qty,
                                      Object taxPctA, Object taxPctB, Object taxPctC,
                                      Boolean applyAfterConcession, Object coPayPct,
                                      Object concessionPlanPct, Object concessionBillPct,
                                      Object doctorSharePct, Object specialization,
                                      Boolean isOtProcedure) {}

    public record CreateBillRequest(Long patientId, Long encounterId, Long admissionId,
                                    Long cashCounterId, Long prescriberDoctorId,
                                    Boolean isSelfCompany, List<Long> chargeIds) {}

    public record ConcessionRequest(Object concessionPlanPct, Object concessionBillPct) {}

    public record SettlementRequest(Object paidSelf, Object paidNonSelf) {}

    public record CancelSettledBillRequest(String reason) {}

    public record CreditNoteRequest(Object amount, String reason) {}

    public record SponsorChangeRequest(Long companyId, Long planId) {}

    public record PackageSubscriptionRequest(Long patientId, Long packageId, Long billId,
                                            Object coveredAmount,
                                            String fromDate, String toDate) {}

    public record EstimateItemRequest(Long serviceId, String serviceName, Object rate, Integer qty,
                                      Object taxPctA, Object taxPctB, Object taxPctC,
                                      Boolean applyAfterConcession, Object coPayPct,
                                      Object concessionPlanPct, Object concessionBillPct,
                                      Object doctorSharePct, Object specialization,
                                      Boolean isOtProcedure) {}

    public record EstimateRequest(Long patientId, Boolean isSelfCompany, List<EstimateItemRequest> items) {}
}
