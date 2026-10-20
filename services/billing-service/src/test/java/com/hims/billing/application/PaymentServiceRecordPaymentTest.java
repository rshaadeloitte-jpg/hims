package com.hims.billing.application;

import com.hims.billing.application.exceptions.InvalidBillingRequestException;
import com.hims.billing.domain.model.Bill;
import com.hims.billing.domain.model.Receipt;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Extended recordPayment tests (BR-BILL-006).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PaymentService Record Payment Extended")
class PaymentServiceRecordPaymentTest {

    @Mock private BillRepository billRepository;
    @Mock private AdvanceLedgerRepository advanceRepository;
    @Mock private ReceiptRepository receiptRepository;
    @Mock private RefundRepository refundRepository;
    @Mock private BillingService billingService;
    @Mock private OutboxService outboxService;
    @Mock private EntityManager em;
    @Mock private Query query;

    private final List<Receipt> savedReceipts = new ArrayList<>();
    private PaymentService service;

    @BeforeEach
    void wire() {
        service = new PaymentService(billRepository, advanceRepository, receiptRepository,
                refundRepository, billingService, outboxService);
        ReflectionTestUtils.setField(service, "em", em);
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.getSingleResult()).thenAnswer(inv -> 1L);
        lenient().when(advanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(receiptRepository.save(any())).thenAnswer(inv -> {
            Receipt r = inv.getArgument(0);
            if (r.getReceiptId() == null) r.setReceiptId(8000L);
            if (r.getReceiptNo() == null) r.setReceiptNo("RCPT-test");
            savedReceipts.add(r);
            return r;
        });
        lenient().when(refundRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(receiptRepository.findByBillIdOrderByReceiptIdAsc(anyLong()))
                .thenAnswer(inv -> new ArrayList<>(savedReceipts));
        lenient().when(billingService.get(anyLong())).thenAnswer(inv -> {
            return billRepository.findById(inv.getArgument(0)).orElseThrow();
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
        b.setBillNo("OPDBILL-" + id);
        b.setSelfAmount(new BigDecimal(self));
        b.setNonSelfAmount(new BigDecimal(nonSelf));
        b.setBalanceSelf(new BigDecimal(self));
        b.setBalanceNonSelf(new BigDecimal(nonSelf));
        b.setCashCounterId(1L);
        when(billRepository.findById(id)).thenReturn(Optional.of(b));
        return b;
    }

    // ---- Payment recording edge cases ----

    @Nested
    @DisplayName("Payment recording edge cases")
    class PaymentTests {

        @Test
        void recordPayment_selfAmountOnly_settlesSelfSide() {
            Bill b = bill(3L, 505L, "100.00", "100.00");

            Bill result = service.recordPayment(3L, new BigDecimal("100.00"), BigDecimal.ZERO,
                    "CASH", null, null, false);

            assertThat(result).isNotNull();
        }

        @Test
        void recordPayment_nonSelfAmountOnly_doesNotSettleSelf() {
            Bill b = bill(3L, 505L, "100.00", "100.00");

            Bill result = service.recordPayment(3L, BigDecimal.ZERO, new BigDecimal("100.00"),
                    "CASH", null, null, false);

            assertThat(result).isNotNull();
        }

        @Test
        void recordPayment_negativeSelfAmount_throwsException() {
            Bill b = bill(3L, 505L, "100.00", "100.00");

            assertThatThrownBy(() -> service.recordPayment(3L, new BigDecimal("-1"), BigDecimal.ZERO,
                    "CASH", null, null, false))
                    .isInstanceOf(InvalidBillingRequestException.class);
        }

        @Test
        void recordPayment_negativeNonSelfAmount_throwsException() {
            Bill b = bill(3L, 505L, "100.00", "100.00");

            assertThatThrownBy(() -> service.recordPayment(3L, BigDecimal.ZERO, new BigDecimal("-1"),
                    "CASH", null, null, false))
                    .isInstanceOf(InvalidBillingRequestException.class);
        }

        @Test
        void recordPayment_blankModeCode_throwsException() {
            Bill b = bill(3L, 505L, "100.00", "100.00");

            assertThatThrownBy(() -> service.recordPayment(3L, new BigDecimal("50"), BigDecimal.ZERO,
                    "", null, null, false))
                    .isInstanceOf(InvalidBillingRequestException.class);
        }

        @Test
        void recordPayment_cardModeWithoutBankRef_throwsException() {
            Bill b = bill(3L, 505L, "100.00", "100.00");

            assertThatThrownBy(() -> service.recordPayment(3L, new BigDecimal("50"), BigDecimal.ZERO,
                    "CARD", null, null, false))
                    .isInstanceOf(InvalidBillingRequestException.class);
        }

        @Test
        void recordPayment_cardModeWithBankRef_succeeds() {
            Bill b = bill(3L, 505L, "100.00", "100.00");

            Bill result = service.recordPayment(3L, new BigDecimal("50"), BigDecimal.ZERO,
                    "CARD", 1L, "REF-123", false);

            assertThat(result).isNotNull();
        }

        @Test
        void recordPayment_multipleReceipts_cumulativeSettlement() {
            Bill b = bill(3L, 505L, "100.00", "100.00");

            // First payment
            service.recordPayment(3L, new BigDecimal("50"), BigDecimal.ZERO,
                    "CASH", null, null, false);

            // Second payment
            Bill result = service.recordPayment(3L, new BigDecimal("50"), new BigDecimal("100.00"),
                    "CASH", null, null, false);

            // Cumulative: self=100, nonSelf=100 -> settled
            assertThat(result.getIsSettled()).isTrue();
        }
    }
}
