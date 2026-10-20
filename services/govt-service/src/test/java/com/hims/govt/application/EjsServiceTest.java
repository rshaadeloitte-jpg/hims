package com.hims.govt.application;

import com.hims.govt.application.exceptions.GovtConflictException;
import com.hims.govt.application.exceptions.GovtNotFoundException;
import com.hims.govt.application.exceptions.InvalidGovtRequestException;
import com.hims.govt.domain.model.EjsAsha;
import com.hims.govt.domain.model.EjsIndicatorEntry;
import com.hims.govt.domain.model.EjsIncentive;
import com.hims.govt.domain.model.IncentiveRate;
import com.hims.govt.domain.repository.EjsAshaRepository;
import com.hims.govt.domain.repository.EjsIndicatorEntryRepository;
import com.hims.govt.domain.repository.EjsIncentiveRepository;
import com.hims.govt.domain.repository.EjsSupervisorRepository;
import com.hims.govt.domain.repository.IncentiveRateRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * L0 — EJS guards: ASHA registration, indicator upsert, compute math,
 * RECOMPUTE-REPLACES-COMPUTED-ONLY rule and the approve/pay machine
 * (register WP-T4), no Spring context.
 * EVENT POLICY note (cssd-service convention): no outbox emitter exists in the
 * constructor graph — nothing here can write an outbox row; IncentiveComputed
 * stays DEFERRED. L2 asserts the negative outbox contract on real PG16.
 */
class EjsServiceTest {

    private EjsAshaRepository ashaRepository;
    private EjsSupervisorRepository supervisorRepository;
    private EjsIndicatorEntryRepository entryRepository;
    private EjsIncentiveRepository incentiveRepository;
    private IncentiveRateRepository rateRepository;
    private EjsService service;
    private final AtomicLong seq = new AtomicLong(300);

    @BeforeEach
    void setUp() {
        ashaRepository = mock(EjsAshaRepository.class);
        supervisorRepository = mock(EjsSupervisorRepository.class);
        entryRepository = mock(EjsIndicatorEntryRepository.class);
        incentiveRepository = mock(EjsIncentiveRepository.class);
        rateRepository = mock(IncentiveRateRepository.class);
        EntityManager em = mock(EntityManager.class);
        service = new EjsService(ashaRepository, supervisorRepository, entryRepository,
                incentiveRepository, rateRepository);
        ReflectionTestUtils.setField(service, "em", em);

        Query seqQuery = mock(Query.class);
        when(seqQuery.getSingleResult()).thenAnswer(inv -> seq.incrementAndGet());
        when(em.createNativeQuery(contains("nextval"))).thenReturn(seqQuery);

        when(ashaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(entryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(incentiveRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(incentiveRepository.findByAshaIdAndPeriodYearAndPeriodMonth(any(), any(), any()))
                .thenReturn(List.of());
        when(ashaRepository.existsByAshaCode(any())).thenReturn(false);
    }

    private void seedRates() {
        rate("ANC_CHECKUP", "50.00");
        rate("DELIVERY", "600.00");
        rate("CHILD_IMMUNISATION", "75.00");
    }

    private void rate(String code, String value) {
        IncentiveRate r = new IncentiveRate();
        r.setIndicatorCode(code);
        r.setRate(new BigDecimal(value));
        when(rateRepository.findById(code)).thenReturn(Optional.of(r));
    }

    private static EjsIndicatorEntry entry(long id, String code, int value) {
        EjsIndicatorEntry e = new EjsIndicatorEntry();
        e.setEntryId(id);
        e.setAshaId(1L);
        e.setIndicatorCode(code);
        e.setPeriodYear(2025);
        e.setPeriodMonth(4);
        e.setValue(value);
        return e;
    }

    private static EjsIncentive incentive(long id, short status) {
        EjsIncentive i = new EjsIncentive();
        i.setIncentiveId(id);
        i.setAshaId(1L);
        i.setPeriodYear(2025);
        i.setPeriodMonth(4);
        i.setAmount(new BigDecimal("100.00"));
        i.setStatus(status);
        return i;
    }

    // ------------------------------------------------------------- asha

    @Test
    void createAsha_requiresCodeAndName_bexa003() {
        assertThatThrownBy(() -> service.createAsha(null, "Asha", null))
                .isInstanceOf(InvalidGovtRequestException.class)
                .hasMessageContaining("code is required");
        assertThatThrownBy(() -> service.createAsha("ASHA-1", " ", null))
                .isInstanceOf(InvalidGovtRequestException.class)
                .hasMessageContaining("name is required");
    }

    @Test
    void createAsha_duplicateCode_mapsTo409() {
        when(ashaRepository.existsByAshaCode("ASHA-DUP")).thenReturn(true);

        assertThatThrownBy(() -> service.createAsha("ASHA-DUP", "Asha", null))
                .isInstanceOf(GovtConflictException.class)
                .extracting(e -> ((GovtConflictException) e).getErrorCode())
                .isEqualTo("GOVT_DUPLICATE_ASHA_CODE");
    }

    @Test
    void createAsha_unknownSupervisor_govtUnknownSupervisor() {
        when(supervisorRepository.existsById(42L)).thenReturn(false);

        assertThatThrownBy(() -> service.createAsha("ASHA-1", "Asha", 42L))
                .isInstanceOf(InvalidGovtRequestException.class)
                .extracting(e -> ((InvalidGovtRequestException) e).getErrorCode())
                .isEqualTo("GOVT_UNKNOWN_SUPERVISOR");
    }

    @Test
    void createAsha_optionalSupervisorLinked_whenExists() {
        when(supervisorRepository.existsById(9L)).thenReturn(true);

        EjsAsha asha = service.createAsha("ASHA-1", "Asha", 9L);

        assertThat(asha.getSupervisorId()).isEqualTo(9L);
    }

    // ------------------------------------------------------------- indicator upsert

    @Test
    void upsert_requiresKnownAsha_404() {
        when(ashaRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.upsertIndicator(404L, "ANC_CHECKUP", 2025, 4, 5))
                .isInstanceOf(GovtNotFoundException.class)
                .extracting(e -> ((GovtNotFoundException) e).getErrorCode())
                .isEqualTo("NOT_FOUND");
    }

    @Test
    void upsert_validatesCodeYearMonthValue_bexa003() {
        when(ashaRepository.findById(1L)).thenReturn(Optional.of(new EjsAsha()));

        assertThatThrownBy(() -> service.upsertIndicator(1L, " ", 2025, 4, 5))
                .isInstanceOf(InvalidGovtRequestException.class)
                .hasMessageContaining("indicatorCode is required");
        assertThatThrownBy(() -> service.upsertIndicator(1L, "ANC_CHECKUP", null, 4, 5))
                .isInstanceOf(InvalidGovtRequestException.class)
                .hasMessageContaining("periodYear is required");
        assertThatThrownBy(() -> service.upsertIndicator(1L, "ANC_CHECKUP", 2025, 0, 5))
                .isInstanceOf(InvalidGovtRequestException.class)
                .hasMessageContaining("periodMonth must be between 1 and 12");
        assertThatThrownBy(() -> service.upsertIndicator(1L, "ANC_CHECKUP", 2025, 13, 5))
                .isInstanceOf(InvalidGovtRequestException.class)
                .hasMessageContaining("periodMonth must be between 1 and 12");
        assertThatThrownBy(() -> service.upsertIndicator(1L, "ANC_CHECKUP", 2025, 4, null))
                .isInstanceOf(InvalidGovtRequestException.class)
                .hasMessageContaining("value is required");
        assertThatThrownBy(() -> service.upsertIndicator(1L, "ANC_CHECKUP", 2025, 4, -1))
                .isInstanceOf(InvalidGovtRequestException.class)
                .hasMessageContaining("value must be >= 0");
    }

    @Test
    void upsert_insertsWhenKeyAbsent_andUpdatesSameKeyInPlace() {
        when(ashaRepository.findById(1L)).thenReturn(Optional.of(new EjsAsha()));
        when(entryRepository.findByAshaIdAndIndicatorCodeAndPeriodYearAndPeriodMonth(1L, "ANC_CHECKUP", 2025, 4))
                .thenReturn(Optional.empty())                 // first call: insert
                .thenReturn(Optional.of(entry(77L, "ANC_CHECKUP", 5)));   // second call: update

        EjsIndicatorEntry inserted = service.upsertIndicator(1L, "ANC_CHECKUP", 2025, 4, 5);
        assertThat(inserted.getEntryId()).isNotNull();     // fresh sequence id
        assertThat(inserted.getValue()).isEqualTo(5);

        EjsIndicatorEntry updated = service.upsertIndicator(1L, "ANC_CHECKUP", 2025, 4, 8);
        assertThat(updated.getEntryId()).isEqualTo(77L);   // SAME row updated, not duplicated
        assertThat(updated.getValue()).isEqualTo(8);
        verify(entryRepository, never()).deleteAll();
    }

    // ------------------------------------------------------------- compute math

    @Test
    void compute_sumsValueTimesRate_overThePeriod() {
        when(ashaRepository.findById(1L)).thenReturn(Optional.of(new EjsAsha()));
        seedRates();
        when(entryRepository.findByAshaIdAndPeriodYearAndPeriodMonth(1L, 2025, 4)).thenReturn(List.of(
                entry(1L, "ANC_CHECKUP", 2),          // 2 x 50  = 100
                entry(2L, "DELIVERY", 1),             // 1 x 600 = 600
                entry(3L, "CHILD_IMMUNISATION", 4))); // 4 x 75  = 300
        // total = 1000.00

        EjsIncentive result = service.compute(1L, 2025, 4);

        assertThat(result.getStatus()).isEqualTo(EjsIncentive.STATUS_COMPUTED);
        assertThat(result.getAmount()).isEqualByComparingTo("1000.00");
        assertThat(result.getComputedAt()).isNotNull();
        verify(incentiveRepository).save(result);
    }

    @Test
    void compute_entryWithoutRateRow_contributesZero_documentedBehavior() {
        when(ashaRepository.findById(1L)).thenReturn(Optional.of(new EjsAsha()));
        seedRates();   // no UNRATED code seeded
        when(entryRepository.findByAshaIdAndPeriodYearAndPeriodMonth(1L, 2025, 4)).thenReturn(List.of(
                entry(1L, "ANC_CHECKUP", 2),   // 100
                entry(2L, "UNRATED", 999)));   // no rate row -> contributes 0 (documented)

        EjsIncentive result = service.compute(1L, 2025, 4);

        assertThat(result.getAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    void compute_unknownAshaOrBadPeriod_mapsTo404OrBexa003() {
        when(ashaRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.compute(404L, 2025, 4))
                .isInstanceOf(GovtNotFoundException.class)
                .hasMessageContaining("ASHA not found");

        when(ashaRepository.findById(1L)).thenReturn(Optional.of(new EjsAsha()));
        assertThatThrownBy(() -> service.compute(1L, 2025, 13))
                .isInstanceOf(InvalidGovtRequestException.class)
                .hasMessageContaining("periodMonth must be between 1 and 12");
    }

    // ------------------------------------------------------------- recompute rule

    @Test
    void recompute_deletesPriorComputedOnly_keepsApprovedAndPaid() {
        when(ashaRepository.findById(1L)).thenReturn(Optional.of(new EjsAsha()));
        seedRates();
        when(entryRepository.findByAshaIdAndPeriodYearAndPeriodMonth(1L, 2025, 4))
                .thenReturn(List.of(entry(1L, "DELIVERY", 1)));   // 600

        EjsIncentive oldComputed = incentive(10L, EjsIncentive.STATUS_COMPUTED);
        EjsIncentive approved = incentive(11L, EjsIncentive.STATUS_APPROVED);
        EjsIncentive paid = incentive(12L, EjsIncentive.STATUS_PAID);
        when(incentiveRepository.findByAshaIdAndPeriodYearAndPeriodMonth(1L, 2025, 4))
                .thenReturn(List.of(oldComputed, approved, paid));

        EjsIncentive recomputed = service.compute(1L, 2025, 4);

        // ONLY the prior COMPUTED row was replaced
        ArgumentCaptor<Iterable<Long>> deleted = ArgumentCaptor.forClass(Iterable.class);
        verify(incentiveRepository).deleteAllById(deleted.capture());
        assertThat(deleted.getValue()).containsExactly(10L);

        // a FRESH COMPUTED row is produced with the recomputed amount
        assertThat(recomputed.getIncentiveId()).isNotIn(List.of(10L, 11L, 12L));
        assertThat(recomputed.getStatus()).isEqualTo(EjsIncentive.STATUS_COMPUTED);
        assertThat(recomputed.getAmount()).isEqualByComparingTo("600.00");
    }

    @Test
    void recompute_withNoPriorRows_stillProducesOneComputedRow() {
        when(ashaRepository.findById(1L)).thenReturn(Optional.of(new EjsAsha()));
        seedRates();

        EjsIncentive created = service.compute(1L, 2025, 4);   // no entries at all -> 0.00

        assertThat(created.getAmount()).isEqualByComparingTo("0.00");
        verify(incentiveRepository, never()).deleteAllById(any());
    }

    // ------------------------------------------------------------- approve/pay machine

    @Test
    void approve_fromComputed_movesToApproved() {
        when(incentiveRepository.findById(1L)).thenReturn(Optional.of(incentive(1L, EjsIncentive.STATUS_COMPUTED)));

        EjsIncentive approved = service.approve(1L);

        assertThat(approved.getStatus()).isEqualTo(EjsIncentive.STATUS_APPROVED);
    }

    @Test
    void approve_fromApprovedOrPaid_rejected_govtIllegalTransition() {
        when(incentiveRepository.findById(11L)).thenReturn(Optional.of(incentive(11L, EjsIncentive.STATUS_APPROVED)));
        when(incentiveRepository.findById(12L)).thenReturn(Optional.of(incentive(12L, EjsIncentive.STATUS_PAID)));

        assertThatThrownBy(() -> service.approve(11L))
                .isInstanceOf(InvalidGovtRequestException.class)
                .extracting(e -> ((InvalidGovtRequestException) e).getErrorCode())
                .isEqualTo("GOVT_ILLEGAL_TRANSITION");
        assertThatThrownBy(() -> service.approve(12L))
                .isInstanceOf(InvalidGovtRequestException.class)
                .extracting(e -> ((InvalidGovtRequestException) e).getErrorCode())
                .isEqualTo("GOVT_ILLEGAL_TRANSITION");
    }

    @Test
    void pay_fromApproved_movesToPaid_directPayFromComputedRejected() {
        when(incentiveRepository.findById(11L)).thenReturn(Optional.of(incentive(11L, EjsIncentive.STATUS_APPROVED)));
        when(incentiveRepository.findById(1L)).thenReturn(Optional.of(incentive(1L, EjsIncentive.STATUS_COMPUTED)));

        EjsIncentive paid = service.pay(11L);
        assertThat(paid.getStatus()).isEqualTo(EjsIncentive.STATUS_PAID);

        // COMPUTED cannot jump straight to PAID — the machine is COMPUTED -> APPROVED -> PAID
        assertThatThrownBy(() -> service.pay(1L))
                .isInstanceOf(InvalidGovtRequestException.class)
                .extracting(e -> ((InvalidGovtRequestException) e).getErrorCode())
                .isEqualTo("GOVT_ILLEGAL_TRANSITION");
    }

    @Test
    void approveAndPay_unknownIds_mapsTo404() {
        when(incentiveRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approve(404L))
                .isInstanceOf(GovtNotFoundException.class)
                .hasMessageContaining("Incentive not found");
        assertThatThrownBy(() -> service.pay(404L))
                .isInstanceOf(GovtNotFoundException.class)
                .hasMessageContaining("Incentive not found");
    }
}
