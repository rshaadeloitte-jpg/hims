package com.hims.billing.application;

import com.hims.billing.application.exceptions.InvalidBillingRequestException;
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
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Tests for package subscription operations (M04 gap).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("BillingService Package Subscription (M04 gap)")
class BillingServicePackageSubscriptionTest {

    @Mock private BillRepository billRepository;
    @Mock private ChargeRepository chargeRepository;
    @Mock private DoctorShareRepository doctorShareRepository;
    @Mock private CreditNoteRepository creditNoteRepository;
    @Mock private PackageSubscriptionRepository packageSubscriptionRepository;
    @Mock private OutboxService outboxService;
    @Mock private EntityManager em;
    @Mock private Query query;

    private BillingService service;
    private final AtomicLong seq = new AtomicLong(700);

    @BeforeEach
    void wireService() {
        service = new BillingService(billRepository, chargeRepository, doctorShareRepository,
                creditNoteRepository, packageSubscriptionRepository, outboxService);
        ReflectionTestUtils.setField(service, "em", em);
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.getSingleResult()).thenAnswer(inv -> seq.incrementAndGet());
        lenient().when(billRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(packageSubscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---- Package subscription ----

    @Nested
    @DisplayName("Package subscription")
    class SubscriptionTests {

        @Test
        void subscribePackage_validRequest_returnsSubscription() {
            PackageSubscription result = service.subscribePackage(505L, 100L, null,
                    new BigDecimal("5000.00"), null, null);
            assertThat(result).isNotNull();
            assertThat(result.getPatientId()).isEqualTo(505L);
            assertThat(result.getPackageId()).isEqualTo(100L);
            assertThat(result.getSubscriptionNo()).startsWith("PS-");
        }

        @Test
        void subscribePackage_withBillId_linksToBill() {
            Bill b = new Bill();
            b.setBillId(3L);
            when(billRepository.findById(3L)).thenReturn(Optional.of(b));

            PackageSubscription result = service.subscribePackage(505L, 100L, 3L,
                    new BigDecimal("5000.00"), null, null);
            assertThat(result.getBillId()).isEqualTo(3L);
        }

        @Test
        void subscribePackage_nullPatientId_throwsException() {
            assertThatThrownBy(() -> service.subscribePackage(null, 100L, null, BigDecimal.ZERO, null, null))
                    .isInstanceOf(InvalidBillingRequestException.class);
        }

        @Test
        void subscribePackage_nullPackageId_throwsException() {
            assertThatThrownBy(() -> service.subscribePackage(505L, null, null, BigDecimal.ZERO, null, null))
                    .isInstanceOf(InvalidBillingRequestException.class);
        }

        @Test
        void subscribePackage_nullCoveredAmount_defaultsToZero() {
            PackageSubscription result = service.subscribePackage(505L, 100L, null, null, null, null);
            assertThat(result.getCoveredAmount()).isEqualByComparingTo("0.00");
        }

        @Test
        void packageSubscriptionsForPatient_returnsList() {
            PackageSubscription ps = new PackageSubscription();
            ps.setSubscriptionId(1L);
            ps.setPatientId(505L);
            when(packageSubscriptionRepository.findByPatientIdOrderBySubscriptionIdDesc(505L))
                    .thenReturn(List.of(ps));

            List<PackageSubscription> result = service.packageSubscriptionsForPatient(505L);
            assertThat(result).hasSize(1);
        }

        @Test
        void packageSubscriptionsForPatient_nullPatientId_throwsException() {
            assertThatThrownBy(() -> service.packageSubscriptionsForPatient(null))
                    .isInstanceOf(InvalidBillingRequestException.class);
        }

        @Test
        void packageSubscriptionsForBill_returnsList() {
            PackageSubscription ps = new PackageSubscription();
            ps.setSubscriptionId(1L);
            ps.setBillId(3L);
            when(packageSubscriptionRepository.findByBillIdOrderBySubscriptionIdAsc(3L))
                    .thenReturn(List.of(ps));

            List<PackageSubscription> result = service.packageSubscriptionsForBill(3L);
            assertThat(result).hasSize(1);
        }
    }
}
