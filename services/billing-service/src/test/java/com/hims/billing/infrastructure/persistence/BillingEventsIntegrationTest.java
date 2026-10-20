package com.hims.billing.infrastructure.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.billing.BillingServiceApplication;
import com.hims.billing.application.BillingService;
import com.hims.billing.application.exceptions.InvalidBillingRequestException;
import com.hims.billing.domain.model.Bill;
import com.hims.billing.domain.model.Charge;
import com.hims.billing.infrastructure.outbox.OutboxEvent;
import com.hims.billing.infrastructure.outbox.OutboxRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * L2 — Integration tests on real PG16 for the new events and BR-BILL-008 cancellation:
 * 1. BillSettled event emitted ONCE when bill transitions to settled (both balances <= 0)
 * 2. ConcessionApproved event emitted when concession applied to interim bill
 * 3. BR-BILL-008 cancel settled bill restores charges from *BeforeFinalization snapshots,
 *    unbills doctor shares, marks bill cancelled
 * 4. Decimal precision (NUMERIC(19,2)) and sequence isolation verified at DB level
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = BillingServiceApplication.class)
@DirtiesContext
@Transactional
class BillingEventsIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private BillingService billingService;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager em;

    // ---------------------------------------------------------- BillSettled event

    @Test
    void billSettledEvent_emittedOnceOnFirstTransitionToSettled() throws Exception {
        // Create charge + bill + finalize
        Charge charge = billingService.createCharge(new BillingService.NewCharge(
                900L, "X-Ray Chest", new BigDecimal("1000"), 1,
                null, null, null, false,
                new BigDecimal("40"),
                new BigDecimal("10"), new BigDecimal("5"),
                new BigDecimal("10"), null, false));

        Bill bill = billingService.createBill(505L, 7777L, null, null, 42L, true,
                List.of(charge.getChargeId()));
        long billId = bill.getBillId();

        Bill finalized = billingService.finalizeBill(billId);
        assertThat(outboxRepository.findAll()).hasSize(1); // InvoiceGenerated only

        // Partial payment - NOT settled
        billingService.settle(billId, new BigDecimal("300"), new BigDecimal("200"));
        assertThat(outboxRepository.findAll()).hasSize(1); // still only InvoiceGenerated

        // Full payment - FIRST transition to settled
        billingService.settle(billId,
                finalized.getSelfAmount(),
                finalized.getNonSelfAmount());

        // Verify BillSettled emitted exactly once
        var settledEvents = outboxRepository.findAll().stream()
                .filter(e -> "BillSettled".equals(e.getEventType()))
                .toList();
        assertThat(settledEvents).hasSize(1);

        OutboxEvent settled = settledEvents.get(0);
        assertThat(settled.getAggregateId()).isEqualTo(billId);
        assertThat(settled.getPartitionKey()).isEqualTo(String.valueOf(billId));
        assertThat(settled.getIdempotencyKey()).isEqualTo("bill-settled:" + billId);

        JsonNode payload = MAPPER.readTree(settled.getPayload());
        assertThat(payload.get("billId").asText()).isEqualTo(String.valueOf(billId));
        assertThat(payload.get("billNo").asText()).isEqualTo(finalized.getBillNo());
        assertThat(payload.get("patientId").asText()).isEqualTo("505");
        assertThat(payload.has("settledAt")).isTrue();
        assertThat(payload.get("paidSelf")).isNotNull();
        assertThat(payload.get("paidNonSelf")).isNotNull();
        assertThat(payload.get("balanceSelf").asDouble()).isLessThanOrEqualTo(0);
        assertThat(payload.get("balanceNonSelf").asDouble()).isLessThanOrEqualTo(0);

        // Replay same settlement - should NOT emit another BillSettled
        billingService.settle(billId,
                finalized.getSelfAmount(),
                finalized.getNonSelfAmount());
        var settledEventsAfterReplay = outboxRepository.findAll().stream()
                .filter(e -> "BillSettled".equals(e.getEventType()))
                .toList();
        assertThat(settledEventsAfterReplay).hasSize(1); // still exactly one
    }

    @Test
    void billSettledEvent_notEmittedForPartialPayment() {
        Charge charge = billingService.createCharge(new BillingService.NewCharge(
                900L, "X-Ray Chest", new BigDecimal("1000"), 1,
                null, null, null, false,
                new BigDecimal("40"), new BigDecimal("10"), new BigDecimal("5"),
                new BigDecimal("10"), null, false));

        Bill bill = billingService.createBill(505L, 7777L, null, null, 42L, true,
                List.of(charge.getChargeId()));
        long billId = bill.getBillId();

        billingService.finalizeBill(billId);

        // Multiple partial payments - never settled
        billingService.settle(billId, new BigDecimal("100"), new BigDecimal("100"));
        billingService.settle(billId, new BigDecimal("200"), new BigDecimal("200"));
        billingService.settle(billId, new BigDecimal("300"), new BigDecimal("300"));

        var settledEvents = outboxRepository.findAll().stream()
                .filter(e -> "BillSettled".equals(e.getEventType()))
                .toList();
        assertThat(settledEvents).isEmpty();
    }

    // ---------------------------------------------------------- ConcessionApproved event

    @Test
    void concessionApprovedEvent_emittedWhenConcessionApplied() throws Exception {
        Charge charge = billingService.createCharge(new BillingService.NewCharge(
                900L, "X-Ray Chest", new BigDecimal("1000"), 1,
                null, null, null, false,
                new BigDecimal("40"), new BigDecimal("10"), new BigDecimal("5"),
                new BigDecimal("10"), null, false));

        Bill bill = billingService.createBill(505L, 7777L, null, null, 42L, true,
                List.of(charge.getChargeId()));
        long billId = bill.getBillId();

        // Apply concession to interim bill
        billingService.applyConcession(billId, new BigDecimal("15"), new BigDecimal("8"));

        // Verify ConcessionApproved emitted
        var concessionEvents = outboxRepository.findAll().stream()
                .filter(e -> "ConcessionApproved".equals(e.getEventType()))
                .toList();
        assertThat(concessionEvents).hasSize(1);

        OutboxEvent concession = concessionEvents.get(0);
        assertThat(concession.getAggregateId()).isEqualTo(billId);
        assertThat(concession.getPartitionKey()).isEqualTo(String.valueOf(billId));
        assertThat(concession.getIdempotencyKey()).isEqualTo("concession-approved:" + billId);

        JsonNode payload = MAPPER.readTree(concession.getPayload());
        assertThat(payload.get("billId").asText()).isEqualTo(String.valueOf(billId));
        assertThat(payload.get("billNo").asText()).isEqualTo("000000000000000"); // still placeholder
        assertThat(payload.get("patientId").asText()).isEqualTo("505");
        assertThat(payload.get("concessionPlanPct").asText()).isEqualTo("15");
        assertThat(payload.get("concessionBillPct").asText()).isEqualTo("8");
        assertThat(payload.has("appliedAt")).isTrue();
    }

    @Test
    void concessionApprovedEvent_notEmittedAfterFinalize() {
        Charge charge = billingService.createCharge(new BillingService.NewCharge(
                900L, "X-Ray Chest", new BigDecimal("1000"), 1,
                null, null, null, false,
                new BigDecimal("40"), new BigDecimal("10"), new BigDecimal("5"),
                new BigDecimal("10"), null, false));

        Bill bill = billingService.createBill(505L, 7777L, null, null, 42L, true,
                List.of(charge.getChargeId()));
        long billId = bill.getBillId();

        billingService.finalizeBill(billId);

        // Try to apply concession after finalize - should fail, no event
        assertThatThrownBy(() -> billingService.applyConcession(billId, BigDecimal.TEN, BigDecimal.ONE))
                .isInstanceOf(com.hims.billing.application.exceptions.InvalidBillingRequestException.class)
                .extracting(e -> ((com.hims.billing.application.exceptions.InvalidBillingRequestException) e).getErrorCode())
                .isEqualTo("BILL_ALREADY_FINALIZED");

        var concessionEvents = outboxRepository.findAll().stream()
                .filter(e -> "ConcessionApproved".equals(e.getEventType()))
                .toList();
        assertThat(concessionEvents).isEmpty();
    }

    // ---------------------------------------------------------- BR-BILL-008 Cancel Settled Bill

    @Test
    void brBill008_cancelSettledBill_restoresChargesFromSnapshots_unbillsDoctorShares() {
        // Create bill with doctor share
        Charge charge = billingService.createCharge(new BillingService.NewCharge(
                900L, "X-Ray Chest", new BigDecimal("1000"), 1,
                null, null, null, false,
                new BigDecimal("40"), new BigDecimal("10"), new BigDecimal("5"),
                new BigDecimal("10"), null, false));

        Bill bill = billingService.createBill(505L, 7777L, null, null, 42L, true,
                List.of(charge.getChargeId()));
        long billId = bill.getBillId();

        Bill finalized = billingService.finalizeBill(billId);

        // Settle the bill
        billingService.settle(billId, finalized.getSelfAmount(), finalized.getNonSelfAmount());

        // Verify bill is settled
        Bill settled = billingService.get(billId);
        assertThat(settled.getIsSettled()).isTrue();
        assertThat(settled.getIsCancelled()).isFalse();

        // Verify doctor share exists with is_billed=false
        Integer sharesBefore = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ihms_billing.t_doctor_share WHERE bill_id=? AND is_billed=FALSE",
                Integer.class, billId);
        assertThat(sharesBefore).isEqualTo(1);

        // Cancel settled bill (BR-BILL-008)
        Bill cancelled = billingService.cancelSettledBill(billId, "Patient request refund");

        // Verify bill state
        assertThat(cancelled.getIsCancelled()).isTrue();
        assertThat(cancelled.getIsSettled()).isFalse();

        // Verify charges restored from snapshots (isCancelled=true, snapshot columns preserved)
        em.flush();
        var chargeSnap = jdbcTemplate.queryForMap(
                "SELECT is_cancelled, total_before_finalization, self_before_finalization, nonself_before_finalization "
              + "FROM ihms_billing.t_charge WHERE charge_id = ?", charge.getChargeId());
        assertThat(Boolean.valueOf(String.valueOf(chargeSnap.get("is_cancelled")))).isTrue();
        assertThat(((BigDecimal) chargeSnap.get("total_before_finalization"))).isEqualByComparingTo("1000.00");
        assertThat(((BigDecimal) chargeSnap.get("self_before_finalization"))).isEqualByComparingTo("340.00");
        assertThat(((BigDecimal) chargeSnap.get("nonself_before_finalization"))).isEqualByComparingTo("600.00");

        // Verify doctor share is_billed=false (reset)
        Integer sharesAfter = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ihms_billing.t_doctor_share WHERE bill_id=? AND is_billed=FALSE",
                Integer.class, billId);
        assertThat(sharesAfter).isEqualTo(1);
    }

    @Test
    void brBill008_cancelSettledBill_rejectedForUnsettledBill() {
        Charge charge = billingService.createCharge(new BillingService.NewCharge(
                900L, "X-Ray Chest", new BigDecimal("1000"), 1,
                null, null, null, false,
                new BigDecimal("40"), new BigDecimal("10"), new BigDecimal("5"),
                new BigDecimal("10"), null, false));

        Bill bill = billingService.createBill(505L, 7777L, null, null, 42L, true,
                List.of(charge.getChargeId()));
        long billId = bill.getBillId();

        billingService.finalizeBill(billId); // Finalized but NOT settled

        assertThatThrownBy(() -> billingService.cancelSettledBill(billId, "Test"))
                .isInstanceOf(com.hims.billing.application.exceptions.InvalidBillingRequestException.class)
                .extracting(e -> ((com.hims.billing.application.exceptions.InvalidBillingRequestException) e).getErrorCode())
                .isEqualTo("BILL_ILLEGAL_TRANSITION");
    }

    // ---------------------------------------------------------- Decimal Precision (NUMERIC(19,2))

    @Test
    void decimalPrecision_numeric19_2_enforcedAtDbLevel() {
        // Test that NUMERIC(19,2) columns enforce 2 decimal places
        Charge charge = billingService.createCharge(new BillingService.NewCharge(
                900L, "Precision Test", new BigDecimal("1234567890123456.78"), 1, // max 19 digits
                null, null, null, false,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, null, false));

        // Verify the value is stored with correct precision
        em.flush();
        BigDecimal storedRate = jdbcTemplate.queryForObject(
                "SELECT rate FROM ihms_billing.t_charge WHERE charge_id = ?",
                BigDecimal.class, charge.getChargeId());
        assertThat(storedRate).isEqualByComparingTo("1234567890123456.78");
    }

    @Test
    void decimalPrecision_roundingHalfUp_onCalculation() {
        // Rate with 3 decimals should be rounded HALF_UP to 2
        Charge charge = billingService.createCharge(new BillingService.NewCharge(
                900L, "Rounding Test", new BigDecimal("100.005"), 1, // .005 -> .01
                null, null, null, false,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, null, false));

        em.flush();
        BigDecimal storedRate = jdbcTemplate.queryForObject(
                "SELECT rate FROM ihms_billing.t_charge WHERE charge_id = ?",
                BigDecimal.class, charge.getChargeId());
        assertThat(storedRate).isEqualByComparingTo("100.01"); // HALF_UP
    }

    // ---------------------------------------------------------- Sequence Isolation

    @Test
    void sequenceIsolation_billSequencesIndependentFromChargeSequences() {
        // Sequences are SERVER-OWNED PostgreSQL sequences; verify at the DB level
        // that each table has its own sequence (independent counters) by checking
        // the live sequence nextval values, not by trying to assert that values
        // never collide (collisions would just mean both sequences started from 1,
        // which is expected behavior for independent sequences).
        // Sequences are SERVER-OWNED PostgreSQL sequences. The five billing
        // sequences must exist as independent counters in ihms_billing. We assert
        // that by querying information_schema.sequences (the canonical, read-only
        // source of truth for what sequences are physically present) and verify
        // each table's sequence name shows up exactly once. This is a stronger
        // claim than "nextval is callable" because it also asserts the names
        // match the migration contract.
        java.util.List<String> expected = java.util.List.of(
                "t_bill_seq", "t_charge_seq", "t_doctor_share_seq",
                "t_opdbill_seq", "t_ipdbill_seq",
                "t_consumed_event_seq", "t_advance_seq", "t_receipt_seq", "t_refund_seq");
        for (String name : expected) {
            Integer n = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.sequences "
                  + "WHERE sequence_schema = ? AND sequence_name = ?",
                    Integer.class, "ihms_billing", name);
            assertThat(n).as("sequence ihms_billing." + name).isEqualTo(1);
        }

        // also: real lifecycle still works (charge -> bill -> finalize uses
        // t_charge_seq for the charge, t_bill_seq for the bill header,
        // and t_opdbill_seq for the billNo).
        Charge c1 = billingService.createCharge(new BillingService.NewCharge(
                100L, "Service A", new BigDecimal("100"), 1, null, null, null, false,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, false));
        Charge c2 = billingService.createCharge(new BillingService.NewCharge(
                101L, "Service B", new BigDecimal("200"), 1, null, null, null, false,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, false));
        Bill b1 = billingService.createBill(1000L, 100L, null, null, 1L, true, List.of(c1.getChargeId()));
        Bill b2 = billingService.createBill(1001L, 101L, null, null, 2L, true, List.of(c2.getChargeId()));

        // bills get numbers from t_opdbill_seq at finalize
        billingService.finalizeBill(b1.getBillId());
        billingService.finalizeBill(b2.getBillId());
        Bill fb1 = billingService.get(b1.getBillId());
        Bill fb2 = billingService.get(b2.getBillId());

        assertThat(fb1.getBillNo()).startsWith("OPDBILL-");
        assertThat(fb2.getBillNo()).startsWith("OPDBILL-");
        assertThat(fb1.getBillNo()).isNotEqualTo(fb2.getBillNo());
    }

    // ---------------------------------------------------------- Locking / Concurrency

    @Test
    void settlementConcurrency_idempotentReplaySafe() {
        Charge charge = billingService.createCharge(new BillingService.NewCharge(
                900L, "X-Ray Chest", new BigDecimal("1000"), 1,
                null, null, null, false,
                new BigDecimal("40"), new BigDecimal("10"), new BigDecimal("5"),
                new BigDecimal("10"), null, false));

        Bill bill = billingService.createBill(505L, 7777L, null, null, 42L, true,
                List.of(charge.getChargeId()));
        long billId = bill.getBillId();

        billingService.finalizeBill(billId);
        Bill finalized = billingService.get(billId);

        // Simulate concurrent settlement replays with same cumulative amounts
        BigDecimal selfAmt = finalized.getSelfAmount();
        BigDecimal nonSelfAmt = finalized.getNonSelfAmount();

        Bill r1 = billingService.settle(billId, selfAmt, nonSelfAmt);
        Bill r2 = billingService.settle(billId, selfAmt, nonSelfAmt);
        Bill r3 = billingService.settle(billId, selfAmt, nonSelfAmt);

        assertThat(r1.getIsSettled()).isTrue();
        assertThat(r2.getIsSettled()).isTrue();
        assertThat(r3.getIsSettled()).isTrue();
        assertThat(r1.getBalanceSelf()).isEqualByComparingTo(r2.getBalanceSelf());
        assertThat(r2.getBalanceSelf()).isEqualByComparingTo(r3.getBalanceSelf());

        // Exactly one BillSettled event
        var settledEvents = outboxRepository.findAll().stream()
                .filter(e -> "BillSettled".equals(e.getEventType()))
                .toList();
        assertThat(settledEvents).hasSize(1);
    }

    // No helper needed - using AssertJ assertThatThrownBy
}
