package com.hims.billing.application;

import com.hims.billing.application.exceptions.InvalidBillingRequestException;
import com.hims.billing.domain.model.AdvanceLedger;
import com.hims.billing.domain.model.Bill;
import com.hims.billing.domain.model.Refund;
import com.hims.billing.domain.repository.AdvanceLedgerRepository;
import com.hims.billing.domain.repository.BillRepository;
import com.hims.billing.domain.repository.ReceiptRepository;
import com.hims.billing.domain.repository.RefundRepository;
import com.hims.billing.infrastructure.outbox.OutboxService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Extended BR-BILL-007 FIFO advance consumption edge cases.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PaymentService BR-BILL-007 FIFO Edge Cases")
class PaymentServiceAdvanceFifoTest {

    @Mock private BillRepository billRepository;
    @Mock private AdvanceLedgerRepository advanceRepository;
    @Mock private ReceiptRepository receiptRepository;
    @Mock private RefundRepository refundRepository;
    @Mock private BillingService billingService;
    @Mock private OutboxService outboxService;
    @Mock private EntityManager em;
    @Mock private Query query;

    private PaymentService service;

    @BeforeEach
    void wire() {
        service = new PaymentService(billRepository, advanceRepository, receiptRepository,
                refundRepository, billingService, outboxService);
        ReflectionTestUtils.setField(service, "em", em);
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.getSingleResult()).thenAnswer(inv -> 1L);
        lenient().when(advanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(receiptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(refundRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Bill bill(long id, long patientId, String self, String nonSelf) {
        Bill b = new Bill();
        b.setBillId(id);
        b.setPatientId(patientId);
        b.setSelfAmount(new BigDecimal(self));
        b.setNonSelfAmount(new BigDecimal(nonSelf));
        b.setBalanceSelf(new BigDecimal(self));
        b.setBalanceNonSelf(new BigDecimal(nonSelf));
        b.setIsCancelled(false);
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
        return r;
    }

    // ---- FIFO edge cases ----

    @Nested
    @DisplayName("FIFO consumption edge cases")
    class FifoTests {

        @Test
        void consumeAdvanceFifo_billAlreadySettled_returnsZero() {
            Bill b = bill(3L, 505L, "0.00", "0.00");
            when(advanceRepository.findReceiptsByPatientOrderByAdvanceIdAsc(505L))
                    .thenReturn(List.of(receipt(1L, 505L, "100.00", "100.00")));
            when(advanceRepository.findByBillIdOrderByAdvanceIdAsc(3L)).thenReturn(List.of());
            when(advanceRepository.sumBalanceByPatient(505L)).thenReturn(new BigDecimal("100.00"));

            BigDecimal consumed = service.consumeAdvanceFifo(3L);
            assertThat(consumed).isEqualByComparingTo("0.00");
        }

        @Test
        void consumeAdvanceFifo_partialAdvance_coversPartOfBill() {
            Bill b = bill(3L, 505L, "500.00", "0.00");
            when(advanceRepository.findReceiptsByPatientOrderByAdvanceIdAsc(505L))
                    .thenReturn(List.of(receipt(1L, 505L, "200.00", "200.00")));
            when(advanceRepository.findByBillIdOrderByAdvanceIdAsc(3L)).thenReturn(List.of());
            when(advanceRepository.sumBalanceByPatient(505L)).thenReturn(new BigDecimal("200.00"));

            BigDecimal consumed = service.consumeAdvanceFifo(3L);
            assertThat(consumed).isEqualByComparingTo("200.00");
        }

        @Test
        void consumeAdvanceFifo_multipleAdvances_exhaustsAll() {
            Bill b = bill(3L, 505L, "300.00", "0.00");
            when(advanceRepository.findReceiptsByPatientOrderByAdvanceIdAsc(505L))
                    .thenReturn(List.of(
                            receipt(1L, 505L, "100.00", "100.00"),
                            receipt(2L, 505L, "100.00", "100.00"),
                            receipt(3L, 505L, "100.00", "100.00")));
            when(advanceRepository.findByBillIdOrderByAdvanceIdAsc(3L)).thenReturn(List.of());
            when(advanceRepository.sumBalanceByPatient(505L)).thenReturn(new BigDecimal("300.00"));

            BigDecimal consumed = service.consumeAdvanceFifo(3L);
            assertThat(consumed).isEqualByComparingTo("300.00");
        }

        @Test
        void consumeAdvanceFifo_cancelledBill_throwsException() {
            Bill b = bill(3L, 505L, "100.00", "0.00");
            b.setIsCancelled(true);

            assertThatThrownBy(() -> service.consumeAdvanceFifo(3L))
                    .isInstanceOf(InvalidBillingRequestException.class);
        }

        @Test
        void consumeAdvanceFifo_billNotFound_throwsException() {
            when(billRepository.findById(999L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.consumeAdvanceFifo(999L))
                    .isInstanceOf(com.hims.billing.application.exceptions.BillingEntityNotFoundException.class);
        }
    }

    // ---- Advance deposit edge cases ----

    @Nested
    @DisplayName("Advance deposit edge cases")
    class DepositTests {

        @Test
        void applyAdvance_negativePatientId_throwsException() {
            assertThatThrownBy(() -> service.applyAdvance(-1L, new BigDecimal("100.00"), "CASH", "RCPT-1", "test"))
                    .isInstanceOf(InvalidBillingRequestException.class);
        }

        @Test
        void applyAdvance_zeroPatientId_throwsException() {
            assertThatThrownBy(() -> service.applyAdvance(0L, new BigDecimal("100.00"), "CASH", "RCPT-1", "test"))
                    .isInstanceOf(InvalidBillingRequestException.class);
        }

        @Test
        void applyAdvance_blankModeCode_throwsException() {
            assertThatThrownBy(() -> service.applyAdvance(505L, new BigDecimal("100.00"), "", "RCPT-1", "test"))
                    .isInstanceOf(InvalidBillingRequestException.class);
        }

        @Test
        void applyAdvance_nullModeCode_throwsException() {
            assertThatThrownBy(() -> service.applyAdvance(505L, new BigDecimal("100.00"), null, "RCPT-1", "test"))
                    .isInstanceOf(InvalidBillingRequestException.class);
        }
    }

    // ---- Refund edge cases ----

    @Nested
    @DisplayName("Refund edge cases")
    class RefundTests {

        @Test
        void issueRefund_advanceRefund_exceedsBalance_throwsBexar002() {
            Bill b = bill(3L, 505L, "0.00", "0.00");
            when(advanceRepository.sumBalanceByPatient(505L)).thenReturn(new BigDecimal("50.00"));

            assertThatThrownBy(() -> service.issueRefund(3L, new BigDecimal("100.00"),
                    Refund.MODE_ADVANCE, "over refund", Refund.TX_TYPE_ADVANCE_REFUND))
                    .isInstanceOf(InvalidBillingRequestException.class)
                    .hasMessageContaining("exceeds");
        }

        @Test
        void issueRefund_nullTransactionType_throwsException() {
            Bill b = bill(3L, 505L, "0.00", "0.00");

            assertThatThrownBy(() -> service.issueRefund(3L, new BigDecimal("50.00"),
                    Refund.MODE_CASH, "test", null))
                    .isInstanceOf(InvalidBillingRequestException.class);
        }

        @Test
        void issueRefund_zeroAmount_throwsException() {
            Bill b = bill(3L, 505L, "0.00", "0.00");

            assertThatThrownBy(() -> service.issueRefund(3L, BigDecimal.ZERO,
                    Refund.MODE_CASH, "zero", Refund.TX_TYPE_ADVANCE_REFUND))
                    .isInstanceOf(InvalidBillingRequestException.class);
        }
    }
}
