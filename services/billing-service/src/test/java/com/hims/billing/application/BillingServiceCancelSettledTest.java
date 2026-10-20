package com.hims.billing.application;

import com.hims.billing.application.exceptions.InvalidBillingRequestException;
import com.hims.billing.domain.model.Bill;
import com.hims.billing.domain.model.Charge;
import com.hims.billing.domain.model.DoctorShare;
import com.hims.billing.domain.repository.BillRepository;
import com.hims.billing.domain.repository.ChargeRepository;
import com.hims.billing.domain.repository.CreditNoteRepository;
import com.hims.billing.domain.repository.DoctorShareRepository;
import com.hims.billing.domain.repository.PackageSubscriptionRepository;
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
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for BR-BILL-008 settled bill cancellation.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("BillingService Cancel Settled Bill (BR-BILL-008)")
class BillingServiceCancelSettledTest {

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
        lenient().when(doctorShareRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private static Bill createSettledBill() {
        Bill b = new Bill();
        b.setBillId(3L);
        b.setPatientId(505L);
        b.setBillNo("OPDBILL-1");
        b.setIsInterim(false);
        b.setIsSettled(true);
        b.setIsCancelled(false);
        b.setSelfAmount(new BigDecimal("500.00"));
        b.setNonSelfAmount(new BigDecimal("400.00"));
        b.setBalanceSelf(new BigDecimal("0.00"));
        b.setBalanceNonSelf(new BigDecimal("0.00"));
        b.setAddedDatetime(new Timestamp(System.currentTimeMillis()));
        return b;
    }

    // ---- Cancel settled bill ----

    @Nested
    @DisplayName("Cancel settled bill")
    class CancelTests {

        @Test
        void cancelSettledBill_marksBillCancelled() {
            Bill b = createSettledBill();
            when(billRepository.findById(3L)).thenReturn(Optional.of(b));
            when(chargeRepository.findByBillIdOrderByChargeIdAsc(3L)).thenReturn(List.of());
            when(doctorShareRepository.findByBillIdOrderByShareIdAsc(3L)).thenReturn(List.of());

            Bill result = service.cancelSettledBill(3L, "Patient request");
            assertThat(result.getIsCancelled()).isTrue();
            assertThat(result.getIsSettled()).isFalse();
        }

        @Test
        void cancelSettledBill_restoresDoctorShares() {
            Bill b = createSettledBill();
            DoctorShare ds = new DoctorShare();
            ds.setShareId(1L);
            ds.setBillId(3L);
            ds.setIsBilled(true);
            when(billRepository.findById(3L)).thenReturn(Optional.of(b));
            when(chargeRepository.findByBillIdOrderByChargeIdAsc(3L)).thenReturn(List.of());
            when(doctorShareRepository.findByBillIdOrderByShareIdAsc(3L)).thenReturn(List.of(ds));

            service.cancelSettledBill(3L, "reason");
            verify(doctorShareRepository).save(ds);
            assertThat(ds.getIsBilled()).isFalse();
        }

        @Test
        void cancelSettledBill_marksChargesCancelled() {
            Bill b = createSettledBill();
            Charge c = new Charge();
            c.setChargeId(7L);
            c.setTotalBeforeFinalization(new BigDecimal("100.00"));
            when(billRepository.findById(3L)).thenReturn(Optional.of(b));
            when(chargeRepository.findByBillIdOrderByChargeIdAsc(3L)).thenReturn(List.of(c));
            when(doctorShareRepository.findByBillIdOrderByShareIdAsc(3L)).thenReturn(List.of());

            service.cancelSettledBill(3L, "reason");
            verify(chargeRepository).save(c);
            assertThat(c.getIsCancelled()).isTrue();
        }

        @Test
        void cancelSettledBill_alreadyCancelled_throwsException() {
            Bill b = createSettledBill();
            b.setIsCancelled(true);
            when(billRepository.findById(3L)).thenReturn(Optional.of(b));

            assertThatThrownBy(() -> service.cancelSettledBill(3L, "already cancelled"))
                    .isInstanceOf(InvalidBillingRequestException.class);
        }

        @Test
        void cancelSettledBill_notSettled_throwsException() {
            Bill b = createSettledBill();
            b.setIsSettled(false);
            when(billRepository.findById(3L)).thenReturn(Optional.of(b));

            assertThatThrownBy(() -> service.cancelSettledBill(3L, "not settled"))
                    .isInstanceOf(InvalidBillingRequestException.class);
        }
    }
}
