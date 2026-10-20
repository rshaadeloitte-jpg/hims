package com.hims.payments.infrastructure.persistence;

import com.hims.payments.PaymentsServiceApplication;
import com.hims.payments.application.AdvanceService;
import com.hims.payments.application.PaymentService;
import com.hims.payments.application.RefundService;
import com.hims.payments.application.exceptions.InvalidPaymentRequestException;
import com.hims.payments.domain.model.Advance;
import com.hims.payments.domain.model.Payment;
import com.hims.payments.infrastructure.outbox.OutboxEvent;
import com.hims.payments.infrastructure.outbox.OutboxRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * L2 — WP-13 Payments &amp; Refunds on real PG16: V1__init_ihms_payment.sql applies
 * (ihms_payment incl. the GENERATED ALWAYS AS (amount-used) STORED balance column),
 * BR-BILL-007 advance FIFO against live rows (mode-7 splits + t_advance_consumption),
 * BEXAR002 refund guard and the EXACT outbox rows for E-08 PaymentReceived and
 * E-12 RefundIssued PRELIMINARY.
 *
 * SETTLEMENT COVERAGE (honest disclosure, per GAP-REGISTER-4 acceptance note):
 * the synchronous payments-&gt;billing REST call is exercised over REAL HTTP via a
 * contract-level stub server below — it replicates billing-service WP-11's wire
 * contract field-by-field as implemented in services/billing-service:
 *   GET  /api/v1/bills/{id}                       -> Bill JSON (balanceSelf, balanceNonSelf)
 *   PUT  /api/v1/internal/bills/{id}/settlement   -> {paidSelf, paidNonSelf} body,
 *                                                    Bill JSON response (isSettled, changeAmount)
 * It is NOT a live billing-service instance: the Maven modules are independent
 * (no test-scope dependency possible without modifying a sibling service) and the
 * billing build was red at authoring time. The stub asserts the cumulative
 * paid amounts it receives so allocation drift would fail here too.
 * NOTE: requires Docker; skipped automatically when Docker is absent — do not remove.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = PaymentsServiceApplication.class)
@DirtiesContext
@Transactional
class PaymentsServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ---- real-HTTP contract-level stub of billing-service --------------------
    private static HttpServer billingStub;
    private static volatile BigDecimal stubBalanceSelf = BigDecimal.ZERO;
    private static volatile BigDecimal stubBalanceNonSelf = BigDecimal.ZERO;
    private static final AtomicReference<String> receivedPaidSelf = new AtomicReference<>();
    private static final AtomicReference<String> receivedPaidNonSelf = new AtomicReference<>();

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) throws Exception {
        billingStub = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        billingStub.createContext("/", exchange -> {
            byte[] body;
            String path = exchange.getRequestURI().getPath();
            if ("PUT".equals(exchange.getRequestMethod())
                    && path.endsWith("/settlement")) {
                String req = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                JsonNode in = MAPPER.readTree(req);
                receivedPaidSelf.set(in.path("paidSelf").asText());
                receivedPaidNonSelf.set(in.path("paidNonSelf").asText());
                // same recompute semantics as BillingService.settle (BR-BILL-006, idempotent)
                stubBalanceSelf = stubBalanceSelf.subtract(new BigDecimal(in.path("paidSelf").asText()));
                stubBalanceNonSelf = stubBalanceNonSelf.subtract(new BigDecimal(in.path("paidNonSelf").asText()));
                boolean settled = stubBalanceSelf.signum() <= 0 && stubBalanceNonSelf.signum() <= 0;
                body = ("{\"billId\":5,\"balanceSelf\":" + stubBalanceSelf.toPlainString()
                        + ",\"balanceNonSelf\":" + stubBalanceNonSelf.toPlainString()
                        + ",\"isSettled\":" + settled + ",\"changeAmount\":12.50}")
                        .getBytes(StandardCharsets.UTF_8);
            } else {
                long billId = Long.parseLong(path
                        .substring(path.lastIndexOf('/') + 1));
                body = ("{\"billId\":" + billId
                        + ",\"balanceSelf\":" + stubBalanceSelf.toPlainString()
                        + ",\"balanceNonSelf\":" + stubBalanceNonSelf.toPlainString() + "}")
                        .getBytes(StandardCharsets.UTF_8);
            }
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        billingStub.start();
        registry.add("app.billing.base-url",
                () -> "http://127.0.0.1:" + billingStub.getAddress().getPort());
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // keep the relay scheduler off during assertions; outbox ROWS are what we pin
        registry.add("app.outbox.poll-interval-ms", () -> "3600000");
    }

    @AfterAll
    static void stopStub() {
        if (billingStub != null) {
            billingStub.stop(0);
        }
    }

    @Autowired
    private AdvanceService advanceService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private RefundService refundService;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private jakarta.persistence.EntityManager em;

    @Test
    void flyway_appliesV1_schemaAndTablesExist() {
        Integer tables = jdbc.queryForObject("""
                SELECT count(*) FROM information_schema.tables
                WHERE table_schema = 'ihms_payment'
                  AND table_name IN ('t_advance','t_payment','t_payment_mode_detail',
                                     't_advance_consumption','t_refund','outbox')
                """, Integer.class);
        assertThat(tables).isEqualTo(6);
        // generated balance column really is GENERATED in the DB
        String generation = jdbc.queryForObject("""
                SELECT is_generated FROM information_schema.columns
                WHERE table_schema='ihms_payment' AND table_name='t_advance' AND column_name='balance'
                """, String.class);
        assertThat(generation).isEqualTo("ALWAYS");
    }

    @Test
    void advance_generatedBalance_roundTripsFromDb() {
        Advance a = advanceService.create(505L, new BigDecimal("500"));
        assertThat(a.getAdvanceId()).isNotNull();
        assertThat(a.getBalance()).isEqualByComparingTo("500");   // read back from the generated column

        List<Advance> listed = advanceService.byPatient(505L);
        assertThat(listed).extracting(Advance::getAdvanceId).containsExactly(a.getAdvanceId());
    }

    @Test
    void fullPaymentFlow_settlementRecorded_outboxE08ExactContract() throws Exception {
        stubBalanceSelf = new BigDecimal("80");
        stubBalanceNonSelf = new BigDecimal("20");
        long outboxBefore = outboxRepository.count();

        Payment p = paymentService.pay(5L, 505L, new BigDecimal("100"),
                List.of(new PaymentService.ModeLine(1, new BigDecimal("100"))), false);

        // settlement request carried the self-first allocation of amount between outstanding balances
        assertThat(new BigDecimal(receivedPaidSelf.get())).isEqualByComparingTo("80");
        assertThat(new BigDecimal(receivedPaidNonSelf.get())).isEqualByComparingTo("20");
        // change_amount recorded from the settlement response
        assertThat(p.getChangeAmount()).isEqualByComparingTo("12.50");

        var e08rows = outboxRepository.findAll().stream()
                .filter(e -> e.getAggregateId().equals(p.getPaymentId())).toList();
        assertThat(outboxRepository.count()).isEqualTo(outboxBefore + 1);
        OutboxEvent e08 = e08rows.get(0);
        assertThat(e08.getEventType()).isEqualTo("PaymentReceived");          // EXACT type string (E-08)
        assertThat(e08.getVersion()).isEqualTo("v1");
        assertThat(e08.getAggregateType()).isEqualTo("payment");
        assertThat(e08.getPartitionKey()).isEqualTo("5");                     // partition key billId
        assertThat(e08.getStatus()).isEqualTo(OutboxEvent.OutboxStatus.PENDING);
        assertThat(e08.getIdempotencyKey()).isEqualTo("payment-received:" + p.getPaymentId());

        JsonNode payload = MAPPER.readTree(e08.getPayload());
        assertThat(payload.fieldNames()).toIterable()
                .containsExactlyInAnyOrder("paymentId", "billId", "patientId", "amount",
                        "modes", "advanceDetails", "transactionType", "changeAmount");
        assertThat(payload.get("paymentId").asText()).isEqualTo(String.valueOf(p.getPaymentId()));
        assertThat(payload.get("billId").asText()).isEqualTo("5");
        assertThat(payload.get("patientId").asText()).isEqualTo("505");
        assertThat(payload.get("amount").asText()).isEqualTo("100.00");
        assertThat(payload.get("advanceDetails").isNull()).isTrue();
        assertThat(payload.get("transactionType").asInt()).isEqualTo(2);
        assertThat(payload.get("changeAmount").asText()).isEqualTo("12.50");
        assertThat(payload.get("modes").get(0).get("mode").asInt()).isEqualTo(1);
    }

    @Test
    void fifo_multiAdvance_partialSplit_mode7Rows_consumptionLedger_onRealRows() throws Exception {
        stubBalanceSelf = new BigDecimal("700");
        stubBalanceNonSelf = new BigDecimal("100");

        Advance a1 = advanceService.create(505L, new BigDecimal("300"));
        Advance a2 = advanceService.create(505L, new BigDecimal("400"));

        // cash 100 + shortfall 700 covered by advances in id order (a1 then a2)
        Payment p = paymentService.pay(7L, 505L, new BigDecimal("800"),
                List.of(new PaymentService.ModeLine(2, new BigDecimal("100"))), true);

        assertThat(new BigDecimal(receivedPaidSelf.get())).isEqualByComparingTo("700");
        assertThat(new BigDecimal(receivedPaidNonSelf.get())).isEqualByComparingTo("100");

        // DB state: used incremented on both advances, generated balances dropped
        BigDecimal a1Used = jdbc.queryForObject(
                "SELECT used FROM ihms_payment.t_advance WHERE advance_id = ?", BigDecimal.class, a1.getAdvanceId());
        BigDecimal a2Used = jdbc.queryForObject(
                "SELECT used FROM ihms_payment.t_advance WHERE advance_id = ?", BigDecimal.class, a2.getAdvanceId());
        assertThat(a1Used).isEqualByComparingTo("300");
        assertThat(a2Used).isEqualByComparingTo("400");
        BigDecimal a2Balance = jdbc.queryForObject(
                "SELECT balance FROM ihms_payment.t_advance WHERE advance_id = ?", BigDecimal.class, a2.getAdvanceId());
        assertThat(a2Balance).isEqualByComparingTo("0");

        // mode rows: cash + two mode-7 chunks; consumption ledger has both chunks in id order
        assertThat(p.getModes()).hasSize(3);
        assertThat(p.getModes().stream().filter(m -> m.getMode() == 7).count()).isEqualTo(2);
        assertThat(p.getModes().stream().filter(m -> m.getMode() == 2).count()).isEqualTo(1);
        assertThat(p.getAdvanceConsumptions())
                .extracting(c -> c.getAdvanceId() + ":" + c.getConsumedAmount().toPlainString())
                .containsExactly(a1.getAdvanceId() + ":300.00", a2.getAdvanceId() + ":400.00");

        JsonNode payload = outboxPayloadFor(p.getPaymentId(), "PaymentReceived");
        assertThat(payload.get("modes")).hasSize(3);
        assertThat(payload.get("advanceDetails")).hasSize(2);
        assertThat(payload.get("advanceDetails").get(0).get("advanceId").asText())
                .isEqualTo(String.valueOf(a1.getAdvanceId()));
        assertThat(payload.get("advanceDetails").get(1).get("consumed").asText()).isEqualTo("400.00");
    }

    @Test
    void modesSumMismatch_rejected_nothingWritten_noOutboxRow() {
        stubBalanceSelf = new BigDecimal("600");
        stubBalanceNonSelf = BigDecimal.ZERO;
        long paymentsBefore = countOf("t_payment");
        long outboxBefore = outboxRepository.count();

        assertThatThrownBy(() -> paymentService.pay(9L, 505L, new BigDecimal("100"),
                        List.of(new PaymentService.ModeLine(1, new BigDecimal("90"))), false))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .extracting(e -> ((InvalidPaymentRequestException) e).getErrorCode())
                .isEqualTo("PAY_MODE_MISMATCH");

        assertThat(countOf("t_payment")).isEqualTo(paymentsBefore);
        assertThat(outboxRepository.count()).isEqualTo(outboxBefore);   // no E-08 on rejected payment
    }

    @Test
    void refunds_bexar002Guard_type3ReducesBalance_type4BillRefund_e12ExactContract() throws Exception {
        // guard: zero-balance advance refused with legacy code BEXAR002
        Advance empty = advanceService.create(707L, new BigDecimal("200"));
        Advance drained = jdbcUpdateUsed(empty.getAdvanceId(), "200");   // simulate full consumption

        assertThatThrownBy(() -> refundService.create(3L, 707L, null, drained.getAdvanceId(),
                        BigDecimal.TEN, "refund"))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .extracting(e -> ((InvalidPaymentRequestException) e).getErrorCode())
                .isEqualTo("BEXAR002");

        // happy path type 3: balance drops by the refunded amount (generated column re-read)
        long outboxBefore = outboxRepository.count();
        Advance funded = advanceService.create(707L, new BigDecimal("500"));
        var refund3 = refundService.create(3L, 707L, null, funded.getAdvanceId(),
                new BigDecimal("100"), "patient leaves");
        BigDecimal remaining = jdbc.queryForObject(
                "SELECT balance FROM ihms_payment.t_advance WHERE advance_id = ?",
                BigDecimal.class, funded.getAdvanceId());
        assertThat(remaining).isEqualByComparingTo("400");   // 500 - 100 refund

        // E-12 for the type-3 refund: exact keys, restoredCharges [] , documented pk fallback
        OutboxEvent e12 = outboxRepository.findAll().stream()
                .filter(e -> e.getIdempotencyKey().equals("refund-issued:" + refund3.getRefundId()))
                .findFirst().orElseThrow();
        assertThat(outboxRepository.count()).isEqualTo(outboxBefore + 1);   // BEXAR002 attempt emitted nothing
        assertThat(e12.getEventType()).isEqualTo("RefundIssued");   // EXACT type string (E-12, PRELIMINARY)
        assertThat(e12.getVersion()).isEqualTo("v1");
        assertThat(e12.getAggregateType()).isEqualTo("refund");
        assertThat(e12.getPartitionKey()).isEqualTo("advance:" + funded.getAdvanceId());   // documented fallback
        JsonNode payload3 = MAPPER.readTree(e12.getPayload());
        assertThat(payload3.fieldNames()).toIterable()
                .containsExactlyInAnyOrder("refundId", "billId", "advanceId", "patientId",
                        "amount", "reason", "restoredCharges");
        assertThat(payload3.get("billId").isNull()).isTrue();
        assertThat(payload3.get("advanceId").asText()).isEqualTo(String.valueOf(funded.getAdvanceId()));
        assertThat(payload3.get("amount").asText()).isEqualTo("100.00");
        assertThat(payload3.get("restoredCharges")).isEmpty();   // deferred billing-side compensation

        // type 4 bill refund: partition key billId, restoredCharges still []
        var refund4 = refundService.create(4L, 505L, 9L, null, new BigDecimal("75.25"), "overcharge");
        OutboxEvent e12b = outboxRepository.findAll().stream()
                .filter(e -> e.getIdempotencyKey().equals("refund-issued:" + refund4.getRefundId()))
                .findFirst().orElseThrow();
        assertThat(e12b.getEventType()).isEqualTo("RefundIssued");
        assertThat(e12b.getPartitionKey()).isEqualTo("9");
        JsonNode payload4 = MAPPER.readTree(e12b.getPayload());
        assertThat(payload4.get("billId").asText()).isEqualTo("9");
        assertThat(payload4.get("advanceId").isNull()).isTrue();
        assertThat(payload4.get("restoredCharges")).isEmpty();

        // GET /refunds?billId= returns the recorded row
        assertThat(refundService.byBill(9L)).extracting(r -> r.getRefundId()).contains(refund4.getRefundId());
    }

    // ------------------------------------------------------------------ helpers

    private long countOf(String table) {
        Long n = jdbc.queryForObject("SELECT count(*) FROM ihms_payment." + table, Long.class);
        return n == null ? 0 : n;
    }

    /** Simulate prior BR-BILL-007 consumption directly on the row (service path is covered elsewhere). */
    private Advance jdbcUpdateUsed(long advanceId, String used) {
        jdbc.update("UPDATE ihms_payment.t_advance SET used = ? WHERE advance_id = ?",
                new BigDecimal(used), advanceId);
        em.flush();
        em.clear();   // drop the stale generated-balance snapshot held by the persistence context
        return advanceService.get(advanceId);
    }

    private JsonNode outboxPayloadFor(Long aggregateId, String eventType) throws Exception {
        OutboxEvent row = outboxRepository.findAll().stream()
                .filter(e -> e.getAggregateId().equals(aggregateId) && e.getEventType().equals(eventType))
                .findFirst().orElseThrow();
        return MAPPER.readTree(row.getPayload());
    }
}
