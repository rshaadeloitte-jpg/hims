package com.hims.pharmacy.infrastructure.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.pharmacy.PharmacyServiceApplication;
import com.hims.pharmacy.application.ItemSaleService;
import com.hims.pharmacy.application.PrescriptionDispenseService;
import com.hims.pharmacy.application.DrugInterventionService;
import com.hims.pharmacy.application.WardPicklistService;
import com.hims.pharmacy.application.exceptions.IllegalSaleStateException;
import com.hims.pharmacy.application.exceptions.InvalidSaleRequestException;
import com.hims.pharmacy.application.exceptions.SaleNotFoundException;
import com.hims.pharmacy.application.exceptions.StockIssueException;
import com.hims.pharmacy.domain.model.DrugIntervention;
import com.hims.pharmacy.domain.model.ItemSale;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
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

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * L2 — WP-R2 Pharmacy &amp; Dispensing service on real PG16 (ihms_pharmacy).
 *
 * Pins:
 * - Flyway V1 applies cleanly (all WP-R2 tables + sequences + outbox exist);
 * - BR-PHAR-001 full/partial/boundary dispense flags on real rows;
 * - BR-PHAR-002 close/pending-decrement/master-close on the LOCAL projection
 *   (ordering-service remains owner of truth — write-back deferred post-saga);
 * - the item-service stock call over REAL HTTP against a wire-shape stub
 *   replicating the single-writer movement API field-by-field (DISCLOSED: this is
 *   NOT a live item-service — see outputs/implementation/WP-R2-pharmacy.md);
 * - stock-call FAILURE rolls everything back: sale stays PENDING, nothing marked
 *   dispensed, projection untouched;
 * - NEGATIVE OUTBOX assertion: the outbox table stays EMPTY row-wise after every
 *   flow — DispensingCompleted is PROPOSED and must NOT be emitted.
 *
 * NOTE: requires Docker; skipped automatically when Docker is absent
 * (@Testcontainers(disabledWithoutDocker = true)) — do not remove.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = PharmacyServiceApplication.class)
@DirtiesContext
@Transactional
class PharmacyServiceIntegrationTest {

    /** itemId that makes the stub answer like item-service rejecting the issue. */
    private static final long OUT_OF_STOCK_ITEM = 902L;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Real-HTTP stub of the item-service single-writer stock movement API. */
    private static final HttpServer ITEM_STUB = startItemStub();
    /** Request bodies observed by the stub (wire-shape assertions). */
    private static final List<String> STUB_REQUESTS = new CopyOnWriteArrayList<>();

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @org.springframework.test.context.DynamicPropertySource
    static void pharmacyProps(org.springframework.test.context.DynamicPropertyRegistry registry) {
        registry.add("app.item-service.base-url",
                () -> "http://127.0.0.1:" + ITEM_STUB.getAddress().getPort());
        registry.add("app.pharmacy.store-id", () -> 7L);
    }

    @Autowired
    private ItemSaleService saleService;
    @Autowired
    private PrescriptionDispenseService projectionService;
    @Autowired
    private DrugInterventionService interventionService;
    @Autowired
    private WardPicklistService picklistService;
    @Autowired
    private com.hims.pharmacy.infrastructure.outbox.OutboxRepository outboxRepository;
    @Autowired
    private com.hims.pharmacy.domain.repository.PrescriptionDispenseRepository dispenseMasterRepository;
    @Autowired
    private com.hims.pharmacy.domain.repository.PrescriptionDispenseDetailRepository dispenseDetailRepository;
    @Autowired
    private JdbcTemplate jdbc;

    private static HttpServer startItemStub() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/api/v1/stock/movements", exchange -> {
                try {
                    String body = readBody(exchange);
                    STUB_REQUESTS.add(body);
                    JsonNode node = MAPPER.readTree(body);
                    long itemId = node.path("itemId").asLong();
                    if (itemId == OUT_OF_STOCK_ITEM) {
                        // replicate item-service rejection envelope (HTTP 200 + status ERROR,
                        // exactly what ItemStockClient maps to StockIssueException)
                        respond(exchange, 200, "{\"status\":\"ERROR\",\"errorCode\":\"BEXA003\","
                                + "\"message\":\"Insufficient stock for item " + itemId + "\"}");
                        return;
                    }
                    int qty = node.path("quantity").asInt();
                    respond(exchange, 200, "{\"id\":51,\"storeId\":" + node.path("storeId").asLong()
                            + ",\"itemId\":" + itemId + ",\"batchId\":null,"
                            + "\"qtyInHand\":" + (1000 - qty) + ",\"availableQty\":" + (1000 - qty) + "}");
                } finally {
                    exchange.close();
                }
            });
            server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(4));
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot start item-service stub", e);
        }
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream in = exchange.getRequestBody()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void respond(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    @org.junit.jupiter.api.Test
    void flywayApplied_allWpR2TablesAndOutboxExist() {
        List<String> tables = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables "
                        + "WHERE table_schema = 'ihms_pharmacy' ORDER BY table_name", String.class);
        assertThat(tables).contains(
                "t_itemsale", "t_item_sale_detail",
                "t_prescription_dispense", "t_prescription_dispense_detail",
                "t_drug_intervention", "t_ward_picklist", "outbox");
        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void brPhar001_and_brPhar002_fullFlow_onRealPg_outboxStaysEmpty() throws Exception {
        long outboxBefore = outboxRepository.count();
        assertThat(outboxBefore).isZero();
        int requestsBefore = STUB_REQUESTS.size();

        // ---- create with prescription link: seeds BR-PHAR-002 local projection
        ItemSale sale = saleService.create(505L, 555L, List.of(
                new ItemSaleService.LineInput(501L, null, 5),
                new ItemSaleService.LineInput(502L, null, 2)));
        long saleId = sale.getSaleId();

        assertThat(sale.getStatusCode()).isEqualTo(ItemSale.STATUS_PENDING);
        assertThat(sale.getDetails())
                .extracting(com.hims.pharmacy.domain.model.ItemSaleDetail::getPendingQty)
                .containsExactly(5, 2);

        var master = projectionMasterOf(555L);
        assertThat(master).isPresent();
        assertThat(master.get().getClosed()).isFalse();          // BR-PHAR-002 starts open

        // ---- partial dispense line 1: PARTIALLY_DISPENSED, pending decremented
        ItemSale partial = saleService.dispense(saleId,
                List.of(new ItemSaleService.DispenseLine(1L, 2)));

        assertThat(partial.getStatusCode()).isEqualTo(ItemSale.STATUS_PARTIALLY_DISPENSED);
        assertThat(partial.getDispensed()).isFalse();
        assertThat(detail(saleId, 1L).getPendingQty()).isEqualTo(3);
        assertThat(detail(saleId, 1L).getDispensed()).isFalse();
        assertThat(detail(saleId, 1L).getPartialDispense()).isTrue();

        var projLine1 = projectionDetail(555L, 1L);
        assertThat(projLine1.getPendingQty()).isEqualTo(3);       // PendingQty -= qty (BR-PHAR-002)
        assertThat(projLine1.getClosed()).isFalse();

        // ---- close line 1 exactly (boundary): line flag flips, sale still partial
        ItemSale line1Closed = saleService.dispense(saleId,
                List.of(new ItemSaleService.DispenseLine(1L, 3)));
        assertThat(line1Closed.getStatusCode()).isEqualTo(ItemSale.STATUS_PARTIALLY_DISPENSED);
        assertThat(detail(saleId, 1L).getDispensed()).isTrue();
        assertThat(detail(saleId, 1L).getPendingQty()).isZero();
        assertThat(projectionDetail(555L, 1L).getClosed()).isTrue();
        assertThat(projectionDetail(555L, 1L).getPendingQty()).isZero();
        assertThat(projectionMasterOf(555L).get().getClosed()).isFalse();  // line 2 open

        // ---- last line closes the sale: header flips EXACTLY at this point
        ItemSale full = saleService.dispense(saleId,
                List.of(new ItemSaleService.DispenseLine(2L, 2)));

        assertThat(full.getStatusCode()).isEqualTo(ItemSale.STATUS_FULLY_DISPENSED);
        assertThat(full.getDispensed()).isTrue();                 // BR-PHAR-001 header flag
        assertThat(detail(saleId, 2L).getDispensed()).isTrue();
        assertThat(projectionDetail(555L, 2L).getClosed()).isTrue();
        assertThat(projectionMasterOf(555L).get().getClosed()).isTrue();   // ALL closed -> master '1'

        // ---- terminal guard
        assertThatThrownBy(() -> saleService.dispense(saleId,
                        List.of(new ItemSaleService.DispenseLine(1L, 1))))
                .isInstanceOf(IllegalSaleStateException.class)
                .extracting(e -> ((IllegalSaleStateException) e).getErrorCode())
                .isEqualTo("PH_ILLEGAL_TRANSITION");

        // ---- wire shape of the three real-HTTP stock calls (single-writer D-1)
        List<String> calls = new ArrayList<>(STUB_REQUESTS.subList(requestsBefore, STUB_REQUESTS.size()));
        assertThat(calls).hasSize(3);
        JsonNode first = MAPPER.readTree(calls.get(0));
        assertThat(first.fieldNames()).toIterable().containsExactlyInAnyOrder(
                "storeId", "itemId", "batchId", "movementType", "operation",
                "quantity", "refDocType", "refDocId", "remarks");
        assertThat(first.get("storeId").asInt()).isEqualTo(7);
        assertThat(first.get("movementType").asInt()).isEqualTo(4);      // SALE
        assertThat(first.get("operation").asInt()).isEqualTo(2);         // SUBTRACT
        assertThat(first.get("refDocType").asText()).isEqualTo("PHARMACY_SALE");
        assertThat(first.get("refDocId").asText()).isEqualTo(String.valueOf(saleId));
        assertThat(MAPPER.readTree(calls.get(0)).get("quantity").asInt()).isEqualTo(2);
        assertThat(MAPPER.readTree(calls.get(1)).get("quantity").asInt()).isEqualTo(3);
        assertThat(MAPPER.readTree(calls.get(2)).get("quantity").asInt()).isEqualTo(2);

        // ---- NEGATIVE OUTBOX assertion: nothing emitted, ever
        assertThat(outboxRepository.count()).isEqualTo(outboxBefore);
        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void stock_failure_rollsBack_saleStaysPending_nothingDispensed() {
        ItemSale sale = saleService.create(506L, null,
                List.of(new ItemSaleService.LineInput(OUT_OF_STOCK_ITEM, null, 2)));
        long saleId = sale.getSaleId();

        assertThatThrownBy(() -> saleService.dispense(saleId,
                        List.of(new ItemSaleService.DispenseLine(1L, 2))))
                .isInstanceOf(StockIssueException.class)
                .hasMessageContaining("Insufficient stock");

        // tx rolled back in production semantics: NOTHING was persisted beyond creation
        ItemSale untouched = saleService.get(saleId);
        assertThat(untouched.getStatusCode()).isEqualTo(ItemSale.STATUS_PENDING);
        assertThat(untouched.getDispensed()).isFalse();
        assertThat(detail(saleId, 1L).getPendingQty()).isEqualTo(2);
        assertThat(detail(saleId, 1L).getDispensed()).isFalse();

        assertThat(outboxRepository.count()).isZero();     // failure emitted nothing either
    }

    @Test
    void supportingFlows_interventions_picklists_emrNo_guards_and_outboxEmpty() {
        // ---- guards first (nothing written)
        assertThatThrownBy(() -> interventionService.create(null, null, "X", null, 9L))
                .isInstanceOf(InvalidSaleRequestException.class)
                .extracting(e -> ((InvalidSaleRequestException) e).getErrorCode())
                .isEqualTo("BEXA003");
        assertThatThrownBy(() -> picklistService.create(12L, null))
                .isInstanceOf(InvalidSaleRequestException.class)
                .hasMessageContaining("saleId is required");

        // ---- sale to anchor picklist + EMR stamping (prescription-linked)
        ItemSale sale = saleService.create(507L, 777L,
                List.of(new ItemSaleService.LineInput(503L, null, 1)));
        saleService.dispense(sale.getSaleId(),
                List.of(new ItemSaleService.DispenseLine(1L, 1)));   // stub OK path

        DrugIntervention intervention =
                interventionService.create(507L, sale.getSaleId(), "DOSAGE_ADJUSTED", "renal", 9L);
        assertThat(intervention.getInterventionId()).isPositive();
        assertThat(interventionService.listByPatient(507L))
                .extracting(DrugIntervention::getInterventionId)
                .containsExactly(intervention.getInterventionId());

        var picklist = picklistService.create(12L, sale.getSaleId());
        assertThat(picklist.getPickedAt()).isNotNull();           // stamped server-side
        assertThatThrownBy(() -> picklistService.create(12L, 999_999L))
                .isInstanceOf(SaleNotFoundException.class)
                .extracting(e -> ((SaleNotFoundException) e).getErrorCode())
                .isEqualTo("NOT_FOUND");

        var stamped = projectionService.setEmrNo(777L, "EMR-88");
        assertThat(stamped.getEmrNo()).isEqualTo("EMR-88");
        assertThat(stamped.getClosed()).isTrue();                 // fully dispensed above
        assertThatThrownBy(() -> projectionService.setEmrNo(888_888L, "EMR-X"))
                .isInstanceOf(SaleNotFoundException.class)
                .hasMessageContaining("No pharmacy dispensing record");

        assertThat(outboxRepository.count()).isZero();            // NONE of these flows emit
    }

    // ------------------------------------------------------------------ helpers

    private com.hims.pharmacy.domain.model.ItemSaleDetail detail(long saleId, long lineNo) {
        return saleService.get(saleId).getDetails().stream()
                .filter(d -> d.getLineNo() == lineNo)
                .findFirst()
                .orElseThrow();
    }

    private java.util.Optional<com.hims.pharmacy.domain.model.PrescriptionDispense>
    projectionMasterOf(long prescriptionId) {
        return dispenseMasterRepository.findByPrescriptionId(prescriptionId);
    }

    private com.hims.pharmacy.domain.model.PrescriptionDispenseDetail projectionDetail(
            long prescriptionId, long lineNo) {
        var master = projectionMasterOf(prescriptionId).orElseThrow();
        return dispenseDetailRepository.findByDispenseIdOrderByLineNoAsc(master.getDispenseId())
                .stream()
                .filter(d -> d.getLineNo() == lineNo)
                .findFirst()
                .orElseThrow();
    }
}
