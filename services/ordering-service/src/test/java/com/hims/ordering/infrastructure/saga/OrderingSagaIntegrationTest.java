package com.hims.ordering.infrastructure.saga;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.ordering.OrderingServiceApplication;
import com.hims.ordering.application.OrderService;
import com.hims.ordering.application.PrescriptionService;
import com.hims.ordering.domain.model.ClinicalOrder;
import com.hims.ordering.domain.model.SagaState;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * L2 — MIG-033 saga on real PG16 + Kafka (Docker required; skipped without it):
 *   1. Order placement writes clinical_order + order_item + saga_state + ONE
 *      OrderInitiated outbox row in a SINGLE transaction.
 *   2. Confirm choreography: all applicable steps SUCCEEDED -> order CONFIRMED,
 *      saga COMPLETED.
 *   3. Lab failure compensates the succeeded BILLING step, flips the order FAILED
 *      and writes the OrderFailed event in the SAME tx (no lost-update window).
 *   4. Cancel after partial confirmation compensates and emits OrderFailed too.
 *   5. Multi-item prescription + BR-ORD-005 rejection rolls back EVERYTHING.
 *   6. Kafka roundtrip: relay publishes OrderInitiated / OrderFailed /
 *      PrescriptionCreated to ihms.ordering.events and marks rows PUBLISHED.
 * NOTE: no class-level @Transactional — saga events must COMMIT for the relay
 * (IMP-DEC-038 lesson); each test cleans its own tables in FK order instead.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = OrderingServiceApplication.class)
@DirtiesContext
class OrderingSagaIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection
    static KafkaContainer kafka =
            new KafkaContainer(org.testcontainers.utility.DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private OrderService orderService;

    @Autowired
    private PrescriptionService prescriptionService;

    @Autowired
    private JdbcTemplate jdbc;

    /** Kafka consumer used for the roundtrip test (lazy; only test 6 touches Kafka). */
    private KafkaConsumer<String, String> consumer;

    @BeforeEach
    void cleanTables() {
        // FK order: outbox -> saga_state -> order_item -> clinical_order;
        // prescription_item -> prescription
        jdbc.update("DELETE FROM ihms_ordering.outbox");
        jdbc.update("DELETE FROM ihms_ordering.saga_state");
        jdbc.update("DELETE FROM ihms_ordering.order_item");
        jdbc.update("DELETE FROM ihms_ordering.clinical_order");
        jdbc.update("DELETE FROM ihms_ordering.prescription_item");
        jdbc.update("DELETE FROM ihms_ordering.prescription");
    }

    @AfterEach
    void closeConsumer() {
        if (consumer != null) {
            consumer.close();
            consumer = null;
        }
    }

    // ------------------------------------------------------------------ helpers

    private long placeLabOrder() {
        ClinicalOrder o = orderService.create(501L, 7001L, null, 11L, 2L,
                new BigDecimal("10"), null,
                List.of(new OrderService.ItemInput("LAB", 10L, "CBC",
                                new BigDecimal("500"), 1, new BigDecimal("10")),
                        new OrderService.ItemInput("CONSULTATION", 20L, "OPD consult",
                                new BigDecimal("100"), 2, null)));
        return o.getOrderId();
    }

    private Map<String, Object> orderRow(long orderId) {
        return jdbc.queryForMap(
                "SELECT * FROM ihms_ordering.clinical_order WHERE order_id = ?", orderId);
    }

    private Map<String, Object> sagaRow(long orderId) {
        return jdbc.queryForMap(
                "SELECT * FROM ihms_ordering.saga_state WHERE order_id = ?", orderId);
    }

    private List<Map<String, Object>> outboxRows(String eventType) {
        return jdbc.queryForList(
                "SELECT * FROM ihms_ordering.outbox WHERE event_type = ? ORDER BY id", eventType);
    }

    private JsonNode payloadOf(Map<String, Object> row) throws Exception {
        return MAPPER.readTree(row.get("payload").toString());
    }

    // ------------------------------------------------------------------ 1

    @Test
    void placementWritesOrderItemsSagaAndOrderInitiatedAtomically() throws Exception {
        long id = placeLabOrder();

        Map<String, Object> row = orderRow(id);
        assertThat(row.get("status_code")).isEqualTo(1);   // INITIATED
        assertThat(new BigDecimal(row.get("gross_total").toString()))
                .isEqualByComparingTo("700.00");
        // concession 10% of 700 HALF_UP = 70.00
        assertThat(new BigDecimal(row.get("concession_amount").toString()))
                .isEqualByComparingTo("70.00");
        assertThat(new BigDecimal(row.get("net_total").toString()))
                .isEqualByComparingTo("630.00");

        List<Map<String, Object>> items = jdbc.queryForList(
                "SELECT item_type, item_id, service_name, rate, qty, doctor_share_percent"
                        + " FROM ihms_ordering.order_item WHERE order_id = ? ORDER BY line_no", id);
        assertThat(items).hasSize(2);
        assertThat(items.get(0).get("item_type")).isEqualTo("LAB");
        assertThat(items.get(0).get("service_name")).isEqualTo("CBC");
        assertThat(new BigDecimal(items.get(1).get("doctor_share_percent").toString()))
                .isEqualByComparingTo("0");   // BR-ORD-004 default

        Map<String, Object> saga = sagaRow(id);
        assertThat(saga.get("saga_status")).isEqualTo("IN_PROGRESS");
        assertThat(saga.get("billing_step")).isEqualTo("PENDING");
        assertThat(saga.get("lab_step")).isEqualTo("PENDING");
        assertThat(saga.get("radiology_step")).isNull();   // N/A — no RADIOLOGY items
        assertThat(saga.get("pharmacy_step")).isNull();    // N/A — no linked prescription

        // exactly ONE OrderInitiated row, PENDING, partition key encounterId
        List<Map<String, Object>> initiated = outboxRows("OrderInitiated");
        assertThat(initiated).hasSize(1);
        assertThat(initiated.get(0).get("status")).isEqualTo("PENDING");
        assertThat(initiated.get(0).get("partition_key")).isEqualTo("7001");
        assertThat(initiated.get(0).get("producer_service")).isEqualTo("ordering-service");

        JsonNode payload = payloadOf(initiated.get(0));
        assertThat(payload.get("orderId").asText()).isEqualTo(String.valueOf(id));
        assertThat(payload.get("items")).hasSize(2);
        // jsonb normalizes NUMERIC scale (10.00 -> 10.0) — compare values, not lexemes
        assertThat(payload.get("items").get(0).get("doctorSharePercent").decimalValue())
                .isEqualByComparingTo("10");
        assertThat(payload.get("netTotal").decimalValue()).isEqualByComparingTo("630");
        assertThat(payload.get("pharmacyPrescriptionId").isNull()).isTrue();
    }

    // ------------------------------------------------------------------ 2

    @Test
    void confirmingAllStepsCompletesSagaAndConfirmsOrder() {
        long id = placeLabOrder();
        orderService.confirmStep(id, SagaState.BILLING);

        ClinicalOrder midFlight = orderService.get(id);
        assertThat(midFlight.getStatusCode()).isEqualTo(ClinicalOrder.STATUS_INITIATED); // LAB still pending

        orderService.confirmStep(id, SagaState.LAB);

        ClinicalOrder confirmed = orderService.get(id);
        assertThat(confirmed.getStatusCode()).isEqualTo(ClinicalOrder.STATUS_CONFIRMED);
        assertThat(confirmed.getSaga().getSagaStatus()).isEqualTo(SagaState.COMPLETED);
        assertThat(orderRow(id).get("status_code")).isEqualTo(2);
        assertThat(sagaRow(id).get("saga_status")).isEqualTo("COMPLETED");
        // no compensation event on success
        assertThat(outboxRows("OrderFailed")).isEmpty();
    }

    // ------------------------------------------------------------------ 3

    @Test
    void labFailureCompensatesBillingAndEmitsOrderFailedSameTx() throws Exception {
        long id = placeLabOrder();
        orderService.confirmStep(id, SagaState.BILLING);   // billing already reserved the charge

        ClinicalOrder failed = orderService.failStep(id, SagaState.LAB, "PACS unreachable");

        assertThat(failed.getStatusCode()).isEqualTo(ClinicalOrder.STATUS_FAILED);
        assertThat(failed.getRejectionReason()).isEqualTo("PACS unreachable");

        Map<String, Object> saga = sagaRow(id);
        assertThat(saga.get("saga_status")).isEqualTo("FAILED");
        assertThat(saga.get("failed_step")).isEqualTo("LAB");
        assertThat(saga.get("failure_reason")).isEqualTo("PACS unreachable");
        assertThat(saga.get("billing_step")).isEqualTo("COMPENSATED");   // reservation rolled back
        assertThat(saga.get("lab_step")).isEqualTo("COMPENSATED");

        // OrderInitiated AND OrderFailed both present, both PENDING (same-tx integrity)
        assertThat(outboxRows("OrderInitiated")).hasSize(1);
        List<Map<String, Object>> failedEvents = outboxRows("OrderFailed");
        assertThat(failedEvents).hasSize(1);
        assertThat(failedEvents.get(0).get("partition_key")).isEqualTo("7001");
        JsonNode payload = payloadOf(failedEvents.get(0));
        assertThat(payload.get("failedStep").asText()).isEqualTo("LAB");
        assertThat(payload.get("reason").asText()).isEqualTo("PACS unreachable");
        assertThat(payload.get("compensatedSteps").toString()).contains("BILLING");
    }

    // ------------------------------------------------------------------ 4

    @Test
    void cancelAfterPartialConfirmationCompensatesAndEmitsOrderFailed() throws Exception {
        long id = placeLabOrder();
        orderService.confirmStep(id, SagaState.BILLING);

        ClinicalOrder cancelled = orderService.cancel(id);

        assertThat(cancelled.getStatusCode()).isEqualTo(ClinicalOrder.STATUS_CANCELLED);
        assertThat(cancelled.getSaga().getSagaStatus()).isEqualTo(SagaState.CANCELLED);
        assertThat(sagaRow(id).get("billing_step")).isEqualTo("COMPENSATED");

        List<Map<String, Object>> failedEvents = outboxRows("OrderFailed");
        assertThat(failedEvents).hasSize(1);
        JsonNode payload = payloadOf(failedEvents.get(0));
        assertThat(payload.get("failedStep").asText()).isEqualTo("CANCELLED_BY_USER");
    }

    @Test
    void cancelGuardRejectsConfirmedOrder() {
        long id = placeLabOrder();
        orderService.confirmStep(id, SagaState.BILLING);
        orderService.confirmStep(id, SagaState.LAB);
        assertThatThrownBy(() -> orderService.cancel(id))
                .hasMessageContaining("only INITIATED orders can be cancelled");
    }

    // ------------------------------------------------------------------ 5

    @Test
    void multiItemPrescriptionPersistsAndBrOrd005RejectsAtomically() {
        Long rxId = prescriptionService.create(601L, null, 8001L, 12L, 3L,
                List.of(new PrescriptionService.LineInput(10L, new BigDecimal("2.5"), "500mg", "TDS"),
                        new PrescriptionService.LineInput(11L, new BigDecimal("1"), "10ml", "OD")))
                .getPrescriptionId();

        Integer rxCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM ihms_ordering.prescription WHERE prescription_id = ?", Integer.class, rxId);
        Integer itemCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM ihms_ordering.prescription_item WHERE prescription_id = ?", Integer.class, rxId);
        assertThat(rxCount).isEqualTo(1);
        assertThat(itemCount).isEqualTo(2);
        // E-06 with admission fallback partition key
        List<Map<String, Object>> e06 = outboxRows("PrescriptionCreated");
        assertThat(e06).hasSize(1);
        assertThat(e06.get(0).get("partition_key")).isEqualTo("8001");

        int beforeOrders = count("ihms_ordering.clinical_order");
        int beforeItems = count("ihms_ordering.order_item");
        int beforeOutbox = count("ihms_ordering.outbox");

        // BR-ORD-005 violation (missing dose) must roll back header+lines+event atomically
        assertThatThrownBy(() -> prescriptionService.create(601L, 9001L, null, 12L, 3L,
                List.of(new PrescriptionService.LineInput(10L, new BigDecimal("1"), null, "TDS"))))
                .hasMessageContaining("dose is required");

        assertThat(count("ihms_ordering.clinical_order")).isEqualTo(beforeOrders);
        assertThat(count("ihms_ordering.order_item")).isEqualTo(beforeItems);
        assertThat(count("ihms_ordering.outbox")).isEqualTo(beforeOutbox);
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }

    // ------------------------------------------------------------------ 6

    @Test
    void kafkaRoundtripDeliversAllThreeEventTypesAndPublishesOutbox() throws Exception {
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "mig033-test-" + System.nanoTime());
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(List.of("ihms.ordering.events"));

        long id = placeLabOrder();                       // OrderInitiated
        orderService.failStep(id, SagaState.LAB, "integration failure");   // OrderFailed
        prescriptionService.create(601L, 9002L, null, 12L, 3L,
                List.of(new PrescriptionService.LineInput(10L, new BigDecimal("1"), "5mg", "OD")));  // E-06

        var seen = new java.util.HashSet<String>();
        JsonNode initiatedEnvelope = null;
        Instant deadline = Instant.now().plusSeconds(60);
        while (seen.size() < 3 && Instant.now().isBefore(deadline)) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
            for (ConsumerRecord<String, String> r : records) {
                JsonNode envelope = MAPPER.readTree(r.value());
                String type = envelope.get("eventType").asText();
                seen.add(type);
                if ("OrderInitiated".equals(type)) {
                    initiatedEnvelope = envelope;
                }
            }
        }
        assertThat(seen).containsExactlyInAnyOrder("OrderInitiated", "OrderFailed", "PrescriptionCreated");

        // envelope contract per KAFKA-EVENT-CATALOG §4
        assertThat(initiatedEnvelope.get("schemaName").asText()).isEqualTo("order-initiated.v1");
        assertThat(initiatedEnvelope.get("producerService").asText()).isEqualTo("ordering-service");
        assertThat(initiatedEnvelope.get("version").asText()).isEqualTo("v1");
        assertThat(initiatedEnvelope.get("partitionKey").asText()).isEqualTo("7001");
        assertThat(initiatedEnvelope.get("payload").get("orderId").asText())
                .isEqualTo(String.valueOf(id));

        // relay marked every dispatched row PUBLISHED
        Integer pending = jdbc.queryForObject(
                "SELECT COUNT(*) FROM ihms_ordering.outbox WHERE status <> 'PUBLISHED'", Integer.class);
        assertThat(pending).isZero();
    }
}
