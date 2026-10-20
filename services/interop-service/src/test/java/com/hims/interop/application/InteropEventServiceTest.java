package com.hims.interop.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.interop.application.exceptions.InvalidRequestException;
import com.hims.interop.domain.model.OutboundHis;
import com.hims.interop.domain.repository.OutboundHisRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * L0 — event-relay ingress routing + idempotency guards, no Spring context.
 * Register WP-T1 assertions pinned here:
 *   - routing: InvoiceGenerated/PaymentReceived -> HL7; LabResultFinalized -> EHR;
 *     PatientRegistered/EncounterCreated/PatientAdmitted/PatientDischarged -> HL7
 *   - unknown eventType -> 400 INTEROP_UNKNOWN_EVENT_TYPE
 *   - eventType / aggregateId required (BEXA003); aggregateId <= 40 chars
 *   - IDEMPOTENCY per (eventType, aggregateId): duplicate returns the existing
 *     row and never writes a second one (incl. the concurrent-insert race via
 *     DataIntegrityViolationException translation)
 *   - EVENT POLICY: the service has NO outbox dependency at all.
 */
class InteropEventServiceTest {

    private OutboundHisRepository repository;
    private InteropEventService service;
    private final AtomicLong seq = new AtomicLong(100);

    @BeforeEach
    void setUp() {
        repository = mock(OutboundHisRepository.class);
        service = new InteropEventService(repository, new ObjectMapper());
        EntityManager em = mock(EntityManager.class);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "em", em);
        Query seqQuery = mock(Query.class);
        when(seqQuery.getSingleResult()).thenAnswer(inv -> seq.incrementAndGet());
        when(em.createNativeQuery(contains("nextval"))).thenReturn(seqQuery);
        Map<String, OutboundHis> store = new HashMap<>();
        when(repository.save(any())).thenAnswer(inv -> {
            OutboundHis o = inv.getArgument(0);
            o.setId(seq.incrementAndGet());
            store.put(o.getEventType() + ":" + o.getAggregateId(), o);
            return o;
        });
        when(repository.findByEventTypeAndAggregateId(anyString(), anyString()))
                .thenAnswer(inv -> Optional.ofNullable(store.get(
                        inv.getArgument(0, String.class) + ":" + inv.getArgument(1, String.class))));
    }

    private static Map<String, Object> payload(String k, Object v) {
        return Map.of(k, v);
    }

    // ------------------------------------------------------------------ routing

    @Test
    void revenueEvents_routeToHl7Target() {
        assertThat(service.ingest("InvoiceGenerated", "B-1", payload("billId", 1)).row().getTargetSystem())
                .isEqualTo("HL7");
        assertThat(service.ingest("PaymentReceived", "P-1", payload("paymentId", 2)).row().getTargetSystem())
                .isEqualTo("HL7");
    }

    @Test
    void labResultFinalized_routesToEhrTarget() {
        assertThat(service.ingest("LabResultFinalized", "LR-1", payload("resultId", 9)).row().getTargetSystem())
                .isEqualTo("EHR");
    }

    @Test
    void patientEncounterAdmissionDischarge_routeToHl7Target() {
        for (String type : new String[]{"PatientRegistered", "EncounterCreated",
                "PatientAdmitted", "PatientDischarged"}) {
            OutboundHis row = service.ingest(type, "AGG-" + type, payload("patientId", 7)).row();
            assertThat(row.getTargetSystem()).as(type).isEqualTo("HL7");
            assertThat(row.getStatus()).isEqualTo(OutboundHis.STATUS_PENDING);
        }
    }

    @Test
    void unknownEventType_mapsTo400UnknownEventCode() {
        assertThatThrownBy(() -> service.ingest("SomethingRandom", "A-1", payload("x", 1)))
                .isInstanceOf(InvalidRequestException.class)
                .extracting(e -> ((InvalidRequestException) e).getErrorCode())
                .isEqualTo("INTEROP_UNKNOWN_EVENT_TYPE");
    }

    // ------------------------------------------------------------------ guards

    @Test
    void missingEventTypeOrAggregateId_mapsTo400Bexa003() {
        assertThatThrownBy(() -> service.ingest(null, "A-1", payload("x", 1)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("eventType is required");
        assertThatThrownBy(() -> service.ingest("InvoiceGenerated", null, payload("x", 1)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("aggregateId is required");
        assertThatThrownBy(() -> service.ingest("InvoiceGenerated", "  ", payload("x", 1)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("aggregateId is required");
        assertThatThrownBy(() -> service.ingest("InvoiceGenerated", "A-1", null))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("payload is required");
    }

    @Test
    void oversizedAggregateId_mapsTo400Bexa003() {
        assertThatThrownBy(() -> service.ingest("InvoiceGenerated", "X".repeat(41), payload("x", 1)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("at most 40 characters");
    }

    // ------------------------------------------------------------------ idempotency

    @Test
    void duplicateIngress_returnsExistingRow_noSecondWrite() {
        InteropEventService.IngestResult first =
                service.ingest("InvoiceGenerated", "BILL-77", payload("billId", 77));
        assertThat(first.created()).isTrue();

        // same event+aggregate, DIFFERENT payload: idempotency key wins, payload ignored
        InteropEventService.IngestResult second =
                service.ingest("InvoiceGenerated", "BILL-77", payload("billId", 999));
        assertThat(second.created()).isFalse();
        assertThat(second.row().getId()).isEqualTo(first.row().getId());
        assertThat(second.row().getPayload()).contains("\"billId\":77");

        verify(repository, never()).findByEventTypeAndAggregateId("InvoiceGenerated", "OTHER");
    }

    @Test
    void sameAggregateDifferentEventType_isANewRow_notADuplicate() {
        service.ingest("InvoiceGenerated", "AG-5", payload("x", 1));
        InteropEventService.IngestResult otherType =
                service.ingest("PaymentReceived", "AG-5", payload("y", 2));
        assertThat(otherType.created()).isTrue();   // uniqueness is per (eventType, aggregateId)
    }

    @Test
    void concurrentInsertRace_translatesToIdempotentDuplicateOutcome() {
        reset(repository);
        doThrow(new org.springframework.dao.DataIntegrityViolationException("uq_t_outbound_event"))
                .when(repository).save(any());

        OutboundHis winner = new OutboundHis();
        winner.setId(555L);
        winner.setEventType("LabResultFinalized");
        winner.setAggregateId("LR-RACE");
        when(repository.findByEventTypeAndAggregateId("LabResultFinalized", "LR-RACE"))
                .thenReturn(Optional.empty())                       // fast-path miss
                .thenReturn(Optional.of(winner));                   // post-constraint refetch

        InteropEventService.IngestResult result = service.ingest("LabResultFinalized", "LR-RACE", payload("r", 1));
        assertThat(result.created()).isFalse();
        assertThat(result.row().getId()).isEqualTo(555L);
    }

    @Test
    void newRow_startsPendingWithZeroAttempts() {
        OutboundHis row = service.ingest("PatientRegistered", "PAT-1", payload("patientId", 3)).row();
        assertThat(row.getStatus()).isEqualTo(OutboundHis.STATUS_PENDING);
        assertThat(row.getAttempts()).isZero();
        assertThat(row.getLastError()).isNull();
        assertThat(row.getId()).isNotNull();
    }
}
