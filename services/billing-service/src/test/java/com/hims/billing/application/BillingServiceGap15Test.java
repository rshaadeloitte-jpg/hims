package com.hims.billing.application;

import com.hims.billing.domain.model.Bill;
import com.hims.billing.domain.model.PackageSubscription;
import com.hims.billing.domain.repository.BillRepository;
import com.hims.billing.domain.repository.ChargeRepository;
import com.hims.billing.domain.repository.CreditNoteRepository;
import com.hims.billing.domain.repository.DoctorShareRepository;
import com.hims.billing.domain.repository.PackageSubscriptionRepository;
import com.hims.billing.infrastructure.outbox.OutboxService;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BillingServiceGap15Test {

    @Mock private BillRepository billRepository;
    @Mock private ChargeRepository chargeRepository;
    @Mock private DoctorShareRepository doctorShareRepository;
    @Mock private CreditNoteRepository creditNoteRepository;
    @Mock private PackageSubscriptionRepository packageSubscriptionRepository;
    @Mock private OutboxService outboxService;
    @Mock private EntityManager em;
    @Mock private Query query;

    private BillingService service;
    private final AtomicLong seq = new AtomicLong(200);

    @BeforeEach
    void wiring() {
        service = new BillingService(billRepository, chargeRepository, doctorShareRepository,
                creditNoteRepository, packageSubscriptionRepository, outboxService);
        ReflectionTestUtils.setField(service, "em", em);
        when(em.createNativeQuery(anyString())).thenAnswer(inv -> query);
        when(query.getSingleResult()).thenAnswer(inv -> seq.incrementAndGet());
        lenient().when(billRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(packageSubscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---- estimate ----------------------------------------------------

    @Test
    void estimate_sumsLineTotalsUsingCalculator_selfPath() {
        BillingService.EstimateItem a = new BillingService.EstimateItem(1L, "A", new BigDecimal("100"), 1,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false,
                new BigDecimal("40"), new BigDecimal("10"), new BigDecimal("5"),
                BigDecimal.ZERO, null, false);
        BillingService.EstimateItem b = new BillingService.EstimateItem(2L, "B", new BigDecimal("200"), 2,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false,
                new BigDecimal("0"), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, null, false);
        BillingService.EstimateResult r = service.estimate(505L, true, List.of(a, b));
        // a: lineTotal 100, selfBase 40, con 15% -> selfRate 34, nonSelf 60
        // b: coPay 0 => effective 100, selfBase 400, con 0 -> selfRate 400, nonSelf 0
        assertThat(r.totalAmount()).isEqualByComparingTo("500.00");
        assertThat(r.selfAmount()).isEqualByComparingTo("434.00");
        assertThat(r.nonSelfAmount()).isEqualByComparingTo("60.00");
        assertThat(r.lines()).hasSize(2);
    }

    @Test
    void estimate_rejectsEmptyItems() {
        assertThatThrownBy(() -> service.estimate(505L, true, List.of()))
                .hasMessageContaining("At least one item");
    }

    @Test
    void estimate_rejectsNullPatient() {
        assertThatThrownBy(() -> service.estimate(null, true, List.of(
                new BillingService.EstimateItem(1L, "A", BigDecimal.TEN, 1, null, null, null, false, null, null, null, null, null, false))))
                .hasMessageContaining("patientId");
    }

    // ---- sponsor -----------------------------------------------------

    @Test
    void changeSponsor_flipsIsSelfAndEmitsEvent() {
        Bill b = new Bill();
        b.setBillId(10L);
        b.setPatientId(505L);
        b.setIsSelfCompany(true);
        b.setIsCancelled(false);
        when(billRepository.findById(10L)).thenReturn(Optional.of(b));

        Bill result = service.changeSponsor(10L, 99L, 7L);

        ArgumentCaptor<Bill> captor = ArgumentCaptor.forClass(Bill.class);
        verify(billRepository).save(captor.capture());
        assertThat(captor.getValue().getSponsorCompanyId()).isEqualTo(99L);
        assertThat(captor.getValue().getSponsorPlanId()).isEqualTo(7L);
        assertThat(captor.getValue().getIsSelfCompany()).isFalse();
        verify(outboxService).append(eq("SponsorChanged"), eq("bill"), eq(10L), anyString(), anyString(), eq("sponsor-changed:10"));
        // clearing sponsor restores self
        when(billRepository.findById(10L)).thenReturn(Optional.of(captor.getValue()));
        service.changeSponsor(10L, null, null);
        assertThat(b.getIsSelfCompany()).isTrue();
    }

    @Test
    void changeSponsor_rejectsCancelledBill() {
        Bill b = new Bill();
        b.setBillId(10L);
        b.setIsCancelled(true);
        when(billRepository.findById(10L)).thenReturn(Optional.of(b));
        assertThatThrownBy(() -> service.changeSponsor(10L, 1L, null))
                .hasMessageContaining("cancelled");
    }

    // ---- package subscription ----------------------------------------

    @Test
    void subscribePackage_createsRowsAndEmitsPackageSubscribed() {
        Bill b = new Bill();
        b.setBillId(10L);
        b.setPatientId(505L);
        when(billRepository.findById(10L)).thenReturn(Optional.of(b));

        PackageSubscription ps = service.subscribePackage(505L, 777L, 10L, new BigDecimal("5000"), null, null);
        assertThat(ps.getSubscriptionNo()).startsWith("PS-");
        assertThat(ps.getPatientId()).isEqualTo(505L);
        assertThat(ps.getPackageId()).isEqualTo(777L);
        assertThat(ps.getBillId()).isEqualTo(10L);
        verify(packageSubscriptionRepository).save(any());
        verify(outboxService).append(eq("PackageSubscribed"), eq("package_subscription"), anyLong(), eq("505"), anyString(), anyString());
    }

    @Test
    void subscribePackage_withoutBill_stillCreates() {
        PackageSubscription ps = service.subscribePackage(505L, 777L, null, null, null, null);
        assertThat(ps.getBillId()).isNull();
        assertThat(ps.getCoveredAmount()).isEqualByComparingTo("0");
        verify(billRepository, never()).findById(anyLong());
    }

    @Test
    void subscribePackage_rejectsMissingPatient() {
        assertThatThrownBy(() -> service.subscribePackage(null, 1L, null, null, null, null))
                .hasMessageContaining("patientId");
    }
}
