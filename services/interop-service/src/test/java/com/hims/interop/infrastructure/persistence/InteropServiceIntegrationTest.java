package com.hims.interop.infrastructure.persistence;

import com.hims.interop.InteropServiceApplication;
import com.hims.interop.application.AbdmService;
import com.hims.interop.application.InboundService;
import com.hims.interop.application.InteropEventService;
import com.hims.interop.application.WhatsappService;
import com.hims.interop.application.exceptions.ConflictException;
import com.hims.interop.domain.model.AbdmRequestQueue;
import com.hims.interop.domain.model.InboundHis;
import com.hims.interop.domain.model.OutboundHis;
import com.hims.interop.domain.model.WhatsappMessageQueue;
import com.hims.interop.infrastructure.outbox.OutboxRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * L2 — WP-T1 interop service on real PG16: V1 schema applies (ihms_interop:
 * t_outbound_his, t_inbound_his, t_abdm_request_queue, t_whatsapp_message_queue,
 * t_whatsapp_message_status, t_ehr_job_details, outbox), the internal event-relay
 * ingress routes + de-duplicates against the DB (uq_t_outbound_event), ABDM
 * correlation uniqueness is pinned at DB level (uq_t_abdm_correlation), the
 * WhatsApp forward-only machine writes history rows — and, per register WP-T1
 * EVENT POLICY, the outbox stays EMPTY through EVERY flow (the service emits
 * NOTHING; explicit NEGATIVE outbox assertion).
 * NOTE: requires Docker; skipped automatically when Docker is absent
 * (@Testcontainers(disabledWithoutDocker = true)) — do not remove.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = InteropServiceApplication.class)
@DirtiesContext
@Transactional
class InteropServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private InteropEventService eventService;
    @Autowired private InboundService inboundService;
    @Autowired private AbdmService abdmService;
    @Autowired private WhatsappService whatsappService;
    @Autowired private OutboxRepository outboxRepository;
    @Autowired private EntityManager em;

    @SuppressWarnings("unchecked")
    private List<String> tables() {
        return em.createNativeQuery("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = 'ihms_interop' ORDER BY table_name
                """).getResultList();
    }

    /** Commit the surrounding tx and open a fresh one (for raw-SQL constraint probes). */
    private void commitAndRestartTx() {
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();
    }

    /** Discard an aborted tx (constraint probe) and open a fresh one for the rest of the test. */
    private void discardAbortedTxAndRestart() {
        TestTransaction.end();   // discards the aborted tx
        em.clear();
        TestTransaction.start();
    }

    private long countRows(String table) {
        Number n = (Number) em.createNativeQuery("SELECT count(*) FROM ihms_interop." + table)
                .getSingleResult();
        return n.longValue();
    }

    @Test
    void flywaySchema_applies_allTablesAndSequencesPresent() {
        assertThat(tables()).contains("outbox", "t_outbound_his", "t_inbound_his",
                "t_abdm_request_queue", "t_whatsapp_message_queue",
                "t_whatsapp_message_status", "t_ehr_job_details");
        Number seqCount = (Number) em.createNativeQuery("""
                SELECT count(*) FROM information_schema.sequences
                WHERE sequence_schema = 'ihms_interop'
                  AND sequence_name IN ('t_outbound_his_seq', 't_inbound_his_seq',
                      't_abdm_request_queue_seq', 't_whatsapp_message_queue_seq',
                      't_whatsapp_message_status_seq', 't_ehr_job_details_seq')
                """).getSingleResult();
        assertThat(seqCount.intValue()).isEqualTo(6);
        // EVENT POLICY: nothing has touched the outbox during migration/boot
        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void eventIngress_routesAndIsIdempotent_withNegativeOutboxAssertions() {
        assertThat(outboxRepository.count()).isZero();

        // routing: revenue -> HL7, lab -> EHR, ADT feed -> HL7
        InteropEventService.IngestResult invoice =
                eventService.ingest("InvoiceGenerated", "BILL-1001", Map.of("billId", 1001, "total", 500));
        InteropEventService.IngestResult lab =
                eventService.ingest("LabResultFinalized", "LR-77", Map.of("resultId", 77));
        InteropEventService.IngestResult admitted =
                eventService.ingest("PatientAdmitted", "ADM-5", Map.of("admissionId", 5));

        assertThat(invoice.row().getTargetSystem()).isEqualTo("HL7");
        assertThat(lab.row().getTargetSystem()).isEqualTo("EHR");
        assertThat(admitted.row().getTargetSystem()).isEqualTo("HL7");
        assertThat(invoice.created()).isTrue();
        assertThat(countRows("t_outbound_his")).isEqualTo(3);

        // DB-level backstop: uq_t_outbound_event rejects a raw second insert too.
        // Run in its own committed tx so the constraint failure does not poison
        // this test's surrounding transaction (PG aborts on first error).
        commitAndRestartTx();
        try {
            assertThatThrownBy(() -> {
                em.createNativeQuery("""
                    INSERT INTO ihms_interop.t_outbound_his
                        (id, event_type, aggregate_id, payload, target_system)
                    VALUES (nextval('ihms_interop.t_outbound_his_seq'), 'InvoiceGenerated', 'BILL-1001', '{}', 'HL7')
                    """).executeUpdate();
            }).isInstanceOf(Exception.class);   // uq_t_outbound_event
        } finally {
            discardAbortedTxAndRestart();
        }

        // IDEMPOTENCY: duplicate ingress returns the SAME row, no duplicate written.
        // The payload differs on purpose — the idempotency key wins.
        InteropEventService.IngestResult dupInvoice =
                eventService.ingest("InvoiceGenerated", "BILL-1001", Map.of("billId", 1001, "total", 999_999));
        assertThat(dupInvoice.created()).isFalse();
        assertThat(dupInvoice.row().getId()).isEqualTo(invoice.row().getId());
        // jsonb round-trip is normalized by PG (spacing/order); assert values survive
        assertThat(dupInvoice.row().getPayload()).contains("1001").doesNotContain("999_999");
        assertThat(countRows("t_outbound_his")).isEqualTo(3);


        // unknown eventType -> 400 code, nothing written
        assertThatThrownBy(() -> eventService.ingest("SomethingRandom", "A-1", Map.of("x", 1)))
                .extracting(e -> ((com.hims.interop.application.exceptions.InvalidRequestException) e).getErrorCode())
                .isEqualTo("INTEROP_UNKNOWN_EVENT_TYPE");

        // list filters hit the read model
        assertThat(eventService.list((short) OutboundHis.STATUS_PENDING, null)).hasSize(3);
        assertThat(eventService.list(null, "EHR"))
                .allSatisfy(r -> assertThat(r.getTargetSystem()).isEqualTo("EHR"));

        // NEGATIVE OUTBOX: ingress is pure consumption — nothing emitted anywhere
        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void abdm_correlationUniquenessPinnedAtDb_andCallbackIdempotent_withNegativeOutboxAssertion() {
        AbdmRequestQueue created = abdmService.create("HIP", "CORR-42", Map.of("purpose", "LINK"));
        assertThat(created.getStatus()).isEqualTo(AbdmRequestQueue.STATUS_ACCEPTED);
        assertThat(created.hasCallback()).isFalse();

        // DB-level uniqueness: raw duplicate insert violates uq_t_abdm_correlation.
        // Run in its own committed tx BEFORE any 409 assertion poisons this test's
        // surrounding transaction with rollback-only.
        commitAndRestartTx();
        try {
            assertThatThrownBy(() -> {
                em.createNativeQuery("""
                    INSERT INTO ihms_interop.t_abdm_request_queue
                        (id, request_kind, correlation_id, status, milestone)
                    VALUES (nextval('ihms_interop.t_abdm_request_queue_seq'), 'HIU', 'CORR-42', 0, 'M3')
                    """).executeUpdate();
            }).isInstanceOf(Exception.class);   // uq_t_abdm_correlation
        } finally {
            discardAbortedTxAndRestart();
        }

        assertThat(countRows("t_abdm_request_queue")).isEqualTo(1);

        // API-level 409
        assertThatThrownBy(() -> abdmService.create("HIU", "CORR-42", Map.of()))
                .isInstanceOf(ConflictException.class)
                .extracting(e -> ((ConflictException) e).getErrorCode())
                .isEqualTo("INTEROP_DUPLICATE_CORRELATION_ID");

        // callback applies once...
        AbdmRequestQueue applied = abdmService.callback("CORR-42", Map.of("resp", "ok"), (short) 2);
        assertThat(applied.getStatus()).isEqualTo(AbdmRequestQueue.STATUS_COMPLETED);
        assertThat(applied.getCallbackPayload()).contains("\"resp\":\"ok\"");

        // ...and repeats are NO-OPS even with different data (first callback wins)
        AbdmRequestQueue repeat = abdmService.callback("CORR-42", Map.of("resp", "TAMPERED"), (short) 3);
        assertThat(repeat.getStatus()).isEqualTo(AbdmRequestQueue.STATUS_COMPLETED);
        assertThat(repeat.getCallbackPayload()).contains("\"resp\":\"ok\"");

        // unknown correlationId -> 404 code
        assertThatThrownBy(() -> abdmService.callback("GHOST", Map.of(), (short) 0))
                .extracting(e -> ((com.hims.interop.application.exceptions.InteropNotFoundException) e).getErrorCode())
                .isEqualTo("NOT_FOUND");

        // NEGATIVE OUTBOX assertion
        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void whatsapp_forwardOnlyMachine_writesHistory_withNegativeOutboxAssertion() {
        long queued = whatsappService.create("appt_reminder", "+91981234567", Map.of("name", "A")).getId();

        // full forward path through real persistence
        whatsappService.applyStatus(queued, (short) 1, "PROV-9", null);
        whatsappService.applyStatus(queued, (short) 2, null, null);
        whatsappService.applyStatus(queued, (short) 3, null, null);

        WhatsappMessageQueue read = whatsappService.get(queued);
        assertThat(read.getStatus()).isEqualTo(WhatsappMessageQueue.STATUS_READ);
        assertThat(read.getProviderMessageId()).isEqualTo("PROV-9");   // stamped at SENT, kept after
        assertThat(read.getHistory())
                .extracting(com.hims.interop.domain.model.WhatsappMessageStatus::getStatusCode)
                .containsExactly((short) 1, (short) 2, (short) 3);
        assertThat(countRows("t_whatsapp_message_status")).isEqualTo(3);

        // terminal READ accepts nothing further
        for (short target : new short[]{1, 2, 3, 4}) {
            short t = target;
            assertThatThrownBy(() -> whatsappService.applyStatus(queued, t, null, null))
                    .extracting(e -> ((com.hims.interop.application.exceptions.InvalidRequestException) e).getErrorCode())
                    .isEqualTo("INTEROP_ILLEGAL_TRANSITION");
        }
        // FAILED from DELIVERED would have been illegal too (covered at L0 + machine rules)

        // FAILED path from QUEUED
        long failed = whatsappService.create("otp", "+91981112233", null).getId();
        whatsappService.applyStatus(failed, (short) 4, null, "provider rejected");
        assertThat(whatsappService.list((short) WhatsappMessageQueue.STATUS_FAILED))
                .hasSize(1);
        assertThat(countRows("t_whatsapp_message_status")).isEqualTo(4);   // 3 forward + 1 fail history

        // NEGATIVE OUTBOX assertion
        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void inboundReceive_landsReceived_andHealthJobsReadModelIsEmpty_withNegativeOutboxAssertion() {
        InboundHis row = inboundService.receive("ADT^A01", "HIS-LEGACY", "MSH|^~\\&|GHH|...");
        assertThat(row.getStatus()).isEqualTo(InboundHis.STATUS_RECEIVED);
        assertThat(row.getProcessedAt()).isNull();   // dispatcher OUT OF SCOPE (comment-only)
        assertThat(countRows("t_inbound_his")).isEqualTo(1);

        // unsupported type rejected, nothing persisted
        assertThatThrownBy(() -> inboundService.receive("ADT^A99", "X", "MSH"))
                .extracting(e -> ((com.hims.interop.application.exceptions.InvalidRequestException) e).getErrorCode())
                .isEqualTo("INTEROP_INVALID_MESSAGE_TYPE");
        assertThat(countRows("t_inbound_his")).isEqualTo(1);

        // EHR job read model exists but has NO phase-1 write path (dispatcher slice
        // OUT OF SCOPE — disclosed in WP report): empty until dispatchers land.
        assertThat(countRows("t_ehr_job_details")).isZero();

        // NEGATIVE OUTBOX: final explicit assertion — every flow above emitted NOTHING
        assertThat(outboxRepository.count()).isZero();
    }
}
