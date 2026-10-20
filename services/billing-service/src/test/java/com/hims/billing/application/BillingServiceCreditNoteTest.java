package com.hims.billing.application;

import com.hims.billing.application.exceptions.InvalidBillingRequestException;
import com.hims.billing.domain.model.Bill;
import com.hims.billing.domain.model.Charge;
import com.hims.billing.domain.model.CreditNote;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Tests for BillingService credit note operations (M04 gap).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("BillingService Credit Notes")
class BillingServiceCreditNoteTest {

    @Mock private BillRepository billRepository;
    @Mock private ChargeRepository chargeRepository;
    @Mock private DoctorShareRepository doctorShareRepository;
    @Mock private CreditNoteRepository creditNoteRepository;
    @Mock private PackageSubscriptionRepository packageSubscriptionRepository;
    @Mock private OutboxService outboxService;
    @Mock private EntityManager em;
    @Mock private Query query;

    private BillingService service;
    private final AtomicLong seq = new AtomicLong(500);

    @BeforeEach
    void wireService() {
        service = new BillingService(billRepository, chargeRepository, doctorShareRepository,
                creditNoteRepository, packageSubscriptionRepository, outboxService);
        ReflectionTestUtils.setField(service, "em", em);
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.getSingleResult()).thenAnswer(inv -> seq.incrementAndGet());
        lenient().when(billRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(chargeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(creditNoteRepository.save(any())).thenAnswer(inv -> {
            CreditNote cn = inv.getArgument(0);
            if (cn.getCreditNoteId() == null) cn.setCreditNoteId(7000L);
            return cn;
        });
    }

    private static Bill createFinalizedBill() {
        Bill b = new Bill();
        b.setBillId(3L);
        b.setPatientId(505L);
        b.setBillNo("OPDBILL-1");
        b.setIsInterim(false);
        b.setIsSettled(false);
        b.setIsCancelled(false);
        b.setTotalAmount(new BigDecimal("1000.00"));
        b.setSelfAmount(new BigDecimal("500.00"));
        b.setNonSelfAmount(new BigDecimal("500.00"));
        b.setBalanceSelf(new BigDecimal("500.00"));
        b.setBalanceNonSelf(new BigDecimal("500.00"));
        return b;
    }

    // ---- Credit note issuance ----

    @Nested
    @DisplayName("Credit note issuance")
    class IssuanceTests {

        @Test
        void issueCreditNote_validAmount_returnsCreditNote() {
            Bill b = createFinalizedBill();
            when(billRepository.findById(3L)).thenReturn(Optional.of(b));
            when(creditNoteRepository.findByBillIdAndStatusTrueOrderByCreditNoteIdDesc(3L))
                    .thenReturn(List.of());

            CreditNote result = service.issueCreditNote(3L, new BigDecimal("200.00"), "Overcharge refund");
            assertThat(result).isNotNull();
            assertThat(result.getAmount()).isEqualByComparingTo("200.00");
            assertThat(result.getBillId()).isEqualTo(3L);
            assertThat(result.getReason()).isEqualTo("Overcharge refund");
        }

        @Test
        void issueCreditNote_generatesUniqueNumber() {
            Bill b = createFinalizedBill();
            when(billRepository.findById(3L)).thenReturn(Optional.of(b));
            when(creditNoteRepository.findByBillIdAndStatusTrueOrderByCreditNoteIdDesc(3L))
                    .thenReturn(List.of());

            CreditNote result = service.issueCreditNote(3L, new BigDecimal("100.00"), "reason");
            assertThat(result.getCreditNoteNo()).startsWith("CN-");
        }

        @Test
        void issueCreditNote_zeroAmount_throwsException() {
            Bill b = createFinalizedBill();
            when(billRepository.findById(3L)).thenReturn(Optional.of(b));

            assertThatThrownBy(() -> service.issueCreditNote(3L, BigDecimal.ZERO, "zero"))
                    .isInstanceOf(InvalidBillingRequestException.class);
        }

        @Test
        void issueCreditNote_negativeAmount_throwsException() {
            Bill b = createFinalizedBill();
            when(billRepository.findById(3L)).thenReturn(Optional.of(b));

            assertThatThrownBy(() -> service.issueCreditNote(3L, new BigDecimal("-50"), "negative"))
                    .isInstanceOf(InvalidBillingRequestException.class);
        }

        @Test
        void issueCreditNote_exceedsBillTotal_throwsException() {
            Bill b = createFinalizedBill();
            when(billRepository.findById(3L)).thenReturn(Optional.of(b));
            when(creditNoteRepository.findByBillIdAndStatusTrueOrderByCreditNoteIdDesc(3L))
                    .thenReturn(List.of());

            assertThatThrownBy(() -> service.issueCreditNote(3L, new BigDecimal("1500.00"), "exceeds"))
                    .isInstanceOf(InvalidBillingRequestException.class);
        }

        @Test
        void issueCreditNote_onInterimBill_throwsException() {
            Bill b = createFinalizedBill();
            b.setIsInterim(true);
            when(billRepository.findById(3L)).thenReturn(Optional.of(b));

            assertThatThrownBy(() -> service.issueCreditNote(3L, new BigDecimal("100.00"), "interim"))
                    .isInstanceOf(InvalidBillingRequestException.class);
        }

        @Test
        void issueCreditNote_onCancelledBill_throwsException() {
            Bill b = createFinalizedBill();
            b.setIsCancelled(true);
            when(billRepository.findById(3L)).thenReturn(Optional.of(b));

            assertThatThrownBy(() -> service.issueCreditNote(3L, new BigDecimal("100.00"), "cancelled"))
                    .isInstanceOf(InvalidBillingRequestException.class);
        }

        @Test
        void issueCreditNote_multipleNotes_staysWithinTotal() {
            Bill b = createFinalizedBill();
            when(billRepository.findById(3L)).thenReturn(Optional.of(b));

            CreditNote existing = new CreditNote();
            existing.setAmount(new BigDecimal("400.00"));
            when(creditNoteRepository.findByBillIdAndStatusTrueOrderByCreditNoteIdDesc(3L))
                    .thenReturn(List.of(existing));

            CreditNote result = service.issueCreditNote(3L, new BigDecimal("500.00"), "second note");
            assertThat(result.getAmount()).isEqualByComparingTo("500.00");
            // 400 + 500 = 900 < 1000 total -> OK
        }

        @Test
        void issueCreditNote_multipleNotesExceedingTotal_throwsException() {
            Bill b = createFinalizedBill();
            when(billRepository.findById(3L)).thenReturn(Optional.of(b));

            CreditNote existing = new CreditNote();
            existing.setAmount(new BigDecimal("700.00"));
            when(creditNoteRepository.findByBillIdAndStatusTrueOrderByCreditNoteIdDesc(3L))
                    .thenReturn(List.of(existing));

            assertThatThrownBy(() -> service.issueCreditNote(3L, new BigDecimal("400.00"), "exceeds"))
                    .isInstanceOf(InvalidBillingRequestException.class);
        }
    }

    // ---- Credit note queries ----

    @Nested
    @DisplayName("Credit note queries")
    class QueryTests {

        @Test
        void creditNotesForBill_returnsList() {
            CreditNote cn1 = new CreditNote();
            cn1.setCreditNoteId(1L);
            cn1.setAmount(new BigDecimal("100.00"));
            CreditNote cn2 = new CreditNote();
            cn2.setCreditNoteId(2L);
            cn2.setAmount(new BigDecimal("200.00"));
            when(creditNoteRepository.findByBillIdAndStatusTrueOrderByCreditNoteIdDesc(3L))
                    .thenReturn(List.of(cn1, cn2));

            List<CreditNote> result = service.creditNotesForBill(3L);
            assertThat(result).hasSize(2);
        }
    }
}
