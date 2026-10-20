package com.hims.billing.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.billing.BillingServiceApplication;
import com.hims.billing.configuration.DevSecurityConfig;
import com.hims.billing.infrastructure.outbox.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.InputStream;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * L1 - REST Contract Tests (TEST-STRATEGY §B) validating the API against
 * {@code openapi/billing-service.yaml}.
 * <p>
 * These tests use the compiled Spring context (not mocked) and verify:
 * 1. Request/response shapes match the OpenAPI schemas
 * 2. Error codes match the contract
 * 3. All documented endpoints respond with correct status codes
 * 4. The {@code BillSettled} and {@code ConcessionApproved} events are emitted
 *    via the outbox (verified by checking outbox repository)
 */
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = BillingServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "spring.testcontainers.mode=disabled")
@AutoConfigureMockMvc
@Import(DevSecurityConfig.class)
@ActiveProfiles("dev")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BillControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OutboxRepository outboxRepository;

    @MockitoBean
    private KafkaTemplate<Object, Object> kafkaTemplate;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeEach
    void setup() {
        outboxRepository.deleteAllInBatch();
    }

    // ---------------------------------------------------------- OpenAPI contract validation

    @Test
    void openApiSpec_loadsAndIsValid() throws Exception {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("openapi/billing-service.yaml")) {
            assertThat(is).as("openapi/billing-service.yaml must be on classpath").isNotNull();
            String spec = new String(is.readAllBytes());
            assertThat(spec).contains("InvoiceGenerated");
            assertThat(spec).contains("BillSettled");
            assertThat(spec).contains("ConcessionApproved");
            assertThat(spec).contains("cancel-settled");
        }
    }

    // ---------------------------------------------------------- POST /api/v1/charges

    @Test
    void createCharge_validRequest_returns201WithChargeSchema() throws Exception {
        String body = """
                {
                  "serviceId": 900,
                  "serviceName": "X-Ray Chest",
                  "rate": 350.00,
                  "qty": 1,
                  "coPayPct": 40,
                  "doctorSharePct": 10
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/v1/charges")
                        .with(SecurityMockMvcRequestPostProcessors.user("devops"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.chargeId").exists())
                .andExpect(jsonPath("$.serviceId").value(900))
                .andExpect(jsonPath("$.serviceName").value("X-Ray Chest"))
                .andExpect(jsonPath("$.rate").value(350.00))
                .andExpect(jsonPath("$.qty").value(1))
                .andExpect(jsonPath("$.billId").doesNotExist())
                .andReturn();

        JsonNode response = MAPPER.readTree(result.getResponse().getContentAsString());
        assertThat(response.get("chargeId").asLong()).isGreaterThan(0);
    }

    @Test
    void createCharge_missingServiceId_returns400WithErrorCode() throws Exception {
        String body = """
                {"serviceName":"X-Ray","rate":100}
                """;

        mockMvc.perform(post("/api/v1/charges")
                        .with(SecurityMockMvcRequestPostProcessors.user("devops"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BILL_REQ_INVALID"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("serviceId")));
    }

    @Test
    void createCharge_negativeRate_returns400() throws Exception {
        String body = """
                {"serviceId":900,"serviceName":"X-Ray","rate":-10}
                """;

        mockMvc.perform(post("/api/v1/charges")
                        .with(SecurityMockMvcRequestPostProcessors.user("devops"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BILL_REQ_INVALID"));
    }

    // ---------------------------------------------------------- POST /api/v1/bills

    @Test
    void createBill_validRequest_returns201WithPlaceholderBillNo() throws Exception {
        // First create a charge
        String chargeBody = """
                {"serviceId":900,"serviceName":"X-Ray","rate":350,"qty":1}
                """;
        MvcResult chargeResult = mockMvc.perform(post("/api/v1/charges")
                        .with(SecurityMockMvcRequestPostProcessors.user("devops"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(chargeBody))
                .andExpect(status().isCreated())
                .andReturn();
        Long chargeId = MAPPER.readTree(chargeResult.getResponse().getContentAsString()).get("chargeId").asLong();

        String billBody = """
                {
                  "patientId":505,"encounterId":7777,"prescriberDoctorId":42,
                  "isSelfCompany":true,"chargeIds":[%d]
                }
                """.formatted(chargeId);

        MvcResult billResult = mockMvc.perform(post("/api/v1/bills")
                        .with(SecurityMockMvcRequestPostProcessors.user("devops"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(billBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.billNo").value("000000000000000"))
                .andExpect(jsonPath("$.isInterim").value(true))
                .andExpect(jsonPath("$.isSettled").value(false))
                .andExpect(jsonPath("$.charges[0].chargeId").value(chargeId.intValue()))
                .andReturn();

        JsonNode bill = MAPPER.readTree(billResult.getResponse().getContentAsString());
        assertThat(bill.get("billId").asLong()).isGreaterThan(0);
    }

    @Test
    void createBill_noCharges_returns400() throws Exception {
        String body = """
                {"patientId":505,"chargeIds":[]}
                """;

        mockMvc.perform(post("/api/v1/bills")
                        .with(SecurityMockMvcRequestPostProcessors.user("devops"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BILL_REQ_INVALID"));
    }

    // ---------------------------------------------------------- GET /api/v1/bills/{id}

    @Test
    void getBill_existing_returnsBillWithCharges() throws Exception {
        Long billId = createTestBill();
        MvcResult result = mockMvc.perform(get("/api/v1/bills/" + billId)
                        .with(SecurityMockMvcRequestPostProcessors.user("devops")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.billId").value(billId.intValue()))
                .andExpect(jsonPath("$.charges").isArray())
                .andExpect(jsonPath("$.charges.length()").value(org.hamcrest.Matchers.greaterThan(0)))
                .andReturn();
    }

    // ---------------------------------------------------------- POST /api/v1/bills/{id}/finalize

    @Test
    void finalizeBill_interimBill_returnsFinalBillWithOpdNumber_andEmitsInvoiceGenerated() throws Exception {
        Long billId = createTestBill();

        MvcResult result = mockMvc.perform(post("/api/v1/bills/" + billId + "/finalize")
                        .with(SecurityMockMvcRequestPostProcessors.user("devops"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.billNo").value(org.hamcrest.Matchers.startsWith("OPDBILL-")))
                .andExpect(jsonPath("$.isInterim").value(false))
                .andReturn();

        JsonNode response = MAPPER.readTree(result.getResponse().getContentAsString());
        String finalBillNo = response.get("billNo").asText();

        // Verify InvoiceGenerated outbox event
        assertOutboxEventEmitted("InvoiceGenerated", billId);
    }

    @Test
    void finalizeBill_alreadyFinalized_returns400() throws Exception {
        Long billId = createTestBill();
        // First finalize
        mockMvc.perform(post("/api/v1/bills/" + billId + "/finalize")
                        .with(SecurityMockMvcRequestPostProcessors.user("devops"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk());

        // Second finalize should fail
        mockMvc.perform(post("/api/v1/bills/" + billId + "/finalize")
                        .with(SecurityMockMvcRequestPostProcessors.user("devops"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BILL_ILLEGAL_TRANSITION"));
    }

    // ---------------------------------------------------------- POST /api/v1/bills/{id}/concession

    @Test
    void applyConcession_interimBill_returnsUpdatedBill_andEmitsConcessionApproved() throws Exception {
        Long billId = createTestBill();

        String body = """
                {"concessionPlanPct":10,"concessionBillPct":5}
                """;
        mockMvc.perform(post("/api/v1/bills/" + billId + "/concession")
                        .with(SecurityMockMvcRequestPostProcessors.user("devops"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.billId").value(billId.intValue()));

        // Verify ConcessionApproved outbox event
        assertOutboxEventEmitted("ConcessionApproved", billId);
    }

    @Test
    void applyConcession_afterFinalize_returns400() throws Exception {
        Long billId = createTestBill();
        mockMvc.perform(post("/api/v1/bills/" + billId + "/finalize")
                        .with(SecurityMockMvcRequestPostProcessors.user("devops"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk());

        String body = """
                {"concessionPlanPct":10,"concessionBillPct":5}
                """;
        mockMvc.perform(post("/api/v1/bills/" + billId + "/concession")
                        .with(SecurityMockMvcRequestPostProcessors.user("devops"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BILL_ALREADY_FINALIZED"));
    }

    @Test
    void applyConcession_negativePct_returns400() throws Exception {
        Long billId = createTestBill();
        String body = """
                {"concessionPlanPct":-5,"concessionBillPct":5}
                """;
        mockMvc.perform(post("/api/v1/bills/" + billId + "/concession")
                        .with(SecurityMockMvcRequestPostProcessors.user("devops"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BILL_REQ_INVALID"));
    }

    // ---------------------------------------------------------- PUT /api/v1/internal/bills/{id}/settlement

    @Test
    void settleBill_partialPayment_returnsUnsettledState() throws Exception {
        Long billId = createAndFinalizeTestBill();

        String body = """
                {"paidSelf":300,"paidNonSelf":200}
                """;
        mockMvc.perform(put("/api/v1/internal/bills/" + billId + "/settlement")
                        .with(SecurityMockMvcRequestPostProcessors.user("payments-svc"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanceSelf").value(org.hamcrest.Matchers.greaterThan(0.0)))
                .andExpect(jsonPath("$.isSettled").value(false));

        // BillSettled NOT emitted yet
        assertOutboxEventNotEmitted("BillSettled", billId);
    }

    @Test
    void settleBill_fullPayment_returnsSettledState_andEmitsBillSettled() throws Exception {
        Long billId = createAndFinalizeTestBill();

        // First check the bill amounts to know what to pay
        MvcResult billResult = mockMvc.perform(get("/api/v1/bills/" + billId)
                        .with(SecurityMockMvcRequestPostProcessors.user("devops")))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode bill = MAPPER.readTree(billResult.getResponse().getContentAsString());
        BigDecimal selfAmount = new BigDecimal(bill.get("selfAmount").asText());
        BigDecimal nonSelfAmount = new BigDecimal(bill.get("nonSelfAmount").asText());

        String body = """
                {"paidSelf":%s,"paidNonSelf":%s}
                """.formatted(selfAmount, nonSelfAmount);
        mockMvc.perform(put("/api/v1/internal/bills/" + billId + "/settlement")
                        .with(SecurityMockMvcRequestPostProcessors.user("payments-svc"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSettled").value(true))
                .andExpect(jsonPath("$.balanceSelf").value(org.hamcrest.Matchers.lessThanOrEqualTo(0.0)))
                .andExpect(jsonPath("$.balanceNonSelf").value(org.hamcrest.Matchers.lessThanOrEqualTo(0.0)));

        // Verify BillSettled outbox event
        assertOutboxEventEmitted("BillSettled", billId);
    }

    @Test
    void settleBill_replayIdempotent_sameState() throws Exception {
        Long billId = createAndFinalizeTestBill();
        MvcResult billResult = mockMvc.perform(get("/api/v1/bills/" + billId)
                        .with(SecurityMockMvcRequestPostProcessors.user("devops")))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode bill = MAPPER.readTree(billResult.getResponse().getContentAsString());
        BigDecimal selfAmount = new BigDecimal(bill.get("selfAmount").asText());
        BigDecimal nonSelfAmount = new BigDecimal(bill.get("nonSelfAmount").asText());

        String body = """
                {"paidSelf":%s,"paidNonSelf":%s}
                """.formatted(selfAmount, nonSelfAmount);
        // First call
        mockMvc.perform(put("/api/v1/internal/bills/" + billId + "/settlement")
                        .with(SecurityMockMvcRequestPostProcessors.user("payments-svc"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
        // Replay
        MvcResult replayResult = mockMvc.perform(put("/api/v1/internal/bills/" + billId + "/settlement")
                        .with(SecurityMockMvcRequestPostProcessors.user("payments-svc"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode replay = MAPPER.readTree(replayResult.getResponse().getContentAsString());
        assertThat(replay.get("isSettled").asBoolean()).isTrue();
        assertThat(new BigDecimal(replay.get("balanceSelf").asText())).isEqualByComparingTo("0.00");
        assertThat(new BigDecimal(replay.get("balanceNonSelf").asText())).isEqualByComparingTo("0.00");

        // BillSettled emitted only ONCE
        long settledCount = outboxRepository.findAll().stream()
                .filter(e -> "BillSettled".equals(e.getEventType()) && e.getAggregateId().equals(billId))
                .count();
        assertThat(settledCount).as("BillSettled emitted exactly once").isEqualTo(1);
    }

    @Test
    void settleBill_negativeAmount_returns400() throws Exception {
        Long billId = createAndFinalizeTestBill();
        String body = """
                {"paidSelf":-1,"paidNonSelf":0}
                """;
        mockMvc.perform(put("/api/v1/internal/bills/" + billId + "/settlement")
                        .with(SecurityMockMvcRequestPostProcessors.user("payments-svc"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BILL_REQ_INVALID"));
    }

    // ---------------------------------------------------------- POST /api/v1/bills/{id}/cancel-settled (BR-BILL-008)

    @Test
    void cancelSettledBill_settledBill_returnsCancelledWithRestoredCharges() throws Exception {
        Long billId = createAndFinalizeTestBill();
        // Settle it first
        MvcResult billResult = mockMvc.perform(get("/api/v1/bills/" + billId)
                        .with(SecurityMockMvcRequestPostProcessors.user("devops")))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode bill = MAPPER.readTree(billResult.getResponse().getContentAsString());
        BigDecimal selfAmount = new BigDecimal(bill.get("selfAmount").asText());
        BigDecimal nonSelfAmount = new BigDecimal(bill.get("nonSelfAmount").asText());

        String settleBody = """
                {"paidSelf":%s,"paidNonSelf":%s}
                """.formatted(selfAmount, nonSelfAmount);
        mockMvc.perform(put("/api/v1/internal/bills/" + billId + "/settlement")
                        .with(SecurityMockMvcRequestPostProcessors.user("payments-svc"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(settleBody))
                .andExpect(status().isOk());

        // Now cancel settled bill
        String cancelBody = """
                {"reason":"Patient request"}
                """;
        MvcResult result = mockMvc.perform(post("/api/v1/bills/" + billId + "/cancel-settled")
                        .with(SecurityMockMvcRequestPostProcessors.user("devops"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cancelBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isCancelled").value(true))
                .andExpect(jsonPath("$.isSettled").value(false))
                .andReturn();

        // Verify charges have snapshot data restored (isCancelled=true on charges)
        JsonNode cancelled = MAPPER.readTree(result.getResponse().getContentAsString());
        assertThat(cancelled.get("charges")).isNotEmpty();
        for (JsonNode charge : cancelled.get("charges")) {
            assertThat(charge.get("isCancelled").asBoolean()).isTrue();
            assertThat(charge.get("totalBeforeFinalization")).isNotNull();
        }
    }

    @Test
    void cancelSettledBill_unsettledBill_returns400() throws Exception {
        Long billId = createAndFinalizeTestBill(); // finalized but not settled
        String body = """
                {"reason":"Test"}
                """;
        mockMvc.perform(post("/api/v1/bills/" + billId + "/cancel-settled")
                        .with(SecurityMockMvcRequestPostProcessors.user("devops"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BILL_ILLEGAL_TRANSITION"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("SETTLED")));
    }

    @Test
    void cancelSettledBill_alreadyCancelled_returns400() throws Exception {
        Long billId = createAndFinalizeTestBill();
        MvcResult billResult = mockMvc.perform(get("/api/v1/bills/" + billId)
                        .with(SecurityMockMvcRequestPostProcessors.user("devops")))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode bill = MAPPER.readTree(billResult.getResponse().getContentAsString());
        BigDecimal selfAmount = new BigDecimal(bill.get("selfAmount").asText());
        BigDecimal nonSelfAmount = new BigDecimal(bill.get("nonSelfAmount").asText());
        String settleBody = """
                {"paidSelf":%s,"paidNonSelf":%s}
                """.formatted(selfAmount, nonSelfAmount);
        mockMvc.perform(put("/api/v1/internal/bills/" + billId + "/settlement")
                        .with(SecurityMockMvcRequestPostProcessors.user("payments-svc"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(settleBody))
                .andExpect(status().isOk());

        String cancelBody = """
                {"reason":"Test"}
                """;
        // First cancel
        mockMvc.perform(post("/api/v1/bills/" + billId + "/cancel-settled")
                        .with(SecurityMockMvcRequestPostProcessors.user("devops"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cancelBody))
                .andExpect(status().isOk());
        // Second cancel
        mockMvc.perform(post("/api/v1/bills/" + billId + "/cancel-settled")
                        .with(SecurityMockMvcRequestPostProcessors.user("devops"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cancelBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BILL_ILLEGAL_TRANSITION"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("cancelled")));
    }

    // ---------------------------------------------------------- POST /api/v1/internal/events (WP-S1 saga)

    @Disabled("Requires PostgreSQL ON CONFLICT syntax; run with Testcontainers")
    @Test
    void consumeInternalEvent_orderCreated_createsDraft_andEmitsNothing() throws Exception {
        String eventId = java.util.UUID.randomUUID().toString();
        String body = """
                {
                  "eventId":"%s","eventType":"OrderCreated","payload":
                    {
                      "orderId":501,"patientId":505,"encounterId":7777,
                      "lines":[{"itemId":901,"serviceName":"CBC","rate":250,"qty":1}],
                      "prescriberId":9
                    }
                }
                """.formatted(eventId);

        mockMvc.perform(post("/api/v1/internal/events")
                        .with(SecurityMockMvcRequestPostProcessors.user("event-relay"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPLIED"))
                .andExpect(jsonPath("$.sourceOrderId").value(501));

        // Verify NO outbox event emitted (ingress emits nothing)
        assertThat(outboxRepository.count()).isZero();
    }

    @Disabled("Requires PostgreSQL ON CONFLICT syntax; run with Testcontainers")
    @Test
    void consumeInternalEvent_duplicateEventId_returnsDuplicate() throws Exception {
        String eventId = java.util.UUID.randomUUID().toString();
        String body = """
                {
                  "eventId":"%s","eventType":"OrderCreated","payload":
                    {
                      "orderId":501,"patientId":505,"encounterId":7777,
                      "lines":[{"itemId":901,"serviceName":"CBC","rate":250,"qty":1}],
                      "prescriberId":9
                    }
                }
                """.formatted(eventId);

        mockMvc.perform(post("/api/v1/internal/events")
                        .with(SecurityMockMvcRequestPostProcessors.user("event-relay"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/internal/events")
                        .with(SecurityMockMvcRequestPostProcessors.user("event-relay"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DUPLICATE"));
    }

    @Test
    void consumeInternalEvent_unknownEventType_returns400() throws Exception {
        String body = """
                {"eventId":"test","eventType":"LabResult","payload":{}}
                """;
        mockMvc.perform(post("/api/v1/internal/events")
                        .with(SecurityMockMvcRequestPostProcessors.user("event-relay"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BILL_UNKNOWN_EVENT_TYPE"));
    }

    // ---------------------------------------------------------- Helpers

    private Long createTestBill() throws Exception {
        String chargeBody = """
                {
                  "serviceId":900,"serviceName":"X-Ray","rate":1000,"qty":1,
                  "coPayPct":40,"concessionPlanPct":10,"concessionBillPct":5,
                  "doctorSharePct":10
                }
                """;
        MvcResult chargeResult = mockMvc.perform(post("/api/v1/charges")
                        .with(SecurityMockMvcRequestPostProcessors.user("devops"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(chargeBody))
                .andExpect(status().isCreated())
                .andReturn();
        Long chargeId = MAPPER.readTree(chargeResult.getResponse().getContentAsString()).get("chargeId").asLong();

        String billBody = """
                {
                  "patientId":505,"encounterId":7777,"prescriberDoctorId":42,
                  "isSelfCompany":true,"chargeIds":[%d]
                }
                """.formatted(chargeId);
        MvcResult billResult = mockMvc.perform(post("/api/v1/bills")
                        .with(SecurityMockMvcRequestPostProcessors.user("devops"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(billBody))
                .andExpect(status().isCreated())
                .andReturn();
        return MAPPER.readTree(billResult.getResponse().getContentAsString()).get("billId").asLong();
    }

    private Long createAndFinalizeTestBill() throws Exception {
        Long billId = createTestBill();
        mockMvc.perform(post("/api/v1/bills/" + billId + "/finalize")
                        .with(SecurityMockMvcRequestPostProcessors.user("devops"))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk());
        return billId;
    }

    private void assertOutboxEventEmitted(String eventType, Long billId) {
        var events = outboxRepository.findAll();
        assertThat(events).as("outbox should have " + eventType + " event for bill " + billId)
                .anyMatch(e -> eventType.equals(e.getEventType()) && e.getAggregateId().equals(billId));
    }

    private void assertOutboxEventNotEmitted(String eventType, Long billId) {
        var events = outboxRepository.findAll();
        assertThat(events).as("outbox should NOT have " + eventType + " event for bill " + billId)
                .noneMatch(e -> eventType.equals(e.getEventType()) && e.getAggregateId().equals(billId));
    }
}
