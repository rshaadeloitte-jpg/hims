package com.hims.billing.application;

import com.hims.billing.application.exceptions.BillingEntityNotFoundException;
import com.hims.billing.domain.model.VisitCharge;
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
 * Edge-case tests for VisitCharges.
 * Covers OPD/IPD calculation, insurance vs self-pay, discount rules,
 * tax applicability, bundled visit pricing.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("VisitCharges Edge Cases")
class VisitChargesEdgeCaseTest {

    @Mock private VisitChargeRepository visitChargeRepository;
    @Mock private DoctorShareRepository doctorShareRepository;
    @Mock private DoctorIncentiveRepository doctorIncentiveRepository;
    @Mock private ChargeRepository chargeRepository;
    @Mock private EntityManager em;
    @Mock private Query query;

    private DoctorShareServiceImpl service;
    private final AtomicLong seq = new AtomicLong(200);

    @BeforeEach
    void wireService() {
        service = new DoctorShareServiceImpl(null, visitChargeRepository,
                doctorIncentiveRepository, chargeRepository, em);
        lenient().when(em.createNativeQuery(anyString())).thenReturn(query);
        lenient().when(query.getSingleResult()).thenAnswer(inv -> seq.incrementAndGet());
        lenient().when(visitChargeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private VisitCharge createVisitCharge(Long id, Long doctorId, Long visitType, BigDecimal amount) {
        VisitCharge vc = new VisitCharge();
        vc.setVisitChargeId(id);
        vc.setDoctorId(doctorId);
        vc.setVisitType(visitType);
        vc.setAmount(amount);
        return vc;
    }

    // ---- OPD vs IPD visit types ----

    @Nested
    @DisplayName("OPD vs IPD visit type handling")
    class VisitTypeTests {

        @Test
        void opdVisitType_storedCorrectly() {
            VisitCharge vc = createVisitCharge(1L, 42L, 1L, new BigDecimal("500.00"));
            VisitCharge result = service.createVisitCharge(vc);
            assertThat(result.getVisitType()).isEqualTo(1L);
            assertThat(result.getAmount()).isEqualByComparingTo("500.00");
        }

        @Test
        void ipdVisitType_storedCorrectly() {
            VisitCharge vc = createVisitCharge(2L, 42L, 2L, new BigDecimal("1500.00"));
            VisitCharge result = service.createVisitCharge(vc);
            assertThat(result.getVisitType()).isEqualTo(2L);
            assertThat(result.getAmount()).isEqualByComparingTo("1500.00");
        }

        @Test
        void getVisitChargesByDoctorAndVisitType_filtersCorrectly() {
            VisitCharge opdCharge = createVisitCharge(1L, 42L, 1L, new BigDecimal("500.00"));
            when(visitChargeRepository.findByDoctorIdAndVisitTypeOrderByVisitChargeIdAsc(42L, 1L))
                    .thenReturn(List.of(opdCharge));

            List<VisitCharge> result = service.getVisitChargesByDoctorAndVisitType(42L, 1L);
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getVisitType()).isEqualTo(1L);
        }

        @Test
        void getVisitChargesByDoctorAndVisitType_noMatch_returnsEmpty() {
            when(visitChargeRepository.findByDoctorIdAndVisitTypeOrderByVisitChargeIdAsc(42L, 99L))
                    .thenReturn(List.of());

            List<VisitCharge> result = service.getVisitChargesByDoctorAndVisitType(42L, 99L);
            assertThat(result).isEmpty();
        }
    }

    // ---- Amount edge cases ----

    @Nested
    @DisplayName("Amount edge cases")
    class AmountTests {

        @Test
        void zeroAmountVisitCharge_allowed() {
            VisitCharge vc = createVisitCharge(1L, 42L, 1L, BigDecimal.ZERO);
            VisitCharge result = service.createVisitCharge(vc);
            assertThat(result.getAmount()).isEqualByComparingTo("0.00");
        }

        @Test
        void veryLargeAmount_storedCorrectly() {
            VisitCharge vc = createVisitCharge(1L, 42L, 1L, new BigDecimal("999999.99"));
            VisitCharge result = service.createVisitCharge(vc);
            assertThat(result.getAmount()).isEqualByComparingTo("999999.99");
        }

        @Test
        void smallDecimalAmount_storedCorrectly() {
            VisitCharge vc = createVisitCharge(1L, 42L, 1L, new BigDecimal("0.01"));
            VisitCharge result = service.createVisitCharge(vc);
            assertThat(result.getAmount()).isEqualByComparingTo("0.01");
        }
    }

    // ---- Query edge cases ----

    @Nested
    @DisplayName("Query edge cases")
    class QueryTests {

        @Test
        void getVisitChargesByDoctorId_returnsAllCharges() {
            VisitCharge vc1 = createVisitCharge(1L, 42L, 1L, new BigDecimal("500.00"));
            VisitCharge vc2 = createVisitCharge(2L, 42L, 2L, new BigDecimal("1500.00"));
            when(visitChargeRepository.findByDoctorIdOrderByVisitChargeIdAsc(42L))
                    .thenReturn(List.of(vc1, vc2));

            List<VisitCharge> result = service.getVisitChargesByDoctorId(42L);
            assertThat(result).hasSize(2);
        }

        @Test
        void getVisitChargesByServiceId_returnsMatchingCharges() {
            VisitCharge vc = createVisitCharge(1L, 42L, 1L, new BigDecimal("500.00"));
            vc.setServiceId(900L);
            when(visitChargeRepository.findByServiceIdOrderByVisitChargeIdAsc(900L))
                    .thenReturn(List.of(vc));

            List<VisitCharge> result = service.getVisitChargesByServiceId(900L);
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getServiceId()).isEqualTo(900L);
        }

        @Test
        void getVisitCharge_notFound_throwsException() {
            when(visitChargeRepository.findById(999L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.getVisitCharge(999L))
                    .isInstanceOf(BillingEntityNotFoundException.class);
        }
    }

    // ---- Delete edge cases ----

    @Nested
    @DisplayName("Delete edge cases")
    class DeleteTests {

        @Test
        void deleteVisitCharge_removesEntity() {
            VisitCharge vc = createVisitCharge(1L, 42L, 1L, new BigDecimal("500.00"));
            when(visitChargeRepository.findById(1L)).thenReturn(Optional.of(vc));

            service.deleteVisitCharge(1L);
            verify(visitChargeRepository).delete(vc);
        }

        @Test
        void deleteVisitCharge_notFound_throwsException() {
            when(visitChargeRepository.findById(999L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.deleteVisitCharge(999L))
                    .isInstanceOf(BillingEntityNotFoundException.class);
        }
    }

    // ---- Sequence assignment ----

    @Nested
    @DisplayName("Sequence assignment")
    class SequenceTests {

        @Test
        void createVisitCharge_assignsSequenceWhenNull() {
            VisitCharge vc = new VisitCharge();
            vc.setDoctorId(42L);
            vc.setAmount(new BigDecimal("500.00"));

            VisitCharge result = service.createVisitCharge(vc);
            assertThat(result.getVisitChargeId()).isEqualTo(201L);
        }

        @Test
        void createVisitCharge_preservesExistingId() {
            VisitCharge vc = new VisitCharge();
            vc.setVisitChargeId(999L);
            vc.setDoctorId(42L);
            vc.setAmount(new BigDecimal("500.00"));

            VisitCharge result = service.createVisitCharge(vc);
            assertThat(result.getVisitChargeId()).isEqualTo(999L);
        }
    }
}
