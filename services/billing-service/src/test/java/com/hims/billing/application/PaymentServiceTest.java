package com.hims.billing.application;

import com.hims.billing.application.exceptions.InvalidBillingRequestException;
import com.hims.billing.domain.model.AdvanceLedger;
import com.hims.billing.domain.model.Bill;
import com.hims.billing.domain.model.Receipt;
import com.hims.billing.domain.model.Refund;
import com.hims.billing.domain.repository.AdvanceLedgerRepository;
import com.hims.billing.domain.repository.BillRepository;
import com.hims.billing.domain.repository.ReceiptRepository;
import com.hims.billing.domain.repository.RefundRepository;
import com.hims.billing.infrastructure.outbox.OutboxEvent;
import com.hims.billing.infrastructure.outbox.OutboxService;
import com.hims.billing.infrastructure.outbox.OutboxPublisher.BillingEvents;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * L1 — payment rule pins (MIG-041):
 *   BR-BILL-007 FIFO advance consumption in advance_id ASC order; partial splits
 *     when the source advance cannot cover the remainder on its own
 *   BR-BILL-008 advance refund: requires non-zero balance (legacy BEXAR002);
 *     bill-cancel refund delegates to BillingService.cancelSettledBill
 *   E-08 PaymentReceived event emitted EXACTLY ONCE per receipt.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentServiceTest {

    @Mock private BillRepository billRepository;
    @Mock private AdvanceLedgerRepository advanceRepository;
    @Mock private ReceiptRepository receiptRepository;
    @Mock private RefundRepository refundRepository;
    @Mock private BillingService billingService;
    @Mock private OutboxService outboxService;
    @Mock private EntityManager em;
    @Mock private Query query;

    private PaymentService service;
    private final AtomicLong seq = new AtomicLong(0);
    private final AtomicLong receiptSeq = new AtomicLong(0);

    // capture lists of saved entities so test stubs that override
    // findByBillIdOrderByReceiptIdAsc can see them at call time
    final java.util.List<Receipt> savedReceipts = new java.util.ArrayList<>();
    final java.util.List<Refund> savedRefunds = new java.util.ArrayList<>();
    final java.util.List<AdvanceLedger> savedAdvances = new java.util.ArrayList<>();

    @BeforeEach
    void wire() {
        service = new PaymentService(billRepository, advanceRepository, receiptRepository,
                refundRepository, billingService, outboxService);
        ReflectionTestUtils.setField(service, "em", em);
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.getSingleResult()).thenAnswer(inv -> seq.incrementAndGet());
        lenient().when(advanceRepository.save(any())).thenAnswer(inv -> {
            Object o = inv.getArgument(0);
            if (o instanceof AdvanceLedger a) {
                if (a.getAdvanceId() == null) a.setAdvanceId(9000L);
                savedAdvances.add(a);
                return a;
            }
            return o;
        });
        lenient().when(receiptRepository.save(any())).thenAnswer(inv -> {
            Object o = inv.getArgument(0);
            if (o instanceof Receipt r) {
                if (r.getReceiptId() == null) r.setReceiptId(8000L);
                if (r.getReceiptNo() == null) r.setReceiptNo("RCPT-test");
                savedReceipts.add(r);
                return r;
            }
            return o;
        });
        lenient().when(refundRepository.save(any())).thenAnswer(inv -> {
            Object o = inv.getArgument(0);
            if (o instanceof Refund r) {
                if (r.getRefundId() == null) r.setRefundId(7000L);
                savedRefunds.add(r);
                return r;
            }
            return o;
        });
        // default: findByBillIdOrderByReceiptIdAsc returns the savedReceipts list
        lenient().when(receiptRepository.findByBillIdOrderByReceiptIdAsc(anyLong()))
                .thenAnswer(inv -> savedReceipts);
        lenient().when(billingService.get(anyLong())).thenAnswer(inv -> {
            Bill b = billRepository.findById(inv.getArgument(0)).orElseThrow();
            return b;
        });
        lenient().when(billingService.settle(anyLong(), any(), any())).thenAnswer(inv -> {
            long billId = inv.getArgument(0);
            BigDecimal paidSelf = inv.getArgument(1);
            BigDecimal paidNonSelf = inv.getArgument(2);
            Bill b = billRepository.findById(billId).orElseThrow();
            b.setBalanceSelf(b.getSelfAmount().subtract(paidSelf));
            b.setBalanceNonSelf(b.getNonSelfAmount().subtract(paidNonSelf));
            b.setIsSettled(b.getBalanceSelf().signum() <= 0 && b.getBalanceNonSelf().signum() <= 0);
            return b;
        });
    }

    private Bill bill(long id, long patientId, String self, String nonSelf) {
        Bill b = new Bill();
        b.setBillId(id);
        b.setPatientId(patientId);
        b.setEncounterId(7777L);
        b.setBillNo("OPDBILL-" + id);
        b.setIsInterim(false);
        b.setIsSettled(false);
        b.setIsCancelled(false);
        b.setSelfAmount(new BigDecimal(self));
        b.setNonSelfAmount(new BigDecimal(nonSelf));
        b.setBalanceSelf(new BigDecimal(self));
        b.setBalanceNonSelf(new BigDecimal(nonSelf));
        b.setCashCounterId(1L);
        when(billRepository.findById(id)).thenReturn(Optional.of(b));
        return b;
    }

    private AdvanceLedger receipt(long advanceId, long patientId, String amount, String balanceAfter) {
        AdvanceLedger r = new AdvanceLedger();
        r.setAdvanceId(advanceId);
        r.setPatientId(patientId);
        r.setEntryType(AdvanceLedger.TYPE_RECEIPT);
        r.setAmount(new BigDecimal(amount));
        r.setBalanceAfter(new BigDecimal(balanceAfter));
        r.setModeCode("CASH");
        return r;
    }

    // ---------------------------------------------------------- BR-BILL-007 FIFO consumption

    @Test
    void brBill007_consumeAdvanceFifo_singleAdvance_fullCoverage() {
        Bill b = bill(3L, 505L, "100.00", "50.00");
        when(advanceRepository.findReceiptsByPatientOrderByAdvanceIdAsc(505L))
                .thenReturn(List.of(receipt(1L, 505L, "100.00", "100.00")));
        when(advanceRepository.findByBillIdOrderByAdvanceIdAsc(3L)).thenReturn(List.of());
        when(advanceRepository.sumBalanceByPatient(505L)).thenReturn(new BigDecimal("100.00"));

        BigDecimal consumed = service.consumeAdvanceFifo(3L);
        assertThat(consumed).isEqualByComparingTo("100.00");

        ArgumentCaptor<AdvanceLedger> captor = ArgumentCaptor.forClass(AdvanceLedger.class);
        verify(advanceRepository, times(1)).save(captor.capture());
        AdvanceLedger cons = captor.getValue();
        assertThat(cons.getEntryType()).isEqualTo(AdvanceLedger.TYPE_CONSUMPTION);
        assertThat(cons.getAmount()).isEqualByComparingTo("-100.00");
        assertThat(cons.getBillId()).isEqualTo(3L);
        assertThat(cons.getConsumedAdvanceId()).isEqualTo(1L);
        assertThat(cons.getBalanceAfter()).isEqualByComparingTo("0.00");
    }

    @Test
    void brBill007_consumeAdvanceFifo_partialSplit_remainderFlowsToNextAdvance() {
        // bill self balance = 300, two advances 100 + 200
        Bill b = bill(3L, 505L, "300.00", "0.00");
        when(advanceRepository.findReceiptsByPatientOrderByAdvanceIdAsc(505L))
                .thenReturn(List.of(
                        receipt(1L, 505L, "100.00", "100.00"),
                        receipt(2L, 505L, "200.00", "200.00")));
        when(advanceRepository.findByBillIdOrderByAdvanceIdAsc(3L)).thenReturn(List.of());
        when(advanceRepository.sumBalanceByPatient(505L)).thenReturn(new BigDecimal("300.00"));

        BigDecimal consumed = service.consumeAdvanceFifo(3L);
        assertThat(consumed).isEqualByComparingTo("300.00");

        ArgumentCaptor<AdvanceLedger> captor = ArgumentCaptor.forClass(AdvanceLedger.class);
        verify(advanceRepository, times(2)).save(captor.capture());
        List<AdvanceLedger> all = captor.getAllValues();
        // 1st consumption: -100 against advance_id=1
        assertThat(all.get(0).getAmount()).isEqualByComparingTo("-100.00");
        assertThat(all.get(0).getConsumedAdvanceId()).isEqualTo(1L);
        assertThat(all.get(0).getBalanceAfter()).isEqualByComparingTo("0.00");
        // 2nd consumption: -200 against advance_id=2
        assertThat(all.get(1).getAmount()).isEqualByComparingTo("-200.00");
        assertThat(all.get(1).getConsumedAdvanceId()).isEqualTo(2L);
        assertThat(all.get(1).getBalanceAfter()).isEqualByComparingTo("0.00");
    }

    @Test
    void brBill007_consumeAdvanceFifo_noAdvanceAvailable_returnsZero() {
        Bill b = bill(3L, 505L, "100.00", "0.00");
        when(advanceRepository.findReceiptsByPatientOrderByAdvanceIdAsc(505L))
                .thenReturn(List.of());
        when(advanceRepository.findByBillIdOrderByAdvanceIdAsc(3L)).thenReturn(List.of());

        BigDecimal consumed = service.consumeAdvanceFifo(3L);
        assertThat(consumed).isEqualByComparingTo("0.00");
        verify(advanceRepository, times(0)).save(any());
    }

    @Test
    void brBill007_consumeAdvanceFifo_alreadyConsumedAdvance_remainingBypasses() {
        // advance 1 had 100 but 60 already consumed in a prior call: only 40 left
        Bill b = bill(3L, 505L, "100.00", "0.00");
        when(advanceRepository.findReceiptsByPatientOrderByAdvanceIdAsc(505L))
                .thenReturn(List.of(receipt(1L, 505L, "100.00", "40.00")));
        // prior CONSUMPTION entry: -60 against advance_id=1
        AdvanceLedger priorCons = new AdvanceLedger();
        priorCons.setAdvanceId(99L);
        priorCons.setBillId(3L);
        priorCons.setEntryType(AdvanceLedger.TYPE_CONSUMPTION);
        priorCons.setAmount(new BigDecimal("-60.00"));
        priorCons.setConsumedAdvanceId(1L);
        when(advanceRepository.findByBillIdOrderByAdvanceIdAsc(3L)).thenReturn(List.of(priorCons));
        when(advanceRepository.sumBalanceByPatient(505L)).thenReturn(new BigDecimal("40.00"));

        BigDecimal consumed = service.consumeAdvanceFifo(3L);
        assertThat(consumed).isEqualByComparingTo("40.00");
    }

    // ---------------------------------------------------------- advance deposit

    @Test
    void applyAdvance_positiveAmount_creditsBalance() {
        when(advanceRepository.sumBalanceByPatient(505L)).thenReturn(new BigDecimal("50.00"));
        BigDecimal newBal = service.applyAdvance(505L, new BigDecimal("100.00"), "CASH", "RCPT-1", "deposit");
        assertThat(newBal).isEqualByComparingTo("150.00");

        ArgumentCaptor<AdvanceLedger> captor = ArgumentCaptor.forClass(AdvanceLedger.class);
        verify(advanceRepository, times(1)).save(captor.capture());
        AdvanceLedger row = captor.getValue();
        assertThat(row.getEntryType()).isEqualTo(AdvanceLedger.TYPE_RECEIPT);
        assertThat(row.getAmount()).isEqualByComparingTo("100.00");
        assertThat(row.getBalanceAfter()).isEqualByComparingTo("150.00");
    }

    @Test
    void applyAdvance_zeroOrNegativeAmount_rejected() {
        assertThatThrownBy(() -> service.applyAdvance(505L, BigDecimal.ZERO, "CASH", "x", "y"))
                .isInstanceOf(InvalidBillingRequestException.class)
                .hasMessageContaining("amount must be > 0");
        assertThatThrownBy(() -> service.applyAdvance(505L, new BigDecimal("-1"), "CASH", "x", "y"))
                .isInstanceOf(InvalidBillingRequestException.class)
                .hasMessageContaining("amount must be > 0");
    }

    // ---------------------------------------------------------- E-08 PaymentReceived

    @Test
    void recordPayment_emitsPaymentReceivedEvent() {
        Bill b = bill(3L, 505L, "100.00", "50.00");
        when(receiptRepository.findByBillIdOrderByReceiptIdAsc(3L))
                .thenReturn(List.of());   // cumulative = this receipt alone

        service.recordPayment(3L, new BigDecimal("100"), new BigDecimal("50"),
                "CASH", null, null, false);

        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        verify(outboxService, times(1)).append(typeCaptor.capture(), anyString(), eq(3L),
                anyString(), anyString(), anyString());
        assertThat(typeCaptor.getValue()).isEqualTo(BillingEvents.PAYMENT_RECEIVED);
    }

    @Test
    void recordPayment_cumulativeSettlement_idempotentOnReplay() {
        Bill b = bill(3L, 505L, "100.00", "50.00");

        // First receipt: self=30, nonSelf=20 -> balances 70/30 -> not settled
        Receipt r1 = new Receipt();
        r1.setReceiptId(10L);
        r1.setReceiptNo("RCPT-1");
        r1.setAmount(new BigDecimal("30"));
        r1.setNonSelfAmount(new BigDecimal("20"));
        r1.setModeCode("CASH");
        r1.setTransactionType(Receipt.TX_TYPE_RECEIPT);
        // wire() routes findByBillIdOrderByReceiptIdAsc through savedReceipts
        // (and save() appends to it), so pre-seeding here is enough.
        savedReceipts.add(r1);

        Bill settled = service.recordPayment(3L, new BigDecimal("100"), new BigDecimal("50"),
                "CASH", null, null, false);
        // bill 3: paidSelf = 30 (r1) + 100 (new) = 130, paidNonSelf = 20 + 50 = 70
        // balanceSelf = 100-130 = -30, balanceNonSelf = 50-70 = -20 -> settled (overpayment ok)
        assertThat(settled.getIsSettled()).isTrue();
        assertThat(settled.getBalanceSelf()).isEqualByComparingTo("-30.00");
    }

    @Test
    void recordPayment_nonCashMode_requiresBankRef_rejected() {
        Bill b = bill(3L, 505L, "100.00", "50.00");
        when(receiptRepository.findByBillIdOrderByReceiptIdAsc(3L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.recordPayment(3L, new BigDecimal("50"), BigDecimal.ZERO,
                "CARD", null, null, false))
                .isInstanceOf(InvalidBillingRequestException.class)
                .hasMessageContaining("Non-cash modes require bankId");
    }

    // ---------------------------------------------------------- BR-BILL-008 refund

    @Test
    void issueRefund_advanceRefund_creditsBalance() {
        Bill b = bill(3L, 505L, "0.00", "0.00");
        when(advanceRepository.sumBalanceByPatient(505L)).thenReturn(new BigDecimal("50.00"));

        Bill result = service.issueRefund(3L, new BigDecimal("20.00"),
                Refund.MODE_ADVANCE, "refund test", Refund.TX_TYPE_ADVANCE_REFUND);
        assertThat(result).isNotNull();

        ArgumentCaptor<AdvanceLedger> advCap = ArgumentCaptor.forClass(AdvanceLedger.class);
        verify(advanceRepository, times(1)).save(advCap.capture());
        AdvanceLedger restore = advCap.getValue();
        assertThat(restore.getEntryType()).isEqualTo(AdvanceLedger.TYPE_CONSUMPTION);
        assertThat(restore.getAmount()).isEqualByComparingTo("-20.00");
        assertThat(restore.getBalanceAfter()).isEqualByComparingTo("30.00");

        ArgumentCaptor<Refund> refCap = ArgumentCaptor.forClass(Refund.class);
        verify(refundRepository, times(1)).save(refCap.capture());
        assertThat(refCap.getValue().getTransactionType()).isEqualTo(Refund.TX_TYPE_ADVANCE_REFUND);
        assertThat(refCap.getValue().getAmount()).isEqualByComparingTo("20.00");
    }

    @Test
    void issueRefund_advanceRefund_zeroBalance_BEXAR002() {
        Bill b = bill(3L, 505L, "0.00", "0.00");
        when(advanceRepository.sumBalanceByPatient(505L)).thenReturn(new BigDecimal("0.00"));

        assertThatThrownBy(() -> service.issueRefund(3L, new BigDecimal("10"),
                Refund.MODE_ADVANCE, "x", Refund.TX_TYPE_ADVANCE_REFUND))
                .isInstanceOf(InvalidBillingRequestException.class)
                .extracting(e -> ((InvalidBillingRequestException) e).getErrorCode())
                .isEqualTo("BEXAR002");
    }

    @Test
    void issueRefund_billCancel_delegatesToBillingService() {
        Bill b = bill(3L, 505L, "100.00", "50.00");
        b.setIsSettled(true);

        service.issueRefund(3L, new BigDecimal("150.00"),
                Refund.MODE_CASH, "patient request", Refund.TX_TYPE_BILL_CANCEL);

        verify(billingService, times(1)).cancelSettledBill(3L, "patient request");
        ArgumentCaptor<Refund> refCap = ArgumentCaptor.forClass(Refund.class);
        verify(refundRepository, times(1)).save(refCap.capture());
        assertThat(refCap.getValue().getTransactionType()).isEqualTo(Refund.TX_TYPE_BILL_CANCEL);
    }

    @Test
    void issueRefund_invalidTransactionType_rejected() {
        Bill b = bill(3L, 505L, "0.00", "0.00");
        assertThatThrownBy(() -> service.issueRefund(3L, new BigDecimal("10"),
                Refund.MODE_CASH, "x", Short.valueOf("5")))
                .isInstanceOf(InvalidBillingRequestException.class)
                .hasMessageContaining("transactionType must be 3");
    }
}
