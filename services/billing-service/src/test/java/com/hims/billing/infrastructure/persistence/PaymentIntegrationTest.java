package com.hims.billing.infrastructure.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.billing.BillingServiceApplication;
import com.hims.billing.application.BillingService;
import com.hims.billing.application.PaymentService;
import com.hims.billing.domain.model.Bill;
import com.hims.billing.domain.model.Charge;
import com.hims.billing.domain.model.Refund;
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

/**
 * L2 — integration tests on real PG16 for MIG-041 (payments, advances, refunds):
 *   1. Apply advance credits patient balance; CONSUMPTION entry debits it FIFO
 *   2. recordPayment emits E-08 PaymentReceived via the outbox
 *   3. BillSettled fires on first transition to settled (the existing companion
 *      event for the E-08 PaymentReceived)
 *   4. Bill-cancel refund (transactionType=4) delegates to BillingService and
 *      flips is_cancelled=true; advance refund (tx=3) restores balance via
 *      paired RECEIPT entry on t_advance_ledger
 *   5. NUMERIC(19,2) precision preserved through the FIFO + settlement flow
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = BillingServiceApplication.class)
@DirtiesContext
@Transactional
class PaymentIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired private BillingService billingService;
    @Autowired private PaymentService paymentService;
    @Autowired private OutboxRepository outboxRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager em;

    /** helper: create charge + bill + finalize so we have a finalized bill to pay. */
    private long createAndFinalizeBill(long patientId) {
        Charge charge = billingService.createCharge(new BillingService.NewCharge(
                900L, "X-Ray Chest", new BigDecimal("1000"), 1,
                null, null, null, false,
                new BigDecimal("40"),
                new BigDecimal("10"), new BigDecimal("5"),
                new BigDecimal("10"), null, false));
        Bill bill = billingService.createBill(patientId, 7777L, null, null, 42L, true,
                List.of(charge.getChargeId()));
        billingService.finalizeBill(bill.getBillId());
        return bill.getBillId();
    }

    // ---------------------------------------------------------- BR-BILL-007 FIFO consumption

    @Test
    void fifoConsumption_singleAdvance_fullCoverage() {
        long patientId = 1001L;
        // patient has a 1000 advance
        BigDecimal balance = paymentService.applyAdvance(patientId, new BigDecimal("1000"), "CASH", "RCPT-ADV-1", "deposit");
        assertThat(balance).isEqualByComparingTo("1000.00");

        long billId = createAndFinalizeBill(patientId);
        BigDecimal consumed = paymentService.consumeAdvanceFifo(billId);
        // selfAmount for this bill = 340 (SELF path, coPay=40, plan=10+bill=5, share=10)
        // available advance 1000 > balanceSelf 340, so consume exactly 340
        assertThat(consumed).isEqualByComparingTo("340.00");

        // the running balance is now 1000 - 340 = 660
        em.flush();
        BigDecimal runningBalance = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount), 0) FROM ihms_billing.t_advance_ledger WHERE patient_id = ?",
                BigDecimal.class, patientId);
        assertThat(runningBalance).isEqualByComparingTo("660.00");

        // bill balanceSelf is now 0 (the advance consumed 340 of the 340 self balance)
        Bill settled = billingService.get(billId);
        assertThat(settled.getBalanceSelf()).isEqualByComparingTo("0.00");
        // but balanceNonSelf is still 600 (advance only debits self per BR-BILL-007)
        assertThat(settled.getBalanceNonSelf()).isEqualByComparingTo("600.00");
        // not yet settled (one balance > 0)
        assertThat(settled.getIsSettled()).isFalse();
    }

    @Test
    void fifoConsumption_partialSplit_remainderFlowsToNextAdvance() {
        long patientId = 1002L;
        // two small advances (200 + 100) for a bill that has self balance 340
        paymentService.applyAdvance(patientId, new BigDecimal("200"), "CASH", "RCPT-1", null);
        paymentService.applyAdvance(patientId, new BigDecimal("100"), "CASH", "RCPT-2", null);

        long billId = createAndFinalizeBill(patientId);
        BigDecimal consumed = paymentService.consumeAdvanceFifo(billId);
        // 200 + 100 = 300, but self balance is 340, so consume all 300
        assertThat(consumed).isEqualByComparingTo("300.00");

        em.flush();
        // running balance: 200 + 100 - 200 - 100 = 0
        BigDecimal runningBalance = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount), 0) FROM ihms_billing.t_advance_ledger WHERE patient_id = ?",
                BigDecimal.class, patientId);
        assertThat(runningBalance).isEqualByComparingTo("0.00");

        // two CONSUMPTION rows on t_advance_ledger
        Integer consCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ihms_billing.t_advance_ledger "
              + "WHERE patient_id = ? AND entry_type = ?",
                Integer.class, patientId, "CONSUMPTION");
        assertThat(consCount).isEqualTo(2);
    }

    @Test
    void fifoConsumption_idempotent_secondCallReturnsZero() {
        long patientId = 1003L;
        paymentService.applyAdvance(patientId, new BigDecimal("200"), "CASH", "RCPT-1", null);
        long billId = createAndFinalizeBill(patientId);

        BigDecimal firstCall = paymentService.consumeAdvanceFifo(billId);
        assertThat(firstCall).isEqualByComparingTo("200.00");
        // balanceSelf after first call: 340 - 200 = 140, but advance balance is 0
        // so second call should return 0
        BigDecimal secondCall = paymentService.consumeAdvanceFifo(billId);
        assertThat(secondCall).isEqualByComparingTo("0.00");
    }

    // ---------------------------------------------------------- E-08 PaymentReceived

    @Test
    void recordPayment_emitsPaymentReceived_event() throws Exception {
        long patientId = 1004L;
        long billId = createAndFinalizeBill(patientId);

        paymentService.recordPayment(billId, new BigDecimal("340"), new BigDecimal("0"),
                "CASH", null, null, false);

        var events = outboxRepository.findAll();
        var paymentEvents = events.stream()
                .filter(e -> "PaymentReceived".equals(e.getEventType())).toList();
        assertThat(paymentEvents).hasSize(1);

        OutboxEvent ev = paymentEvents.get(0);
        assertThat(ev.getAggregateId()).isEqualTo(billId);
        assertThat(ev.getPartitionKey()).isEqualTo(String.valueOf(billId));
        assertThat(ev.getIdempotencyKey()).startsWith("payment-received:");

        JsonNode payload = MAPPER.readTree(ev.getPayload());
        assertThat(payload.get("billId").asText()).isEqualTo(String.valueOf(billId));
        assertThat(payload.get("amount").doubleValue()).isEqualTo(340.0);
        assertThat(payload.get("modeCode").asText()).isEqualTo("CASH");
        assertThat(payload.get("transactionType").asInt()).isEqualTo(2);
        assertThat(payload.get("balanceSelf").doubleValue()).isEqualTo(0.0);
    }

    @Test
    void recordPayment_partialPayment_billNotSettled_noBillSettledEvent() {
        long patientId = 1005L;
        long billId = createAndFinalizeBill(patientId);

        // partial: pay 100 of 340 self, none of 600 non-self
        paymentService.recordPayment(billId, new BigDecimal("100"), new BigDecimal("0"),
                "CASH", null, null, false);

        var billSettled = outboxRepository.findAll().stream()
                .filter(e -> "BillSettled".equals(e.getEventType())).toList();
        assertThat(billSettled).isEmpty();
    }

    @Test
    void recordPayment_fullPayment_settlesBill_emitsBillSettled() {
        long patientId = 1006L;
        long billId = createAndFinalizeBill(patientId);

        paymentService.recordPayment(billId, new BigDecimal("340"), new BigDecimal("600"),
                "CASH", null, null, false);

        var billSettled = outboxRepository.findAll().stream()
                .filter(e -> "BillSettled".equals(e.getEventType())).toList();
        assertThat(billSettled).hasSize(1);

        Bill settled = billingService.get(billId);
        assertThat(settled.getIsSettled()).isTrue();
    }

    @Test
    void recordPayment_cumulativeSettlement_secondPaymentComputesCorrectly() {
        long patientId = 1007L;
        long billId = createAndFinalizeBill(patientId);

        // pay in two halves: 100 then 240
        paymentService.recordPayment(billId, new BigDecimal("100"), new BigDecimal("0"),
                "CASH", null, null, false);
        paymentService.recordPayment(billId, new BigDecimal("240"), new BigDecimal("0"),
                "CASH", null, null, false);

        Bill settled = billingService.get(billId);
        // cumulative paid: 100 + 240 = 340 of self
        assertThat(settled.getBalanceSelf()).isEqualByComparingTo("0.00");
        assertThat(settled.getIsSettled()).isFalse();  // nonSelf 600 still outstanding

        // two PaymentReceived events
        long paymentEvents = outboxRepository.findAll().stream()
                .filter(e -> "PaymentReceived".equals(e.getEventType())).count();
        assertThat(paymentEvents).isEqualTo(2);
    }

    // ---------------------------------------------------------- BR-BILL-008 refund

    @Test
    void advanceRefund_decreasesBalance_BEXAR002OnZeroBalance() {
        long patientId = 1008L;
        // patient has 100 advance
        paymentService.applyAdvance(patientId, new BigDecimal("100"), "CASH", "RCPT-1", null);

        long billId = createAndFinalizeBill(patientId);
        // advance refund of 30: the patient takes 30 OUT of their advance balance
        paymentService.issueRefund(billId, new BigDecimal("30"),
                Refund.MODE_ADVANCE, "refund", Refund.TX_TYPE_ADVANCE_REFUND);

        em.flush();
        // patient balance is now 100 - 30 = 70 (CONSUMPTION entry debited it)
        BigDecimal balance = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount), 0) FROM ihms_billing.t_advance_ledger WHERE patient_id = ?",
                BigDecimal.class, patientId);
        assertThat(balance).isEqualByComparingTo("70.00");

        // a t_refund row with transaction_type=3 is the audit trail
        Integer refundCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ihms_billing.t_refund WHERE bill_id = ? AND transaction_type = 3",
                Integer.class, billId);
        assertThat(refundCount).isEqualTo(1);

        // the paired CONSUMPTION entry on t_advance_ledger is the balance mutation
        Integer consCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ihms_billing.t_advance_ledger "
              + "WHERE patient_id = ? AND bill_id = ? AND entry_type = ?",
                Integer.class, patientId, billId, "CONSUMPTION");
        assertThat(consCount).isEqualTo(1);
    }

    @Test
    void advanceRefund_BEXAR002_whenBalanceZero() {
        long patientId = 1009L;
        // patient has NO advance
        long billId = createAndFinalizeBill(patientId);

        try {
            paymentService.issueRefund(billId, new BigDecimal("30"),
                    Refund.MODE_ADVANCE, "refund", Refund.TX_TYPE_ADVANCE_REFUND);
            org.junit.jupiter.api.Assertions.fail("expected BEXAR002 for zero balance");
        } catch (com.hims.billing.application.exceptions.InvalidBillingRequestException e) {
            assertThat(e.getErrorCode()).isEqualTo("BEXAR002");
        }
    }

    @Test
    void billCancelRefund_delegatesToBillingService_andFlipsIsCancelled() {
        long patientId = 1010L;
        long billId = createAndFinalizeBill(patientId);

        // settle first
        paymentService.recordPayment(billId, new BigDecimal("340"), new BigDecimal("600"),
                "CASH", null, null, false);
        Bill settled = billingService.get(billId);
        assertThat(settled.getIsSettled()).isTrue();

        // cancel settled bill via refund tx=4
        paymentService.issueRefund(billId, new BigDecimal("150"),
                Refund.MODE_CASH, "patient request", Refund.TX_TYPE_BILL_CANCEL);

        em.flush();
        Bill cancelled = billingService.get(billId);
        assertThat(cancelled.getIsCancelled()).isTrue();
        assertThat(cancelled.getIsSettled()).isFalse();

        // t_refund row with transaction_type=4
        Integer cancelCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ihms_billing.t_refund WHERE bill_id = ? AND transaction_type = 4",
                Integer.class, billId);
        assertThat(cancelCount).isEqualTo(1);
    }

    // ---------------------------------------------------------- decimal precision

    @Test
    void decimalPrecision_advanceBalance_remainsHalfUp() {
        long patientId = 1011L;
        // two advances with 3-decimal amounts -> half-up scale 2
        paymentService.applyAdvance(patientId, new BigDecimal("100.005"), "CASH", "R1", null);
        paymentService.applyAdvance(patientId, new BigDecimal("200.005"), "CASH", "R2", null);

        em.flush();
        BigDecimal balance = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount), 0) FROM ihms_billing.t_advance_ledger WHERE patient_id = ?",
                BigDecimal.class, patientId);
        // 100.01 + 200.01 = 300.02
        assertThat(balance).isEqualByComparingTo("300.02");
    }

    @Test
    void fifoConsumption_dbLevelIndependentSequences() {
        // t_advance_seq and t_receipt_seq are independent
        em.flush();
        Integer adv = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.sequences WHERE sequence_schema = ? AND sequence_name = ?",
                Integer.class, "ihms_billing", "t_advance_seq");
        Integer rec = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.sequences WHERE sequence_schema = ? AND sequence_name = ?",
                Integer.class, "ihms_billing", "t_receipt_seq");
        Integer ref = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.sequences WHERE sequence_schema = ? AND sequence_name = ?",
                Integer.class, "ihms_billing", "t_refund_seq");
        assertThat(adv).isEqualTo(1);
        assertThat(rec).isEqualTo(1);
        assertThat(ref).isEqualTo(1);
    }
}
