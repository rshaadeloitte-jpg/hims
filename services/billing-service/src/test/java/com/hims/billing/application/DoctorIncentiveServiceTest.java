package com.hims.billing.application;

import com.hims.billing.application.exceptions.BillingEntityNotFoundException;
import com.hims.billing.domain.model.DoctorIncentive;
import com.hims.billing.domain.repository.ChargeRepository;
import com.hims.billing.domain.repository.DoctorIncentiveRepository;
import com.hims.billing.domain.repository.DoctorShareRepository;
import com.hims.billing.domain.repository.VisitChargeRepository;
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

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for DoctorIncentive operations (additions and deductions).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Doctor Incentive Service")
class DoctorIncentiveServiceTest {

    @Mock private DoctorShareRepository doctorShareRepository;
    @Mock private VisitChargeRepository visitChargeRepository;
    @Mock private DoctorIncentiveRepository doctorIncentiveRepository;
    @Mock private ChargeRepository chargeRepository;
    @Mock private EntityManager em;
    @Mock private Query query;

    private DoctorShareServiceImpl service;
    private final AtomicLong seq = new AtomicLong(300);

    @BeforeEach
    void wireService() {
        service = new DoctorShareServiceImpl(doctorShareRepository, visitChargeRepository,
                doctorIncentiveRepository, chargeRepository, em);
        lenient().when(em.createNativeQuery(anyString())).thenReturn(query);
        lenient().when(query.getSingleResult()).thenAnswer(inv -> seq.incrementAndGet());
        lenient().when(doctorIncentiveRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---- Addition tests ----

    @Nested
    @DisplayName("Incentive addition")
    class AdditionTests {

        @Test
        void addIncentive_setsAdditionType() {
            DoctorIncentive incentive = new DoctorIncentive();
            incentive.setDoctorId(42L);
            incentive.setAmount(new BigDecimal("1000.00"));

            DoctorIncentive result = service.addIncentive(incentive);
            assertThat(result.getIncentiveType()).isEqualTo("ADDITION");
        }

        @Test
        void addIncentive_setsIsBilledFalse() {
            DoctorIncentive incentive = new DoctorIncentive();
            incentive.setDoctorId(42L);
            incentive.setAmount(new BigDecimal("500.00"));

            DoctorIncentive result = service.addIncentive(incentive);
            assertThat(result.getIsBilled()).isFalse();
        }

        @Test
        void addIncentive_setsIncentiveDate() {
            DoctorIncentive incentive = new DoctorIncentive();
            incentive.setDoctorId(42L);
            incentive.setAmount(new BigDecimal("500.00"));

            DoctorIncentive result = service.addIncentive(incentive);
            assertThat(result.getIncentiveDate()).isNotNull();
        }

        @Test
        void addIncentive_preservesExistingId() {
            DoctorIncentive incentive = new DoctorIncentive();
            incentive.setIncentiveId(500L);
            incentive.setDoctorId(42L);
            incentive.setAmount(new BigDecimal("500.00"));

            DoctorIncentive result = service.addIncentive(incentive);
            assertThat(result.getIncentiveId()).isEqualTo(500L);
        }

        @Test
        void addIncentive_largeAmount_storedCorrectly() {
            DoctorIncentive incentive = new DoctorIncentive();
            incentive.setDoctorId(42L);
            incentive.setAmount(new BigDecimal("100000.00"));

            DoctorIncentive result = service.addIncentive(incentive);
            assertThat(result.getAmount()).isEqualByComparingTo("100000.00");
            assertThat(result.getIncentiveType()).isEqualTo("ADDITION");
        }

        @Test
        void addIncentive_overridesExistingTypeToAddition() {
            DoctorIncentive incentive = new DoctorIncentive();
            incentive.setDoctorId(42L);
            incentive.setAmount(new BigDecimal("500.00"));
            incentive.setIncentiveType("DEDUCTION");

            DoctorIncentive result = service.addIncentive(incentive);
            assertThat(result.getIncentiveType()).isEqualTo("ADDITION");
        }
    }

    // ---- Deduction tests ----

    @Nested
    @DisplayName("Incentive deduction")
    class DeductionTests {

        @Test
        void deductIncentive_setsDeductionType() {
            DoctorIncentive incentive = new DoctorIncentive();
            incentive.setDoctorId(42L);
            incentive.setAmount(new BigDecimal("200.00"));

            DoctorIncentive result = service.deductIncentive(incentive);
            assertThat(result.getIncentiveType()).isEqualTo("DEDUCTION");
        }

        @Test
        void deductIncentive_setsIsBilledFalse() {
            DoctorIncentive incentive = new DoctorIncentive();
            incentive.setDoctorId(42L);
            incentive.setAmount(new BigDecimal("200.00"));

            DoctorIncentive result = service.deductIncentive(incentive);
            assertThat(result.getIsBilled()).isFalse();
        }

        @Test
        void deductIncentive_setsIncentiveDate() {
            DoctorIncentive incentive = new DoctorIncentive();
            incentive.setDoctorId(42L);
            incentive.setAmount(new BigDecimal("200.00"));

            DoctorIncentive result = service.deductIncentive(incentive);
            assertThat(result.getIncentiveDate()).isNotNull();
        }

        @Test
        void deductIncentive_overridesExistingTypeToDeduction() {
            DoctorIncentive incentive = new DoctorIncentive();
            incentive.setDoctorId(42L);
            incentive.setAmount(new BigDecimal("200.00"));
            incentive.setIncentiveType("ADDITION");

            DoctorIncentive result = service.deductIncentive(incentive);
            assertThat(result.getIncentiveType()).isEqualTo("DEDUCTION");
        }
    }

    // ---- Query tests ----

    @Nested
    @DisplayName("Incentive queries")
    class QueryTests {

        @Test
        void getIncentivesByDoctorId_returnsAll() {
            DoctorIncentive inc1 = new DoctorIncentive();
            inc1.setIncentiveId(1L);
            inc1.setDoctorId(42L);
            inc1.setIncentiveType("ADDITION");
            DoctorIncentive inc2 = new DoctorIncentive();
            inc2.setIncentiveId(2L);
            inc2.setDoctorId(42L);
            inc2.setIncentiveType("DEDUCTION");
            when(doctorIncentiveRepository.findByDoctorIdOrderByIncentiveIdAsc(42L))
                    .thenReturn(List.of(inc1, inc2));

            List<DoctorIncentive> result = service.getIncentivesByDoctorId(42L);
            assertThat(result).hasSize(2);
        }

        @Test
        void getUnbilledIncentivesByDoctorId_returnsOnlyUnbilled() {
            DoctorIncentive unbilled = new DoctorIncentive();
            unbilled.setIncentiveId(1L);
            unbilled.setDoctorId(42L);
            unbilled.setIsBilled(false);
            when(doctorIncentiveRepository.findByDoctorIdAndIsBilledOrderByIncentiveIdAsc(42L, false))
                    .thenReturn(List.of(unbilled));

            List<DoctorIncentive> result = service.getUnbilledIncentivesByDoctorId(42L);
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getIsBilled()).isFalse();
        }

        @Test
        void markIncentiveBilled_setsIsBilledTrue() {
            DoctorIncentive inc = new DoctorIncentive();
            inc.setIncentiveId(1L);
            inc.setIsBilled(false);
            when(doctorIncentiveRepository.findById(1L)).thenReturn(Optional.of(inc));

            DoctorIncentive result = service.markIncentiveBilled(1L);
            assertThat(result.getIsBilled()).isTrue();
        }

        @Test
        void markIncentiveBilled_notFound_throwsException() {
            when(doctorIncentiveRepository.findById(999L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.markIncentiveBilled(999L))
                    .isInstanceOf(BillingEntityNotFoundException.class);
        }
    }
}
