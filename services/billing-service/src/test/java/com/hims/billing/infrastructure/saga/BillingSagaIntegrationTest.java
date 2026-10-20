package com.hims.billing.infrastructure.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.billing.BillingServiceApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * L2 — WP-S1 order-saga CONSUMER side on real PG16: the internal event-relay
 * ingress (POST /api/v1/internal/events — TRANSPORT STUB for the future Kafka
 * listener on ihms.ordering.events) is fed the EXACT envelope that ordering's
 * OutboxPublisher publishes (eventId/eventType/version/partitionKey/payload):
 *   1. first delivery -> REAL bill DRAFT + one t_charge per line, ledger APPLIED,
 *      and NOTHING emitted to ihms_billing.outbox (ingress emits nothing);
 *   2. redelivery of the SAME eventId -> no-op asserted at DB level (single
 *      draft, single charge set, one ledger row; DB UNIQUE backstops exist);
 *   3. invalid payload -> structured REJECTED rejection recorded on the ledger,
 *      ZERO bill/charge rows, replay of the rejected eventId stays rejected;
 *   plus unknown eventType 400 and default-deny on the internal endpoint.
 * NOTE: requires Docker; skipped automatically when Docker is absent
 * (@Testcontainers(disabledWithoutDocker = true)) — do not remove.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = BillingServiceApplication.class)
@AutoConfigureMockMvc
@DirtiesContext
@Transactional
class BillingSagaIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private com.hims.billing.infrastructure.outbox.OutboxRepository outboxRepository;

    /**
     * TRANSPORT STUB helper: builds the envelope EXACTLY as ordering-service
     * OutboxPublisher.publish() sends it to ihms.ordering.events.
     */
    static Map<String, Object> envelope(String eventId) throws Exception {
        Map<String, Object> line1 = new LinkedHashMap<>();
        line1.put("itemId", "901");
        line1.put("serviceName", "CBC Panel");
        line1.put("rate", 250.00);
        line1.put("qty", 1);
        Map<String, Object> line2 = new LinkedHashMap<>();
        line2.put("itemId", "902");
        line2.put("serviceName", "X-Ray Chest");
        line2.put("rate", 1000);
        line2.put("qty", 2);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", "501");
        payload.put("patientId", "505");
        payload.put("encounterId", "7777");
        payload.put("admissionId", null);            // unsupplied context slot travels as explicit null
        payload.put("lines", List.of(line1, line2));
        payload.put("prescriberId", "9");
        payload.put("wardId", "3");

        Map<String, Object> env = new LinkedHashMap<>();
        env.put("eventId", eventId);
        env.put("eventType", "OrderCreated");
        env.put("version", "v1");
        env.put("occurredAt", "2026-08-23T10:00:00Z");
        env.put("producerService", "ordering-service");
        env.put("entityId", 501L);
        env.put("partitionKey", "7777");             // encounterId fallback chain per E-13
        env.put("idempotencyKey", "order-created:501");
        env.put("schemaName", "order-created.v1");
        env.put("payload", payload);
        return env;
    }

    /** Deliver one envelope to the internal ingress with relay auth. */
    private MvcResult deliver(Map<String, Object> body) throws Exception {
        return mockMvc.perform(post("/api/v1/internal/events")
                        .with(SecurityMockMvcRequestPostProcessors.user("event-relay")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(body)))
                .andReturn();
    }

    // --------------------------------------------------------------- happy path

    @Test
    void deliverOnce_createsRealDraftRows_andEmitsNothing() throws Exception {
        assertThat(outboxRepository.count()).isZero();

        String eventId = UUID.randomUUID().toString();
        var response = mockMvc.perform(post("/api/v1/internal/events")
                        .with(SecurityMockMvcRequestPostProcessors.user("event-relay")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(envelope(eventId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPLIED"))
                .andExpect(jsonPath("$.sourceOrderId").value(501))
                .andReturn();
        long billId = com.jayway.jsonpath.JsonPath.parse(response.getResponse().getContentAsString())
                .read("$.billId", Long.class);

        // ---- real bill DRAFT row at DB level ---------------------------------
        Map<String, Object> bill = jdbcTemplate.queryForMap(
                "SELECT patient_id, encounter_id, admission_id, prescriber_doctor_id, bill_no, "
              + "is_interim, is_settled, is_cancelled, saga_source_order_id, saga_event_id "
              + "FROM ihms_billing.t_bill WHERE bill_id = ?", billId);
        assertThat(String.valueOf(bill.get("patient_id"))).isEqualTo("505");
        assertThat(String.valueOf(bill.get("encounter_id"))).isEqualTo("7777");
        assertThat(bill.get("admission_id")).isNull();
        assertThat(String.valueOf(bill.get("prescriber_doctor_id"))).isEqualTo("9");
        assertThat(String.valueOf(bill.get("bill_no"))).isEqualTo("000000000000000");   // BR-BILL-001 placeholder
        assertThat(Boolean.valueOf(String.valueOf(bill.get("is_interim")))).isTrue();   // DRAFT == interim
        assertThat(Boolean.valueOf(String.valueOf(bill.get("is_settled")))).isFalse();
        assertThat(String.valueOf(bill.get("saga_source_order_id"))).isEqualTo("501");
        assertThat(String.valueOf(bill.get("saga_event_id"))).isEqualTo(eventId);

        // ---- one real charge per E-13 line ------------------------------------
        Integer chargeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ihms_billing.t_charge WHERE bill_id = ?", Integer.class, billId);
        assertThat(chargeCount).isEqualTo(2);
        Map<String, Object> c1 = jdbcTemplate.queryForMap(
                "SELECT service_id, service_name, rate, qty FROM ihms_billing.t_charge "
              + "WHERE bill_id = ? AND service_id = 901", billId);
        assertThat(c1.get("service_name")).isEqualTo("CBC Panel");
        assertThat(new java.math.BigDecimal(String.valueOf(c1.get("rate"))))
                .isEqualByComparingTo("250.00");
        assertThat(String.valueOf(c1.get("qty"))).isEqualTo("1");

        // ---- ledger APPLIED ---------------------------------------------------
        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM ihms_billing.t_consumed_event WHERE event_id = ?",
                String.class, eventId);
        assertThat(status).isEqualTo("APPLIED");

        // ---- the ingress EMITS NOTHING (E-07 stays exclusively on finalize) ---
        assertThat(outboxRepository.count()).isZero();
    }

    // --------------------------------------------------------------- idempotency

    @Test
    void redeliverSameEventId_noOpAtDbLevel_backstoppedByUniqueConstraints() throws Exception {
        assertThat(outboxRepository.count()).isZero();

        String eventId = UUID.randomUUID().toString();
        Map<String, Object> body = envelope(eventId);
        deliver(body);
        deliver(body);   // duplicate replay

        // second delivery answered DUPLICATE 200 by the fast path
        mockMvc.perform(post("/api/v1/internal/events")
                        .with(SecurityMockMvcRequestPostProcessors.user("event-relay")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DUPLICATE"));

        // DB level: EXACTLY ONE draft + ONE charge set + ONE ledger row
        Integer billCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ihms_billing.t_bill WHERE saga_event_id = ?",
                Integer.class, eventId);
        assertThat(billCount).isEqualTo(1);
        Integer chargeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ihms_billing.t_charge c JOIN ihms_billing.t_bill b "
              + "ON c.bill_id = b.bill_id WHERE b.saga_event_id = ?", Integer.class, eventId);
        assertThat(chargeCount).isEqualTo(2);
        Integer ledgerCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ihms_billing.t_consumed_event WHERE event_id = ?",
                Integer.class, eventId);
        assertThat(ledgerCount).isEqualTo(1);
        assertThat(outboxRepository.count()).isZero();

        // race backstops physically present in the schema:
        Integer uqLedger = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.table_constraints "
              + "WHERE table_schema='ihms_billing' AND table_name='t_consumed_event' "
              + "AND constraint_type='UNIQUE' AND constraint_name='uq_t_consumed_event_event_id'",
                Integer.class);
        assertThat(uqLedger).as("ledger event_id UNIQUE backstop").isEqualTo(1);
        Integer uqBill = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes "
              + "WHERE schemaname='ihms_billing' AND indexname='uq_t_bill_saga_event'", Integer.class);
        assertThat(uqBill).as("bill saga_event_id partial UNIQUE backstop").isEqualTo(1);
    }

    // --------------------------------------------------------------- rejection

    @Test
    void invalidPayload_structuredRejection_noBillRows_replayStaysRejected() throws Exception {
        assertThat(outboxRepository.count()).isZero();

        // zero-line order — exactly what the producer-side guard forbids too
        String eventId = UUID.randomUUID().toString();
        Map<String, Object> body = envelope(eventId);
        ((Map<String, Object>) body.get("payload")).put("lines", List.of());

        mockMvc.perform(post("/api/v1/internal/events")
                        .with(SecurityMockMvcRequestPostProcessors.user("event-relay")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(body)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.errorCode").value("BILL_REQ_INVALID"))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("At least one charge is required")));

        // structured failure recorded, NO bill/charge rows anywhere
        Integer billCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ihms_billing.t_bill WHERE saga_event_id = ?",
                Integer.class, eventId);
        assertThat(billCount).isZero();
        Integer totalBills = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ihms_billing.t_bill", Integer.class);
        assertThat(totalBills).as("no draft rows at all").isZero();
        Integer totalCharges = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ihms_billing.t_charge", Integer.class);
        assertThat(totalCharges).isZero();
        String stored = jdbcTemplate.queryForObject(
                "SELECT status || '|' || rejection_reason FROM ihms_billing.t_consumed_event "
              + "WHERE event_id = ?", String.class, eventId);
        assertThat(stored).startsWith("REJECTED|BILL_REQ_INVALID: At least one charge is required");

        // relay redelivery of the REJECTED event replays the same structured failure
        mockMvc.perform(post("/api/v1/internal/events")
                        .with(SecurityMockMvcRequestPostProcessors.user("event-relay")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(body)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.errorCode").value("BILL_REQ_INVALID"));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ihms_billing.t_consumed_event WHERE event_id = ?",
                Integer.class, eventId)).isEqualTo(1);
        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void unknownEventType_400_nothingWritten_defaultDenyHolds() throws Exception {
        String eventId = UUID.randomUUID().toString();
        Map<String, Object> body = envelope(eventId);
        body.put("eventType", "LabResultFinalized");   // NOT this consumer's contract

        mockMvc.perform(post("/api/v1/internal/events")
                        .with(SecurityMockMvcRequestPostProcessors.user("event-relay")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BILL_UNKNOWN_EVENT_TYPE"));

        Integer rows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ihms_billing.t_consumed_event", Integer.class);
        assertThat(rows).as("unknown events are rejected BEFORE any ledger write").isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ihms_billing.t_bill", Integer.class)).isZero();

        // internal ingress is NOT exempt from default-deny
        mockMvc.perform(post("/api/v1/internal/events").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }
}
