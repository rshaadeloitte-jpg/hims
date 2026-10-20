package com.hims.interop.infrastructure.persistence;

import com.hims.interop.application.AbdmService;
import com.hims.interop.application.InboundService;
import com.hims.interop.application.InteropEventService;
import com.hims.interop.application.InteropKafkaConsumer;
import com.hims.interop.application.SmsService;
import com.hims.interop.application.WhatsappService;
import com.hims.interop.application.exceptions.ConflictException;
import com.hims.interop.domain.model.AbdmRequestQueue;
import com.hims.interop.domain.model.SmsNotificationQueue;
import com.hims.interop.infrastructure.outbox.OutboxRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * L2 — MIG-055 integration tests on real PG16: V1+V2 schema applies, the new
 * tables (t_sms_notification_queue, t_dispatch_dlq) and the new columns
 * (milestone, envelope_event_id, consumer_group) are in place, the ABDM
 * ABHA/M1 milestone tag is correctly derived, the Kafka consumer stub
 * dedup is observable at the DB level via envelope_event_id, and the outbox
 * stays EMPTY through EVERY flow (the interop service emits NOTHING).
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = com.hims.interop.InteropServiceApplication.class)
@DirtiesContext
@Transactional
class InteropMig055IntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private InteropEventService eventService;
    @Autowired private InboundService inboundService;
    @Autowired private AbdmService abdmService;
    @Autowired private WhatsappService whatsappService;
    @Autowired private SmsService smsService;
    @Autowired private InteropKafkaConsumer kafkaConsumer;
    @Autowired private OutboxRepository outboxRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @PersistenceContext private EntityManager em;

    private long countRows(String table) {
        Number n = (Number) em.createNativeQuery("SELECT count(*) FROM ihms_interop." + table)
                .getSingleResult();
        return n.longValue();
    }

    private long countSchemaColumns(String table, String... columnNames) {
        // Use JdbcTemplate (which is the proven path) with positional
        // parameters (the ? syntax, 1-based binding).
        String inList = String.join(", ", java.util.Collections.nCopies(columnNames.length, "?"));
        String sql = "SELECT count(*) FROM information_schema.columns "
                + "WHERE table_schema = ? AND table_name = ? AND column_name IN (" + inList + ")";
        Object[] params = new Object[2 + columnNames.length];
        params[0] = "ihms_interop";
        params[1] = table;
        for (int i = 0; i < columnNames.length; i++) {
            params[2 + i] = columnNames[i];
        }
        return jdbcTemplate.queryForObject(sql, Long.class, params);
    }

    private boolean indexExists(String indexName) {
        Long n = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_indexes WHERE schemaname = ? AND indexname = ?",
                Long.class, "ihms_interop", indexName);
        return n != null && n > 0;
    }

    /** V2 schema: every new table and column lands on PG16. */
    @Test
    void v2Schema_allNewTablesAndColumnsPresent() {
        // New tables
        assertThat(countRows("t_sms_notification_queue")).isZero();
        assertThat(countRows("t_sms_notification_status")).isZero();
        assertThat(countRows("t_dispatch_dlq")).isZero();
        // ABDM new columns
        assertThat(countSchemaColumns("t_abdm_request_queue",
                "milestone", "envelope_event_id"))
                .as("ABDM milestone + envelope_event_id columns")
                .isEqualTo(2);
        // InboundHis new columns
        assertThat(countSchemaColumns("t_inbound_his",
                "consumer_group", "envelope_event_id"))
                .as("InboundHis consumer_group + envelope_event_id columns")
                .isEqualTo(2);
        // UNIQUE index on t_inbound_his.envelope_event_id
        assertThat(indexExists("uq_t_inbound_envelope_event"))
                .as("InboundHis UNIQUE envelope_event_id backstop")
                .isTrue();
        // UNIQUE on t_abdm_request_queue.envelope_event_id
        assertThat(indexExists("uq_t_abdm_envelope_event"))
                .as("ABDM UNIQUE envelope_event_id backstop")
                .isTrue();

        // ABDM request_kind now includes ABHA (M1) — verified by the service path.
        AbdmRequestQueue abha = abdmService.create("ABHA", "C-ABHA-M1", Map.of("k", 1));
        assertThat(abha.getRequestKind()).isEqualTo("ABHA");
        assertThat(abha.getMilestone()).isEqualTo("M1");
    }

    @Test
    void smsQueue_forwardOnlyMachineWithExpireTerminal_writesHistoryRows() {
        long queued = smsService.create("appt", "+91981234567", null, Map.of("k", 1)).getId();
        smsService.applyStatus(queued, (short) 1, "PROV-9", null);
        smsService.applyStatus(queued, (short) 2, null, null);
        smsService.applyStatus(queued, (short) 4, null, "ttl");

        SmsNotificationQueue read = smsService.get(queued);
        assertThat(read.getStatus()).isEqualTo(SmsNotificationQueue.STATUS_EXPIRED);
        assertThat(read.getProviderMessageId()).isEqualTo("PROV-9");
        assertThat(read.getHistory())
                .extracting(SmsNotificationStatus -> SmsNotificationStatus.getStatusCode())
                .containsExactly((short) 1, (short) 2, (short) 4);
        assertThat(countRows("t_sms_notification_status")).isEqualTo(3);
    }

    @Test
    void abdm_createWithEnvelopeEventId_dedupedByDb() {
        AbdmRequestQueue first = abdmService.create("HIP", "C-1", Map.of("k", 1), "EVT-FRESH");
        assertThat(first.getEnvelopeEventId()).isEqualTo("EVT-FRESH");
        assertThat(first.getMilestone()).isEqualTo("M2");

        assertThatThrownBy(() -> abdmService.create("HIU", "C-2", Map.of("k", 2), "EVT-FRESH"))
                .isInstanceOf(ConflictException.class)
                .extracting(e -> ((ConflictException) e).getErrorCode())
                .isEqualTo("INTEROP_DUPLICATE_ENVELOPE_EVENT_ID");
    }

    @Test
    void abdm_allFiveMilestonesAutoDerivedAtDb() {
        assertThat(abdmService.create("ABHA", "M-ABHA", Map.of()).getMilestone()).isEqualTo("M1");
        assertThat(abdmService.create("HIP",  "M-HIP",  Map.of()).getMilestone()).isEqualTo("M2");
        assertThat(abdmService.create("CM",   "M-CM",   Map.of()).getMilestone()).isEqualTo("M2");
        assertThat(abdmService.create("HIU",  "M-HIU",  Map.of()).getMilestone()).isEqualTo("M3");
        assertThat(abdmService.create("HFR",  "M-HFR",  Map.of()).getMilestone()).isEqualTo("M0");

        em.flush();   // ensure JPA writes are visible to the JDBC query
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT request_kind, milestone FROM ihms_interop.t_abdm_request_queue " +
                "WHERE correlation_id LIKE ? ORDER BY correlation_id", "M-%");
        assertThat(rows).hasSize(5);
        for (Map<String, Object> r : rows) {
            String kind = (String) r.get("request_kind");
            String ms = (String) r.get("milestone");
            switch (kind) {
                case "ABHA" -> assertThat(ms).isEqualTo("M1");
                case "HIP", "CM" -> assertThat(ms).isEqualTo("M2");
                case "HIU" -> assertThat(ms).isEqualTo("M3");
                case "HFR" -> assertThat(ms).isEqualTo("M0");
                default -> throw new AssertionError("unexpected kind " + kind);
            }
        }
    }

    @Test
    void kafkaConsumer_dedupIsObservableAtDbLevel_zeroMessageLoss() {
        // First delivery from Kafka: a new row lands.
        AbdmRequestQueue first = kafkaConsumer.consumeAbdmRequest(
                "EVT-KAFKA-1", "HIP", "KAFKA-1", Map.of("k", 1));
        assertThat(first.getId()).isNotNull();
        assertThat(first.getCorrelationId()).isEqualTo("KAFKA-1");
        assertThat(first.getMilestone()).isEqualTo("M2");
        assertThat(countRows("t_abdm_request_queue")).isEqualTo(1);

        // Same envelope (Kafka replay) -> existing row returned, no second insert.
        AbdmRequestQueue replay = kafkaConsumer.consumeAbdmRequest(
                "EVT-KAFKA-1", "HIP", "KAFKA-1", Map.of("k", 99));
        assertThat(replay.getId()).isEqualTo(first.getId());
        // The first payload (k=1) is preserved; the new payload is IGNORED.
        assertThat(replay.getPayload()).contains("\"k\":1");
        assertThat(countRows("t_abdm_request_queue")).isEqualTo(1);

        // Inbound HL7 Kafka consumer stamps consumerGroup at the row level so
        // ops can observe the HTTP-to-Kafka cutover.
        var h2 = kafkaConsumer.consumeInboundHl7(
                "EVT-KAFKA-2", "ADT^A01", "HIS-1", "MSH|^~\\&|GHH|ELABs|RIH|src|20240815120000||ADT^A01|MSG-1|P|2.5");
        assertThat(h2.getEnvelopeEventId()).isEqualTo("EVT-KAFKA-2");
        assertThat(h2.getConsumerGroup()).isEqualTo(InteropKafkaConsumer.CONSUMER_GROUP_HL7);
    }

    @Test
    void negativeOutbox_staysEmptyThroughEveryFlow() {
        abdmService.create("ABHA", "N-1", Map.of("k", 1));
        whatsappService.create("otp", "+91", Map.of());
        smsService.create("otp", "+91", null, Map.of());
        inboundService.receive("ADT^A01", "HIS-1", "MSH|...|ADT^A01|1|P|2.5");
        eventService.ingest("InvoiceGenerated", "B-N-1", Map.of());
        assertThat(outboxRepository.count()).isZero();
    }
}
