package com.hims.billing.infrastructure.persistence;

import com.hims.billing.BillingServiceApplication;
import com.hims.billing.application.BillingService;
import com.hims.billing.domain.model.Bill;
import com.hims.billing.domain.model.Charge;
import com.hims.billing.infrastructure.outbox.OutboxEvent;
import com.hims.billing.infrastructure.outbox.OutboxRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * L2 — WP-11 Billing service on real PG16: Flyway V1__init_ihms_billing.sql applies
 * (ihms_billing schema: t_bill / t_charge / t_doctor_share / outbox + BR-BILL-001
 * numbering sequences), full lifecycle temp-charge -> bill (placeholder 000000000000000)
 * -> finalize (BR-BILL-005 SELF-path money, snapshots, OPDBILL-&lt;seq&gt;) with the EXACT
 * E-07 InvoiceGenerated outbox row (type 'InvoiceGenerated', version v1, partition key
 * billId, payload keys EXACTLY {billId, billNo, patientId, encounterId, admissionId,
 * header{total,tax,concession,self,nonSelf}, lines[{chargeId,serviceName,rate,qty}],
 * charges{taxTotal,concessionTotal,self,nonSelf}, doctorShare, isInterimOrFinal}),
 * and BR-BILL-006 settlement replay on the persisted row.
 * NOTE: requires Docker; skipped automatically when Docker is absent
 * (@Testcontainers(disabledWithoutDocker = true)) — do not remove.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = BillingServiceApplication.class)
@DirtiesContext
@Transactional
class BillingServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private BillingService billingService;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager em;

    @Test
    void flywayApplied_schemaAndSequencesExist() {
        Integer v1 = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ihms_billing.flyway_schema_history WHERE version = '1' AND success = TRUE",
                Integer.class);
        assertThat(v1).isEqualTo(1);   // V1__init_ihms_billing.sql applied cleanly

        for (String table : List.of("t_bill", "t_charge", "t_doctor_share", "outbox")) {
            Integer n = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables "
                  + "WHERE table_schema='ihms_billing' AND table_name='" + table + "'", Integer.class);
            assertThat(n).as("table ihms_billing.%s", table).isEqualTo(1);
        }
        for (String seq : List.of("t_bill_seq", "t_charge_seq", "t_doctor_share_seq",
                                  "t_opdbill_seq", "t_ipdbill_seq")) {
            Integer n = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.sequences "
                  + "WHERE sequence_schema='ihms_billing' AND sequence_name='" + seq + "'", Integer.class);
            assertThat(n).as("sequence ihms_billing.%s", seq).isEqualTo(1);
        }
    }

    @Test
    void fullBillingFlow_withExactE07OutboxRow_andSettlementReplay() throws Exception {
        assertThat(outboxRepository.count()).isZero();

        // ---- TEMP charge -------------------------------------------------------
        Charge charge = billingService.createCharge(new BillingService.NewCharge(
                900L, "X-Ray Chest", new BigDecimal("1000"), 1,
                null, null, null, false,
                new BigDecimal("40"),          // coPay 40 -> selfBase 400
                new BigDecimal("10"), new BigDecimal("5"),   // SELF path conPct = 15 (ADDED)
                new BigDecimal("10"), null, false));
        assertThat(charge.getBillId()).isNull();       // TEMP until attached to a bill

        // ---- bill header with placeholder billNo -------------------------------
        Bill bill = billingService.createBill(505L, 7777L, null, null, 42L, true,
                List.of(charge.getChargeId()));
        long billId = bill.getBillId();
        assertThat(bill.getBillNo()).isEqualTo("000000000000000");   // BR-BILL-001 placeholder
        assertThat(bill.getIsInterim()).isTrue();
        assertThat(bill.getIsSettled()).isFalse();
        assertThat(outboxRepository.count()).isZero();               // creation emits nothing

        // ---- finalize ----------------------------------------------------------
        Bill finalized = billingService.finalizeBill(billId);

        assertThat(finalized.getBillNo()).startsWith("OPDBILL-");    // no admissionId -> OPD variant
        assertThat(finalized.getIsInterim()).isFalse();
        // SELF path: selfRate = 400 - 15% = 340; nonSelf = 600; no taxes
        assertThat(finalized.getTotalAmount()).isEqualByComparingTo("1000.00");
        assertThat(finalized.getTaxAmount()).isEqualByComparingTo("0.00");
        assertThat(finalized.getConcessionAmount()).isEqualByComparingTo("60.00");
        assertThat(finalized.getSelfAmount()).isEqualByComparingTo("340.00");
        assertThat(finalized.getNonSelfAmount()).isEqualByComparingTo("600.00");
        assertThat(finalized.getBalanceSelf()).isEqualByComparingTo("340.00");
        assertThat(finalized.getBalanceNonSelf()).isEqualByComparingTo("600.00");

        // snapshot columns persisted (BR-BILL-008 restore contract)
        em.flush();   // push pending entity changes to JDBC so raw reads see them
        var snap = jdbcTemplate.queryForMap(
                "SELECT total_before_finalization, self_before_finalization, "
              + "nonself_before_finalization FROM ihms_billing.t_charge WHERE charge_id = ?",
                charge.getChargeId());
        assertThat(((BigDecimal) snap.get("total_before_finalization"))).isEqualByComparingTo("1000.00");
        assertThat(((BigDecimal) snap.get("self_before_finalization"))).isEqualByComparingTo("340.00");
        assertThat(((BigDecimal) snap.get("nonself_before_finalization"))).isEqualByComparingTo("600.00");

        // doctor share row (BR-BILL-002): CEILING 10% x 1000 = 100.00, is_billed=false
        Integer shares = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ihms_billing.t_doctor_share WHERE bill_id=? AND doctor_id=42 "
              + "AND share_amount=100.00 AND hospital_amount=900.00 AND is_billed=FALSE",
                Integer.class, billId);
        assertThat(shares).isEqualTo(1);

        // ---- EXACT outbox row for E-07 (exactly ONE per finalize) --------------
        var rows = outboxRepository.findAll();
        assertThat(rows).hasSize(1);

        OutboxEvent e07 = rows.get(0);
        assertThat(e07.getEventType()).isEqualTo("InvoiceGenerated");   // EXACT type string (E-07)
        assertThat(e07.getVersion()).isEqualTo("v1");                   // schema token invoice-generated.v1
        assertThat(e07.getAggregateType()).isEqualTo("bill");
        assertThat(e07.getAggregateId()).isEqualTo(billId);
        assertThat(e07.getPartitionKey()).isEqualTo(String.valueOf(billId));   // partition key billId
        assertThat(e07.getProducerService()).isEqualTo("billing-service");
        assertThat(e07.getStatus()).isEqualTo(OutboxEvent.OutboxStatus.PENDING);
        assertThat(e07.getIdempotencyKey()).isEqualTo("invoice-generated:" + billId);

        JsonNode payload = MAPPER.readTree(e07.getPayload());
        assertThat(payload.fieldNames()).toIterable().containsExactlyInAnyOrder(
                "billId", "billNo", "patientId", "encounterId", "admissionId",
                "header", "lines", "charges", "doctorShare", "isInterimOrFinal");
        assertThat(payload.get("billId").asText()).isEqualTo(String.valueOf(billId));
        assertThat(payload.get("billNo").asText()).isEqualTo(finalized.getBillNo());
        assertThat(payload.get("patientId").asText()).isEqualTo("505");
        assertThat(payload.get("encounterId").asText()).isEqualTo("7777");
        assertThat(payload.get("admissionId").isNull()).isTrue();

        assertThat(payload.get("header").fieldNames()).toIterable()
                .containsExactlyInAnyOrder("total", "tax", "concession", "self", "nonSelf");
        assertThat(payload.get("header").get("self").doubleValue()).isEqualTo(340.00);
        assertThat(payload.get("header").get("nonSelf").doubleValue()).isEqualTo(600.00);

        assertThat(payload.get("lines")).hasSize(1);
        assertThat(payload.get("lines").get(0).fieldNames()).toIterable()
                .containsExactlyInAnyOrder("chargeId", "serviceName", "rate", "qty");
        assertThat(payload.get("lines").get(0).get("chargeId").asText())
                .isEqualTo(String.valueOf(charge.getChargeId()));
        assertThat(payload.get("lines").get(0).get("serviceName").asText()).isEqualTo("X-Ray Chest");

        assertThat(payload.get("charges").fieldNames()).toIterable()
                .containsExactlyInAnyOrder("taxTotal", "concessionTotal", "self", "nonSelf");
        assertThat(payload.get("charges").get("taxTotal").doubleValue()).isEqualTo(0.0);
        assertThat(payload.get("charges").get("concessionTotal").doubleValue()).isEqualTo(60.0);

        assertThat(payload.get("doctorShare").get("doctorId").asText()).isEqualTo("42");
        assertThat(payload.get("doctorShare").get("shareAmount").doubleValue()).isEqualTo(100.0);
        assertThat(payload.get("doctorShare").get("hospitalAmount").doubleValue()).isEqualTo(900.0);

        assertThat(payload.get("isInterimOrFinal").asText()).isEqualTo("FINAL");

        // ---- BR-BILL-006 settlement + idempotent replay on the PERSISTED row ---
        Bill partial = billingService.settle(billId, new BigDecimal("340"), new BigDecimal("200"));
        assertThat(partial.getBalanceSelf()).isEqualByComparingTo("0.00");
        assertThat(partial.getBalanceNonSelf()).isEqualByComparingTo("400.00");
        assertThat(partial.getIsSettled()).isFalse();      // one balance > 0 => not settled

        Bill settled = billingService.settle(billId, new BigDecimal("340"), new BigDecimal("600"));
        assertThat(settled.getIsSettled()).isTrue();       // BOTH <= 0 => settled

        em.flush();
        Boolean dbSettled = jdbcTemplate.queryForObject(
                "SELECT is_settled FROM ihms_billing.t_bill WHERE bill_id=?", Boolean.class, billId);
        assertThat(dbSettled).isTrue();

        // payments-service retry replays the same cumulative request: state unchanged
        Bill replay = billingService.settle(billId, new BigDecimal("340"), new BigDecimal("600"));
        assertThat(replay.getIsSettled()).isTrue();
        assertThat(replay.getBalanceSelf()).isEqualByComparingTo("0.00");
        assertThat(replay.getBalanceNonSelf()).isEqualByComparingTo("0.00");

        // settlement does not re-emit InvoiceGenerated; the new BillSettled event
        // is fired exactly once on the first transition to settled (BR-BILL-006
        // replay-safe by recomputation; idempotent emission pinned by the
        // BillingEventsIntegrationTest). So we expect InvoiceGenerated + BillSettled.
        assertThat(outboxRepository.count()).isEqualTo(2);
        assertThat(outboxRepository.findAll().stream()
                .anyMatch(e -> "BillSettled".equals(e.getEventType()))).isTrue();
        assertThat(outboxRepository.findAll().stream()
                .anyMatch(e -> "InvoiceGenerated".equals(e.getEventType()))).isTrue();
    }
}
