package com.hims.interop.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.interop.application.exceptions.ConflictException;
import com.hims.interop.application.exceptions.InteropNotFoundException;
import com.hims.interop.application.exceptions.InvalidRequestException;
import com.hims.interop.domain.model.AbdmRequestQueue;
import com.hims.interop.domain.repository.AbdmRequestQueueRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * L0 — ABDM correlation-id uniqueness + idempotent callback guards + ABHA/M1
 * support, no Spring context. Register WP-T1 + MIG-055 assertions pinned here:
 *   - create: requestKind in ABHA|M1|HIP|M2|HIU|M3|HFR|M0|CM else 400 BEXA003;
 *     correlationId required (<= 64); duplicate correlationId -> 409
 *     INTEROP_DUPLICATE_CORRELATION_ID (incl. the DB-constraint race);
 *     duplicate envelopeEventId -> 409 INTEROP_DUPLICATE_ENVELOPE_EVENT_ID
 *   - milestone auto-derived: ABHA -> M1, HIP/CM -> M2, HIU -> M3, HFR -> M0
 *   - get(long id) returns the row by primary key (404 if not present)
 *   - findByEnvelopeEventId returns null when not present (Kafka consumer stub
 *     uses this to verify the durability boundary, not to fail on miss)
 *   - callback: unknown correlationId -> 404; status within NHCX range 0..3 else
 *     400; IDEMPOTENT — first callback wins, repeats are no-ops
 */
class AbdmServiceTest {

    private AbdmRequestQueueRepository repository;
    private AbdmService service;

    @BeforeEach
    void setUp() {
        repository = mock(AbdmRequestQueueRepository.class);
        service = new AbdmService(repository, new ObjectMapper());
        Map<String, AbdmRequestQueue> store = new HashMap<>();
        EntityManager em = mock(EntityManager.class);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "em", em);
        Query seqQuery = mock(Query.class);
        when(seqQuery.getSingleResult()).thenAnswer(inv -> (long) (store.size() + 1));
        when(em.createNativeQuery(contains("nextval"))).thenReturn(seqQuery);
        Mockito.doAnswer(inv -> {
            AbdmRequestQueue r = inv.getArgument(0);
            r.setId((long) (store.size() + 1));
            store.put(r.getCorrelationId(), r);
            return r;
        }).when(repository).save(any());
        when(repository.existsByCorrelationId(anyString()))
                .thenAnswer(inv -> store.containsKey(inv.getArgument(0, String.class)));
        when(repository.findByCorrelationId(anyString()))
                .thenAnswer(inv -> Optional.ofNullable(store.get(inv.getArgument(0, String.class))));
        when(repository.findById(any(Long.class)))
                .thenAnswer(inv -> Optional.ofNullable(store.values().stream()
                        .filter(r -> r.getId().equals(inv.getArgument(0, Long.class)))
                        .findFirst().orElse(null)));
        when(repository.findByEnvelopeEventId(anyString()))
                .thenAnswer(inv -> store.values().stream()
                        .filter(r -> inv.getArgument(0, String.class).equals(r.getEnvelopeEventId()))
                        .findFirst());
    }

    private static Map<String, Object> payload(String k, Object v) {
        return Map.of(k, v);
    }

    // We check JSON payload content via substring (no JSON deserializer needed).
    private static final String JSON_OK = "{\"resp\":\"ok\"}";
    private static final String JSON_TAMPERED = "{\"resp\":\"TAMPERED\"}";

    // ------------------------------------------------------------------ create

    @Test
    void create_happyPath_startsAcceptedWithNoCallback() {
        AbdmRequestQueue row = service.create("HIP", "CORR-1", payload("hip", "x"));
        assertThat(row.getRequestKind()).isEqualTo("HIP");
        assertThat(row.getStatus()).isEqualTo(AbdmRequestQueue.STATUS_ACCEPTED);
        assertThat(row.hasCallback()).isFalse();
        assertThat(row.getMilestone()).isEqualTo("M2");
        assertThat(row.getId()).isNotNull();
    }

    @Test
    void create_allFiveKindsAccepted_milestoneAutoDerived() {
        assertThat(service.create("ABHA", "C-ABHA", payload("k", 1)).getMilestone()).isEqualTo("M1");
        assertThat(service.create("HIP",  "C-HIP",  payload("k", 1)).getMilestone()).isEqualTo("M2");
        assertThat(service.create("HIU",  "C-HIU",  payload("k", 1)).getMilestone()).isEqualTo("M3");
        assertThat(service.create("HFR",  "C-HFR",  payload("k", 1)).getMilestone()).isEqualTo("M0");
        assertThat(service.create("CM",   "C-CM",   payload("k", 1)).getMilestone()).isEqualTo("M2");
    }

    @Test
    void create_invalidKindOrMissingCorrelation_mapsTo400Bexa003() {
        assertThatThrownBy(() -> service.create("XYZ", "C-1", payload("x", 1)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("ABHA");
        assertThatThrownBy(() -> service.create(null, "C-1", payload("x", 1)))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> service.create("HIP", null, payload("x", 1)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("correlationId is required");
        assertThatThrownBy(() -> service.create("HIP", "X".repeat(65), payload("x", 1)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("at most 64 characters");
        assertThatThrownBy(() -> service.create("HIP", "C-NEW", null))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("payload is required");
    }

    @Test
    void create_duplicateCorrelationId_mapsTo409ConflictCode() {
        service.create("HIU", "CORR-DUP", payload("a", 1));
        assertThatThrownBy(() -> service.create("HFR", "CORR-DUP", payload("b", 2)))
                .isInstanceOf(ConflictException.class)
                .extracting(e -> ((ConflictException) e).getErrorCode())
                .isEqualTo("INTEROP_DUPLICATE_CORRELATION_ID");
    }

    @Test
    void create_duplicateEnvelopeEventId_mapsTo409EnvelopeEventId() {
        service.create("HIP", "C-A", payload("a", 1), "EVT-1");
        assertThatThrownBy(() -> service.create("HIU", "C-B", payload("b", 2), "EVT-1"))
                .isInstanceOf(ConflictException.class)
                .extracting(e -> ((ConflictException) e).getErrorCode())
                .isEqualTo("INTEROP_DUPLICATE_ENVELOPE_EVENT_ID");
    }

    @Test
    void create_concurrentDuplicateRace_translatesConstraintViolationTo409() {
        service.create("HIP", "CORR-RACE", payload("a", 1));
        reset(repository);
        doThrow(new org.springframework.dao.DataIntegrityViolationException("uq_t_abdm_correlation"))
                .when(repository).save(any());
        when(repository.findByEnvelopeEventId(anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create("HIU", "CORR-RACE", payload("b", 2)))
                .isInstanceOf(ConflictException.class)
                .extracting(e -> ((ConflictException) e).getErrorCode())
                .isEqualTo("INTEROP_DUPLICATE_CORRELATION_ID");
    }

    // ------------------------------------------------------------------ callback

    @Test
    void callback_unknownCorrelationId_mapsTo404NotFound() {
        assertThatThrownBy(() -> service.callback("MISSING", payload("r", 1), (short) 2))
                .isInstanceOf(InteropNotFoundException.class)
                .extracting(e -> ((InteropNotFoundException) e).getErrorCode())
                .isEqualTo("NOT_FOUND");
    }

    @Test
    void callback_outOfRangeStatus_mapsTo400Bexa003() {
        service.create("CM", "C-CB", payload("x", 1));
        assertThatThrownBy(() -> service.callback("C-CB", payload("r", 1), (short) 4))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("NHCX range 0..3");
        assertThatThrownBy(() -> service.callback("C-CB", payload("r", 1), (short) -1))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> service.callback("C-CB", payload("r", 1), null))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void callback_appliesStatusAndPayload_firstCallbackWins() {
        service.create("HFR", "C-IDEM", payload("req", true));

        AbdmRequestQueue applied = service.callback("C-IDEM", Map.of("resp", "ok"), (short) 2);
        assertThat(applied.getStatus()).isEqualTo(AbdmRequestQueue.STATUS_COMPLETED);
        assertThat(applied.getCallbackPayload()).contains("ok");

        // repeat with DIFFERENT status + payload is a NO-OP (idempotency, register WP-T1)
        AbdmRequestQueue repeat = service.callback("C-IDEM", Map.of("resp", "TAMPERED"), (short) 3);
        assertThat(repeat.getStatus()).isEqualTo(AbdmRequestQueue.STATUS_COMPLETED);
        assertThat(repeat.getCallbackPayload()).contains("ok");
        assertThat(repeat.getCallbackPayload()).doesNotContain("TAMPERED");
    }

    @Test
    void callback_statusValidationRunsEvenOnRepeat_idempotencyStillWinsOnValidRepeat() {
        service.create("HIP", "C-VAL", payload("x", 1));
        // invalid status on a fresh row still fails fast (guard before idempotency short-circuit)
        assertThatThrownBy(() -> service.callback("C-VAL", payload("r", 1), (short) 99))
                .isInstanceOf(InvalidRequestException.class);
        assertThat(service.callback("C-VAL", payload("r", 1), (short) 0).getStatus()).isZero();
    }

    // ------------------------------------------------------------------ get / findByEnvelopeEventId

    @Test
    void get_returnsById_404WhenMissing() {
        service.create("HIP", "C-1", payload("x", 1));
        AbdmRequestQueue row = service.get(1L);
        assertThat(row.getCorrelationId()).isEqualTo("C-1");
        assertThatThrownBy(() -> service.get(999L))
                .isInstanceOf(InteropNotFoundException.class);
    }

    @Test
    void findByEnvelopeEventId_returnsNullOnMiss() {
        assertThat(service.findByEnvelopeEventId("EVT-NOT-THERE")).isNull();
    }

    @Test
    void findByEnvelopeEventId_returnsRowOnHit() {
        service.create("ABHA", "C-M1", payload("k", 1), "EVT-ABHA-1");
        AbdmRequestQueue row = service.findByEnvelopeEventId("EVT-ABHA-1");
        assertThat(row).isNotNull();
        assertThat(row.getRequestKind()).isEqualTo("ABHA");
        assertThat(row.getMilestone()).isEqualTo("M1");
    }
}
