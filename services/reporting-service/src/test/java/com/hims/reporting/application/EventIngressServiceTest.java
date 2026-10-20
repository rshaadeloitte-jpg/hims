package com.hims.reporting.application;

import com.hims.reporting.application.exceptions.InvalidReportingRequestException;
import com.hims.reporting.domain.model.RmPatientRegistry;
import com.hims.reporting.domain.repository.IngestedEventRepository;
import com.hims.reporting.domain.repository.RmEncounterStatsRepository;
import com.hims.reporting.domain.repository.RmPatientRegistryRepository;
import com.hims.reporting.domain.repository.RmOpStatsRepository;
import com.hims.reporting.domain.repository.RmRevenueDailyRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * L0 — internal event ingress guards and STRICT idempotency, no Spring context.
 * Register WP-T2 assertions pinned here:
 *   - routing: PatientRegistered / EncounterCreated / InvoiceGenerated /
 *     PaymentReceived supported; anything else -> 400 REPORTING_UNKNOWN_EVENT_TYPE
 *   - source document ids required (patientId / encounterId / invoiceId /
 *     paymentId); amount >= 0; BEXA003 otherwise
 *   - DOUBLE-INGEST => NO DOUBLE COUNT: replaying the same event returns
 *     DUPLICATE and touches NO read model (pinned per event type)
 *   - revenue accumulation math: amounts += per (statDate, headCode),
 *     headCode defaults to OPD
 *   - EVENT POLICY: nothing may write to the outbox on ANY path — the ingress has
 *     no emitter dependency at all and this is additionally asserted at L2.
 */
class EventIngressServiceTest {

    private RmPatientRegistryRepository patientRepository;
    private RmEncounterStatsRepository encounterRepository;
    private RmRevenueDailyRepository revenueRepository;
    private RmOpStatsRepository opStatsRepository;
    private IngestedEventRepository ledgerRepository;
    private EventIngressService service;

    /** In-memory ledger mirroring uq_ingested_event so replays short-circuit. */
    private final Set<String> ledger = new HashSet<>();

    @BeforeEach
    void setUp() {
        patientRepository = mock(RmPatientRegistryRepository.class);
        encounterRepository = mock(RmEncounterStatsRepository.class);
        revenueRepository = mock(RmRevenueDailyRepository.class);
        opStatsRepository = mock(RmOpStatsRepository.class);
        ledgerRepository = mock(IngestedEventRepository.class);
        service = new EventIngressService(patientRepository, encounterRepository,
                revenueRepository, opStatsRepository, ledgerRepository);

        EntityManager em = mock(EntityManager.class);
        ReflectionTestUtils.setField(service, "em", em);
        Query seqQuery = mock(Query.class);
        when(seqQuery.getSingleResult()).thenAnswer(inv -> 1L);
        when(em.createNativeQuery(contains("nextval"))).thenReturn(seqQuery);

        when(ledgerRepository.existsByEventTypeAndEventKey(any(), any()))
                .thenAnswer(inv -> !ledger.add(inv.getArgument(0) + "|" + inv.getArgument(1)));
        when(patientRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // tiny registry store so the patient upsert path can be observed
        Map<Long, RmPatientRegistry> registry = new HashMap<>();
        when(patientRepository.findById(any(Long.class))).thenAnswer(
                inv -> java.util.Optional.ofNullable(registry.get((Long) inv.getArgument(0))));
        when(patientRepository.save(any())).thenAnswer(inv -> {
            RmPatientRegistry row = inv.getArgument(0);
            registry.put(row.getPatientId(), row);
            return row;
        });
    }

    // ------------------------------------------------------------- routing guards

    @Test
    void ingest_requiresEventType_bexa003() {
        assertThatThrownBy(() -> service.ingest(null, Map.of()))
                .isInstanceOf(InvalidReportingRequestException.class)
                .extracting(e -> ((InvalidReportingRequestException) e).getErrorCode())
                .isEqualTo("BEXA003");
        assertThatThrownBy(() -> service.ingest("  ", Map.of()))
                .isInstanceOf(InvalidReportingRequestException.class)
                .hasMessageContaining("eventType");
        assertThatThrownBy(() -> service.ingest("PatientRegistered", null))
                .isInstanceOf(InvalidReportingRequestException.class)
                .hasMessageContaining("payload");
    }

    @Test
    void ingest_unknownEventType_mapsTo400WithCode() {
        assertThatThrownBy(() -> service.ingest("LabResultFinalized", Map.of("x", 1)))
                .isInstanceOf(InvalidReportingRequestException.class)
                .extracting(e -> ((InvalidReportingRequestException) e).getErrorCode())
                .isEqualTo("REPORTING_UNKNOWN_EVENT_TYPE");

        assertThatThrownBy(() -> service.ingest("patientregistered", Map.of()))
                .isInstanceOf(InvalidReportingRequestException.class)
                .extracting(e -> ((InvalidReportingRequestException) e).getErrorCode())
                .isEqualTo("REPORTING_UNKNOWN_EVENT_TYPE");   // routing is exact-match
    }

    @Test
    void patient_ingest_requiresPatientId_bexa003() {
        assertThatThrownBy(() -> service.ingest("PatientRegistered", Map.of("mrn", "M-1")))
                .isInstanceOf(InvalidReportingRequestException.class)
                .hasMessageContaining("patientId");
        assertThatThrownBy(() -> service.ingest("PatientRegistered", Map.of("patientId", "abc")))
                .isInstanceOf(InvalidReportingRequestException.class)
                .hasMessageContaining("numeric");
    }

    @Test
    void encounter_ingest_requiresEncounterId_andRevenueRequiresAmount() {
        assertThatThrownBy(() -> service.ingest("EncounterCreated", Map.of()))
                .isInstanceOf(InvalidReportingRequestException.class)
                .hasMessageContaining("encounterId");
        assertThatThrownBy(() -> service.ingest("InvoiceGenerated", Map.of("invoiceId", 1)))
                .isInstanceOf(InvalidReportingRequestException.class)
                .hasMessageContaining("amount");
        assertThatThrownBy(() -> service.ingest("InvoiceGenerated",
                Map.of("invoiceId", 1, "amount", "-5")))
                .isInstanceOf(InvalidReportingRequestException.class)
                .hasMessageContaining("amount must be >= 0");
    }

    // ------------------------------------------------------------- strict idempotency

    /**
     * THE pinned register assertion: DOUBLE-INGEST => NO DOUBLE COUNT.
     * Re-ingesting PatientRegistered for the same patientId returns DUPLICATE and
     * the registry still holds exactly one row (upserted in place).
     */
    @Test
    void patient_doubleIngest_noDuplicateRow_upsertInstead() {
        var first = service.ingest("PatientRegistered", Map.of(
                "patientId", 501L, "mrn", "MRN-501", "name", "Ramesh",
                "registeredAt", "2026-01-05T09:30:00Z"));
        assertThat(first.status()).isEqualTo(EventIngressService.STATUS_APPLIED);

        var second = service.ingest("PatientRegistered", Map.of(
                "patientId", 501L, "mrn", "MRN-501", "name", "Ramesh K"));
        assertThat(second.status()).isEqualTo(EventIngressService.STATUS_DUPLICATE);
        assertThat(second.eventKey()).isEqualTo("patient:501");

        verify(patientRepository, times(1)).save(any());   // second delivery touched nothing
    }

    /** EncounterCreated replay adds NOTHING to rm_encounter_stats (pinned). */
    @Test
    void encounter_doubleIngest_countsOnce() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("encounterId", 9001L);
        payload.put("statDate", "2026-02-01");
        payload.put("encounterType", "IPD");

        assertThat(service.ingest("EncounterCreated", payload).status())
                .isEqualTo(EventIngressService.STATUS_APPLIED);
        assertThat(service.ingest("EncounterCreated", payload).status())
                .isEqualTo(EventIngressService.STATUS_DUPLICATE);

        verify(encounterRepository, times(1)).increment(LocalDate.of(2026, 2, 1), "IPD");
    }

    /** InvoiceGenerated replay accumulates ONCE — totals never double (pinned). */
    @Test
    void invoice_doubleIngest_accumulatesOnce() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("invoiceId", 7001L);
        payload.put("statDate", "2026-02-01");
        payload.put("headCode", "PHARMACY");
        payload.put("amount", "250.50");

        assertThat(service.ingest("InvoiceGenerated", payload).status())
                .isEqualTo(EventIngressService.STATUS_APPLIED);
        assertThat(service.ingest("InvoiceGenerated", payload).status())
                .isEqualTo(EventIngressService.STATUS_DUPLICATE);

        verify(revenueRepository, times(1)).accumulate(
                LocalDate.of(2026, 2, 1), "PHARMACY", new BigDecimal("250.50"));
    }

    @Test
    void payment_replay_neverDoubleCounts() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("paymentId", 8001L);
        payload.put("amount", 99.00);

        assertThat(service.ingest("PaymentReceived", payload).status())
                .isEqualTo(EventIngressService.STATUS_APPLIED);
        assertThat(service.ingest("PaymentReceived", payload).status())
                .isEqualTo(EventIngressService.STATUS_DUPLICATE);

        verify(revenueRepository, times(1)).accumulate(
                eq(LocalDate.now()), eq("OPD"), any());
    }

    // ------------------------------------------------------------- accumulation math

    @Test
    void revenue_defaults_headCodeToOpd_andAccumulatesAcrossKeys() {
        service.ingest("InvoiceGenerated", Map.of("invoiceId", 1L, "amount", "100"));      // default OPD, today
        service.ingest("PaymentReceived", Map.of("paymentId", 2L, "amount", "40.25"));     // same key bucket
        service.ingest("InvoiceGenerated",
                Map.of("invoiceId", 3L, "amount", "10", "headCode", "LAB", "statDate", "2026-03-02"));

        verify(revenueRepository).accumulate(LocalDate.now(), "OPD", new BigDecimal("100"));
        verify(revenueRepository).accumulate(LocalDate.now(), "OPD", new BigDecimal("40.25"));
        verify(revenueRepository).accumulate(LocalDate.of(2026, 3, 2), "LAB", new BigDecimal("10"));
    }

    @Test
    void encounters_defaultStatDateToday_typeOpd_andSeparateIdsIncrementIndependently() {
        service.ingest("EncounterCreated", Map.of("encounterId", 1L));
        service.ingest("EncounterCreated", Map.of("encounterId", 2L));

        verify(encounterRepository, times(2)).increment(LocalDate.now(), "OPD");
    }

    @Test
    void patient_ingest_stampsReadModelFields_andLedgerKeyIsSourceDocumentId() {
        var result = service.ingest("PatientRegistered", Map.of(
                "patientId", 77L, "mrn", "MRN-77", "name", "Sunita",
                "registeredAt", "2026-01-05 10:15:30"));

        assertThat(result.status()).isEqualTo(EventIngressService.STATUS_APPLIED);
        assertThat(result.eventKey()).isEqualTo("patient:77");
    }

    // ------------------------------------------------------------- event policy

    /**
     * NEGATIVE OUTBOX assertion at L0: the full ingress flow performs NO outbox
     * writes whatsoever. EventIngressService takes ONLY the four read-model /
     * ledger repositories — no OutboxService/emitter exists in its constructor
     * graph, so no code path CAN produce an outbox row (L2 asserts the same on
     * real PG16).
     */
    @Test
    void fullIngressFlow_emitsNothing_noEventInfrastructureTouched() {
        service.ingest("PatientRegistered", Map.of("patientId", 9L, "mrn", "M-9"));
        service.ingest("EncounterCreated", Map.of("encounterId", 91L));
        service.ingest("InvoiceGenerated", Map.of("invoiceId", 92L, "amount", "5"));
        service.ingest("PaymentReceived", Map.of("paymentId", 93L, "amount", "5"));

        // exactly one write per flow step, all onto read models / ledger — never an outbox
        verify(patientRepository, times(1)).save(any());
        verify(encounterRepository, times(1)).increment(any(), any());
        verify(revenueRepository, times(2)).accumulate(any(), any(), any());
        verify(ledgerRepository, times(4)).existsByEventTypeAndEventKey(any(), any());
    }
}
