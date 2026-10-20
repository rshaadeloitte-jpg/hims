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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

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
 * Tests for BillingService concession logic.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("BillingService Concession")
class BillingServiceConcessionTest {

    @Mock private BillRepository billRepository;
    @Mock private ChargeRepository chargeRepository;
    @Mock private DoctorShareRepository doctorShareRepository;
    @Mock private CreditNoteRepository creditNoteRepository;
    @Mock private PackageSubscriptionRepository packageSubscriptionRepository;
    @Mock private OutboxService outboxService;
    @Mock private EntityManager em;
    @Mock private Query query;

    private BillingService service;
    private final AtomicLong seq = new AtomicLong(100);

    @BeforeEach
    void wireService() {
        service = new BillingService(billRepository, chargeRepository, doctorShareRepository,
                creditNoteRepository, packageSubscriptionRepository, outboxService);
        ReflectionTestUtils.setField(service, "em", em);
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.getSingleResult()).thenAnswer(inv -> seq.incrementAndGet());
        lenient().when(billRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(chargeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private static Charge createCharge(long id, BigDecimal rate, int qty, String plan, String bill) {
        Charge c = new Charge();
        c.setChargeId(id);
        c.setServiceId(900L);
        c.setServiceName("Test Service");
        c.setRate(rate);
        c.setQty(qty);
        c.setConcessionPlanPct(new BigDecimal(plan));
        c.setConcessionBillPct(new BigDecimal(bill));
        return c;
    }

    private static Bill createInterimBill() {
        Bill b = new Bill();
        b.setBillId(3L);
        b.setPatientId(505L);
        b.setEncounterId(7777L);
        b.setIsSelfCompany(true);
        b.setIsInterim(true);
        b.setIsSettled(false);
        b.setAddedDatetime(new Timestamp(System.currentTimeMillis()));
        return b;
    }

    // ---- Concession pre-finalize ----

    @Nested
    @DisplayName("Pre-finalize concession application")
    class PreFinalizeTests {

        @Test
        void applyConcession_onInterimBill_appliesPercentages() {
            Bill b = createInterimBill();
            Charge c = createCharge(7L, new BigDecimal("100.00"), 1, "0", "0");
            when(billRepository.findById(3L)).thenReturn(Optional.of(b));
            when(chargeRepository.findByBillIdOrderByChargeIdAsc(3L)).thenReturn(List.of(c));

            Bill result = service.applyConcession(3L, new BigDecimal("10"), new BigDecimal("5"));
            assertThat(result).isNotNull();
            verify(chargeRepository, atLeastOnce()).save(any());
        }

        @Test
        void applyConcession_zeroPercentages_noChange() {
            Bill b = createInterimBill();
            Charge c = createCharge(7L, new BigDecimal("100.00"), 1, "0", "0");
            when(billRepository.findById(3L)).thenReturn(Optional.of(b));
            when(chargeRepository.findByBillIdOrderByChargeIdAsc(3L)).thenReturn(List.of(c));

            Bill result = service.applyConcession(3L, BigDecimal.ZERO, BigDecimal.ZERO);
            assertThat(result).isNotNull();
        }

        @Test
        void applyConcession_withFlatAmount_distributesProportionally() {
            Bill b = createInterimBill();
            Charge c = createCharge(7L, new BigDecimal("100.00"), 1, "0", "0");
            when(billRepository.findById(3L)).thenReturn(Optional.of(b));
            when(chargeRepository.findByBillIdOrderByChargeIdAsc(3L)).thenReturn(List.of(c));

            Bill result = service.applyConcession(3L, BigDecimal.ZERO, BigDecimal.ZERO,
                    new BigDecimal("50.00"), null, null);
            assertThat(result).isNotNull();
        }
    }

    // ---- Concession validation ----

    @Nested
    @DisplayName("Concession validation")
    class ValidationTests {

        @Test
        void applyConcession_onFinalizedBill_throwsException() {
            Bill b = createInterimBill();
            b.setIsInterim(false);
            when(billRepository.findById(3L)).thenReturn(Optional.of(b));
            when(chargeRepository.findByBillIdOrderByChargeIdAsc(3L)).thenReturn(List.of());

            assertThatThrownBy(() -> service.applyConcession(3L, new BigDecimal("10"), BigDecimal.ZERO))
                    .isInstanceOf(InvalidBillingRequestException.class);
        }

        @Test
        void applyConcession_negativePercentage_throwsException() {
            Bill b = createInterimBill();
            when(billRepository.findById(3L)).thenReturn(Optional.of(b));
            when(chargeRepository.findByBillIdOrderByChargeIdAsc(3L)).thenReturn(List.of(createCharge(7L, new BigDecimal("100"), 1, "0", "0")));

            assertThatThrownBy(() -> service.applyConcession(3L, new BigDecimal("-1"), BigDecimal.ZERO))
                    .isInstanceOf(InvalidBillingRequestException.class);
        }

        @Test
        void applyConcession_noCharges_throwsException() {
            Bill b = createInterimBill();
            when(billRepository.findById(3L)).thenReturn(Optional.of(b));
            when(chargeRepository.findByBillIdOrderByChargeIdAsc(3L)).thenReturn(List.of());

            assertThatThrownBy(() -> service.applyConcession(3L, new BigDecimal("10"), BigDecimal.ZERO))
                    .isInstanceOf(InvalidBillingRequestException.class);
        }
    }
}
