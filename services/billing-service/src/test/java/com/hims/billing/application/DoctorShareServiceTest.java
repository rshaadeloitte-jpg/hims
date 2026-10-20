package com.hims.billing.application;

import com.hims.billing.application.exceptions.BillingEntityNotFoundException;
import com.hims.billing.application.exceptions.InvalidBillingRequestException;
import com.hims.billing.domain.model.Charge;
import com.hims.billing.domain.model.DoctorShare;
import com.hims.billing.domain.model.DoctorIncentive;
import com.hims.billing.domain.model.VisitCharge;
import com.hims.billing.domain.repository.ChargeRepository;
import com.hims.billing.domain.repository.DoctorIncentiveRepository;
import com.hims.billing.domain.repository.DoctorShareRepository;
import com.hims.billing.domain.repository.VisitChargeRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * M06 Doctor Share unit tests.
 * Tests share calculation logic (percentage-based + fixed amounts),
 * visit charges, and doctor incentives.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DoctorShareServiceTest {

    @Mock private DoctorShareRepository doctorShareRepository;
    @Mock private VisitChargeRepository visitChargeRepository;
    @Mock private DoctorIncentiveRepository doctorIncentiveRepository;
    @Mock private ChargeRepository chargeRepository;
    @Mock private EntityManager em;
    @Mock private Query query;

    private DoctorShareServiceImpl service;
    private final AtomicLong seq = new AtomicLong(100);

    @BeforeEach
    void wireService() {
        service = new DoctorShareServiceImpl(doctorShareRepository, visitChargeRepository,
                doctorIncentiveRepository, chargeRepository, em);
        lenient().when(em.createNativeQuery(anyString())).thenReturn(query);
        lenient().when(query.getSingleResult()).thenAnswer(inv -> seq.incrementAndGet());
        lenient().when(doctorShareRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(visitChargeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(doctorIncentiveRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---- Test 1: Percentage-based share calculation ----
    @Test
    void calculateShare_percentageBased_ceilingRounding() {
        DoctorShareService.DoctorShareCalculation result =
                service.calculateShare(new BigDecimal("1000.00"), new BigDecimal("33.33"), null);
        // 1000 * 33.33% = 333.30 -> CEILING scale 2 = 333.30
        assertThat(result.shareAmount()).isEqualByComparingTo("333.30");
        assertThat(result.hospitalAmount()).isEqualByComparingTo("666.70");
        assertThat(result.calculationType()).isEqualTo("PERCENTAGE");
    }

    // ---- Test 2: Fixed amount share calculation ----
    @Test
    void calculateShare_fixedAmount_takesPriority() {
        DoctorShareService.DoctorShareCalculation result =
                service.calculateShare(new BigDecimal("1000.00"), new BigDecimal("10"), new BigDecimal("250.00"));
        assertThat(result.shareAmount()).isEqualByComparingTo("250.00");
        assertThat(result.hospitalAmount()).isEqualByComparingTo("750.00");
        assertThat(result.calculationType()).isEqualTo("FIXED");
    }

    // ---- Test 3: Fixed amount capped at total ----
    @Test
    void calculateShare_fixedAmountCappedAtTotal() {
        DoctorShareService.DoctorShareCalculation result =
                service.calculateShare(new BigDecimal("100.00"), null, new BigDecimal("200.00"));
        assertThat(result.shareAmount()).isEqualByComparingTo("100.00");
        assertThat(result.hospitalAmount()).isEqualByComparingTo("0.00");
    }

    // ---- Test 4: No share when both null/zero ----
    @Test
    void calculateShare_noShareWhenBothZero() {
        DoctorShareService.DoctorShareCalculation result =
                service.calculateShare(new BigDecimal("500.00"), BigDecimal.ZERO, BigDecimal.ZERO);
        assertThat(result.shareAmount()).isEqualByComparingTo("0.00");
        assertThat(result.hospitalAmount()).isEqualByComparingTo("500.00");
        assertThat(result.calculationType()).isEqualTo("NONE");
    }

    // ---- Test 5: Negative total amount rejected ----
    @Test
    void calculateShare_negativeTotalAmount_rejected() {
        assertThatThrownBy(() -> service.calculateShare(new BigDecimal("-100"), new BigDecimal("10"), null))
                .isInstanceOf(InvalidBillingRequestException.class);
    }

    // ---- Test 6: Create doctor share ----
    @Test
    void createShare_assignsSequenceAndDefaults() {
        DoctorShare share = new DoctorShare();
        share.setBillId(1L);
        share.setChargeId(2L);
        share.setDoctorId(42L);
        share.setShareAmount(new BigDecimal("100.00"));
        share.setHospitalAmount(new BigDecimal("900.00"));

        DoctorShare result = service.createShare(share);
        assertThat(result.getShareId()).isEqualTo(101L);
        assertThat(result.getIsBilled()).isFalse();
    }

    // ---- Test 7: Mark share as paid ----
    @Test
    void markPaid_setsIsBilledTrue() {
        DoctorShare share = new DoctorShare();
        share.setShareId(1L);
        share.setIsBilled(false);
        when(doctorShareRepository.findById(1L)).thenReturn(Optional.of(share));

        DoctorShare result = service.markPaid(1L);
        assertThat(result.getIsBilled()).isTrue();
    }

    // ---- Test 8: Mark paid - not found ----
    @Test
    void markPaid_notFound_throwsException() {
        when(doctorShareRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.markPaid(999L))
                .isInstanceOf(BillingEntityNotFoundException.class)
                .hasMessageContaining("Doctor share not found");
    }

    // ---- Test 9: Get shares by bill ID ----
    @Test
    void getSharesByBillId_returnsShares() {
        DoctorShare share1 = new DoctorShare();
        share1.setShareId(1L);
        share1.setBillId(10L);
        DoctorShare share2 = new DoctorShare();
        share2.setShareId(2L);
        share2.setBillId(10L);
        when(doctorShareRepository.findByBillIdOrderByShareIdAsc(10L))
                .thenReturn(List.of(share1, share2));

        List<DoctorShare> result = service.getSharesByBillId(10L);
        assertThat(result).hasSize(2);
    }

    // ---- Test 10: Get shares by doctor ID ----
    @Test
    void getSharesByDoctorId_returnsShares() {
        DoctorShare share = new DoctorShare();
        share.setShareId(1L);
        share.setDoctorId(42L);
        when(doctorShareRepository.findByDoctorIdOrderByShareIdAsc(42L))
                .thenReturn(List.of(share));

        List<DoctorShare> result = service.getSharesByDoctorId(42L);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDoctorId()).isEqualTo(42L);
    }

    // ---- Test 11: Create visit charge ----
    @Test
    void createVisitCharge_assignsSequence() {
        VisitCharge vc = new VisitCharge();
        vc.setDoctorId(42L);
        vc.setAmount(new BigDecimal("500.00"));
        vc.setVisitType(1L);

        VisitCharge result = service.createVisitCharge(vc);
        assertThat(result.getVisitChargeId()).isEqualTo(101L);
    }

    // ---- Test 12: Get visit charges by doctor ----
    @Test
    void getVisitChargesByDoctorId_returnsCharges() {
        VisitCharge vc = new VisitCharge();
        vc.setVisitChargeId(1L);
        vc.setDoctorId(42L);
        when(visitChargeRepository.findByDoctorIdOrderByVisitChargeIdAsc(42L))
                .thenReturn(List.of(vc));

        List<VisitCharge> result = service.getVisitChargesByDoctorId(42L);
        assertThat(result).hasSize(1);
    }

    // ---- Test 13: Delete visit charge ----
    @Test
    void deleteVisitCharge_removesEntity() {
        VisitCharge vc = new VisitCharge();
        vc.setVisitChargeId(1L);
        when(visitChargeRepository.findById(1L)).thenReturn(Optional.of(vc));

        service.deleteVisitCharge(1L);
        verify(visitChargeRepository).delete(vc);
    }

    // ---- Test 14: Delete visit charge - not found ----
    @Test
    void deleteVisitCharge_notFound_throwsException() {
        when(visitChargeRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deleteVisitCharge(999L))
                .isInstanceOf(BillingEntityNotFoundException.class);
    }

    // ---- Test 15: Add incentive ----
    @Test
    void addIncentive_setsTypeAndDefaults() {
        DoctorIncentive incentive = new DoctorIncentive();
        incentive.setDoctorId(42L);
        incentive.setAmount(new BigDecimal("1000.00"));
        incentive.setRemark("Performance bonus");

        DoctorIncentive result = service.addIncentive(incentive);
        assertThat(result.getIncentiveType()).isEqualTo("ADDITION");
        assertThat(result.getIsBilled()).isFalse();
        assertThat(result.getIncentiveId()).isEqualTo(101L);
        assertThat(result.getIncentiveDate()).isNotNull();
    }

    // ---- Test 16: Deduct incentive ----
    @Test
    void deductIncentive_setsTypeAsDeduction() {
        DoctorIncentive incentive = new DoctorIncentive();
        incentive.setDoctorId(42L);
        incentive.setAmount(new BigDecimal("500.00"));
        incentive.setRemark("Penalty");

        DoctorIncentive result = service.deductIncentive(incentive);
        assertThat(result.getIncentiveType()).isEqualTo("DEDUCTION");
        assertThat(result.getIsBilled()).isFalse();
    }

    // ---- Test 17: Get incentives by doctor ----
    @Test
    void getIncentivesByDoctorId_returnsIncentives() {
        DoctorIncentive inc = new DoctorIncentive();
        inc.setIncentiveId(1L);
        inc.setDoctorId(42L);
        inc.setIncentiveType("ADDITION");
        when(doctorIncentiveRepository.findByDoctorIdOrderByIncentiveIdAsc(42L))
                .thenReturn(List.of(inc));

        List<DoctorIncentive> result = service.getIncentivesByDoctorId(42L);
        assertThat(result).hasSize(1);
    }

    // ---- Test 18: Get unbilled incentives ----
    @Test
    void getUnbilledIncentivesByDoctorId_returnsOnlyUnbilled() {
        DoctorIncentive inc = new DoctorIncentive();
        inc.setIncentiveId(1L);
        inc.setDoctorId(42L);
        inc.setIsBilled(false);
        when(doctorIncentiveRepository.findByDoctorIdAndIsBilledOrderByIncentiveIdAsc(42L, false))
                .thenReturn(List.of(inc));

        List<DoctorIncentive> result = service.getUnbilledIncentivesByDoctorId(42L);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIsBilled()).isFalse();
    }

    // ---- Test 19: Mark incentive as billed ----
    @Test
    void markIncentiveBilled_setsIsBilledTrue() {
        DoctorIncentive inc = new DoctorIncentive();
        inc.setIncentiveId(1L);
        inc.setIsBilled(false);
        when(doctorIncentiveRepository.findById(1L)).thenReturn(Optional.of(inc));

        DoctorIncentive result = service.markIncentiveBilled(1L);
        assertThat(result.getIsBilled()).isTrue();
    }

    // ---- Test 20: Calculate shares for bill ----
    @Test
    void calculateSharesForBill_computesSharesForAllCharges() {
        Charge charge = new Charge();
        charge.setChargeId(7L);
        charge.setRate(new BigDecimal("1000.00"));
        charge.setQty(1);
        charge.setDoctorSharePct(new BigDecimal("10"));
        charge.setIsOtProcedure(false);
        when(chargeRepository.findByBillIdOrderByChargeIdAsc(10L)).thenReturn(List.of(charge));
        when(query.getSingleResult()).thenReturn(42L);

        List<DoctorShareService.DoctorShareCalculation> results = service.calculateSharesForBill(10L);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).shareAmount()).isEqualByComparingTo("100.00");
        assertThat(results.get(0).hospitalAmount()).isEqualByComparingTo("900.00");
    }

    // ---- Test 21: Calculate shares for bill skips OT ----
    @Test
    void calculateSharesForBill_skipsOtProcedures() {
        Charge otCharge = new Charge();
        otCharge.setChargeId(7L);
        otCharge.setRate(new BigDecimal("1000.00"));
        otCharge.setQty(1);
        otCharge.setDoctorSharePct(new BigDecimal("10"));
        otCharge.setIsOtProcedure(true);
        when(chargeRepository.findByBillIdOrderByChargeIdAsc(10L)).thenReturn(List.of(otCharge));

        List<DoctorShareService.DoctorShareCalculation> results = service.calculateSharesForBill(10L);
        assertThat(results).isEmpty();
    }

    // ---- Test 22: List shares with pagination ----
    @Test
    void listShares_returnsPage() {
        DoctorShare share = new DoctorShare();
        share.setShareId(1L);
        Page<DoctorShare> page = new PageImpl<>(List.of(share));
        when(doctorShareRepository.findAll(any(PageRequest.class))).thenReturn(page);

        Page<DoctorShare> result = service.listShares(0, 20);
        assertThat(result.getContent()).hasSize(1);
    }

    // ---- Test 23: Get share by ID ----
    @Test
    void getShare_returnsShare() {
        DoctorShare share = new DoctorShare();
        share.setShareId(1L);
        when(doctorShareRepository.findById(1L)).thenReturn(Optional.of(share));

        DoctorShare result = service.getShare(1L);
        assertThat(result.getShareId()).isEqualTo(1L);
    }

    // ---- Test 24: Get share not found ----
    @Test
    void getShare_notFound_throwsException() {
        when(doctorShareRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getShare(999L))
                .isInstanceOf(BillingEntityNotFoundException.class);
    }

    // ---- Test 25: Get bills by doctor ID ----
    @Test
    void getBillsByDoctorId_returnsShares() {
        DoctorShare share = new DoctorShare();
        share.setShareId(1L);
        share.setDoctorId(42L);
        when(doctorShareRepository.findByDoctorIdOrderByShareIdAsc(42L))
                .thenReturn(List.of(share));

        List<DoctorShare> result = service.getBillsByDoctorId(42L);
        assertThat(result).hasSize(1);
    }
}
