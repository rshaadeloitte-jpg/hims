package com.hims.billing.application;

import com.hims.billing.application.exceptions.InvalidBillingRequestException;
import com.hims.billing.domain.model.Bill;
import com.hims.billing.domain.model.Charge;
import com.hims.billing.domain.repository.BillRepository;
import com.hims.billing.domain.repository.ChargeRepository;
import com.hims.billing.domain.repository.CreditNoteRepository;
import com.hims.billing.domain.repository.DoctorShareRepository;
import com.hims.billing.domain.repository.PackageSubscriptionRepository;
import com.hims.billing.infrastructure.outbox.OutboxPublisher.BillingEvents;
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
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Extended settlement tests for BR-BILL-006.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("BillingService Settlement Extended")
class BillingServiceSettlementTest {

    @Mock private BillRepository billRepository;
    @Mock private ChargeRepository chargeRepository;
    @Mock private DoctorShareRepository doctorShareRepository;
    @Mock private CreditNoteRepository creditNoteRepository;
    @Mock private PackageSubscriptionRepository packageSubscriptionRepository;
    @Mock private OutboxService outboxService;
    @Mock private EntityManager em;
    @Mock private Query query;

    private BillingService service;

    @BeforeEach
    void wireService() {
        service = new BillingService(billRepository, chargeRepository, doctorShareRepository,
                creditNoteRepository, packageSubscriptionRepository, outboxService);
        ReflectionTestUtils.setField(service, "em", em);
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.getSingleResult()).thenAnswer(inv -> 1L);
        lenient().when(billRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(chargeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Bill createFinalizedBill(String self, String nonSelf) {
        Bill b = new Bill();
        b.setBillId(3L);
        b.setPatientId(505L);
        b.setBillNo("OPDBILL-1");
        b.setIsInterim(false);
        b.setIsSettled(false);
        b.setIsCancelled(false);
        b.setSelfAmount(new BigDecimal(self));
        b.setNonSelfAmount(new BigDecimal(nonSelf));
        b.setBalanceSelf(new BigDecimal(self));
        b.setBalanceNonSelf(new BigDecimal(nonSelf));
        b.setAddedDatetime(new Timestamp(System.currentTimeMillis()));
        when(billRepository.findById(3L)).thenReturn(Optional.of(b));
        when(chargeRepository.findByBillIdOrderByChargeIdAsc(3L)).thenReturn(List.of());
        return b;
    }

    // ---- Settlement edge cases ----

    @Nested
    @DisplayName("Settlement edge cases")
    class SettlementEdgeTests {

        @Test
        void settle_exactPaymentBothSides_settled() {
            Bill b = createFinalizedBill("500.00", "400.00");
            Bill result = service.settle(3L, new BigDecimal("500.00"), new BigDecimal("400.00"));
            assertThat(result.getIsSettled()).isTrue();
            assertThat(result.getBalanceSelf()).isEqualByComparingTo("0.00");
            assertThat(result.getBalanceNonSelf()).isEqualByComparingTo("0.00");
        }

        @Test
        void settle_overPayment_settledWithNegativeBalance() {
            Bill b = createFinalizedBill("500.00", "400.00");
            Bill result = service.settle(3L, new BigDecimal("600.00"), new BigDecimal("500.00"));
            assertThat(result.getIsSettled()).isTrue();
            assertThat(result.getBalanceSelf()).isEqualByComparingTo("-100.00");
            assertThat(result.getBalanceNonSelf()).isEqualByComparingTo("-100.00");
        }

        @Test
        void settle_partialOnOneSide_notSettled() {
            Bill b = createFinalizedBill("500.00", "400.00");
            Bill result = service.settle(3L, new BigDecimal("500.00"), new BigDecimal("200.00"));
            assertThat(result.getIsSettled()).isFalse();
            assertThat(result.getBalanceSelf()).isEqualByComparingTo("0.00");
            assertThat(result.getBalanceNonSelf()).isEqualByComparingTo("200.00");
        }

        @Test
        void settle_zeroPayment_notSettled() {
            Bill b = createFinalizedBill("500.00", "400.00");
            Bill result = service.settle(3L, BigDecimal.ZERO, BigDecimal.ZERO);
            assertThat(result.getIsSettled()).isFalse();
            assertThat(result.getBalanceSelf()).isEqualByComparingTo("500.00");
            assertThat(result.getBalanceNonSelf()).isEqualByComparingTo("400.00");
        }

        @Test
        void settle_negativeSelfPaid_throwsException() {
            createFinalizedBill("500.00", "400.00");
            assertThatThrownBy(() -> service.settle(3L, new BigDecimal("-1"), BigDecimal.ZERO))
                    .isInstanceOf(InvalidBillingRequestException.class);
        }

        @Test
        void settle_negativeNonSelfPaid_throwsException() {
            createFinalizedBill("500.00", "400.00");
            assertThatThrownBy(() -> service.settle(3L, BigDecimal.ZERO, new BigDecimal("-1")))
                    .isInstanceOf(InvalidBillingRequestException.class);
        }

        @Test
        void settle_onCancelledBill_throwsException() {
            Bill b = createFinalizedBill("500.00", "400.00");
            b.setIsCancelled(true);
            assertThatThrownBy(() -> service.settle(3L, new BigDecimal("500"), new BigDecimal("400")))
                    .isInstanceOf(InvalidBillingRequestException.class);
        }
    }

    // ---- Settlement idempotency ----

    @Nested
    @DisplayName("Settlement idempotency")
    class IdempotencyTests {

        @Test
        void settleReplay_sameState_noAdditionalEvent() {
            Bill b = createFinalizedBill("500.00", "400.00");
            // First call
            Bill first = service.settle(3L, new BigDecimal("500.00"), new BigDecimal("400.00"));
            assertThat(first.getIsSettled()).isTrue();

            // Replay
            Bill second = service.settle(3L, new BigDecimal("500.00"), new BigDecimal("400.00"));
            assertThat(second.getIsSettled()).isTrue();
            assertThat(second.getBalanceSelf()).isEqualByComparingTo("0.00");
        }
    }
}
