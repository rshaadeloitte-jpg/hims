package com.hims.pharmacy.application;

import com.hims.pharmacy.application.exceptions.InvalidSaleRequestException;
import com.hims.pharmacy.application.exceptions.IllegalSaleStateException;
import com.hims.pharmacy.application.exceptions.SaleNotFoundException;
import com.hims.pharmacy.application.exceptions.StockIssueException;
import com.hims.pharmacy.domain.model.ItemSale;
import com.hims.pharmacy.domain.model.ItemSaleDetail;
import com.hims.pharmacy.domain.repository.ItemSaleDetailRepository;
import com.hims.pharmacy.domain.repository.ItemSaleRepository;
import com.hims.pharmacy.application.DrugAllergyService;
import com.hims.pharmacy.application.DrugInteractionService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * L0 — pharmacy rules without Spring context (GAP-REGISTER-5 WP-R2).
 *
 * Pinned here:
 * - creation guards (patientId, >=1 line, itemId, integer qty > 0)
 * - BR-PHAR-001 full / partial / boundary(last-line) dispense flags
 * - BR-PHAR-002 close / partial-decrement delegation onto the local projection
 * - stock-call happy path (exact wire shape to item-service) and failure path
 *   (exception -> NOTHING persisted, sale would stay PENDING)
 * - lifecycle guards (FULLY_DISPENSED terminal, unknown line, qty > pending)
 *
 * OUTBOX NEGATIVE (structural, unit level): ItemSaleService has NO outbox
 * dependency at all — there is nothing to verify interactions against; the PG16
 * integration test asserts the outbox table stays EMPTY row-wise.
 */
class ItemSaleServiceTest {

    private static final String ITEM_BASE = "http://item";
    private static final long STORE_ID = 7L;

    private ItemSaleRepository saleRepository;
    private ItemSaleDetailRepository detailRepository;
    private PrescriptionDispenseService projectionService;
    private DrugAllergyService allergyService;
    private DrugInteractionService interactionService;
    private MockRestServiceServer itemServer;
    private ItemSaleService service;

    private final AtomicLong seq = new AtomicLong(100);
    private final Map<Long, ItemSale> salesById = new LinkedHashMap<>();
    private final Map<Long, List<ItemSaleDetail>> detailsBySale = new LinkedHashMap<>();

    @BeforeEach
    void setUp() {
        saleRepository = mock(ItemSaleRepository.class);
        detailRepository = mock(ItemSaleDetailRepository.class);
        projectionService = mock(PrescriptionDispenseService.class);
        allergyService = mock(DrugAllergyService.class);
        interactionService = mock(DrugInteractionService.class);

        EntityManager em = mock(EntityManager.class);
        Query seqQuery = mock(Query.class);
        when(seqQuery.getSingleResult()).thenAnswer(inv -> seq.incrementAndGet());
        when(em.createNativeQuery(contains("nextval"))).thenReturn(seqQuery);

        RestClient.Builder builder = RestClient.builder().baseUrl(ITEM_BASE);
        itemServer = MockRestServiceServer.bindTo(builder).build();
        ItemStockClient stockClient = new ItemStockClient(builder.build(),
                new com.fasterxml.jackson.databind.ObjectMapper());

        when(saleRepository.save(any())).thenAnswer(inv -> {
            ItemSale s = inv.getArgument(0);
            salesById.put(s.getSaleId(), s);
            return s;
        });
        when(saleRepository.findById(anyLong()))
                .thenAnswer(inv -> Optional.ofNullable(salesById.get(inv.getArgument(0, Long.class))));
        when(saleRepository.search(any(), any())).thenAnswer(inv -> List.copyOf(salesById.values()));
        when(detailRepository.save(any())).thenAnswer(inv -> {
            ItemSaleDetail d = inv.getArgument(0);
            detailsBySale.computeIfAbsent(d.getSaleId(), k -> new ArrayList<>()).add(d);
            return d;
        });
        when(detailRepository.findBySaleIdOrderByLineNoAsc(anyLong()))
                .thenAnswer(inv -> {
                    List<ItemSaleDetail> lines = detailsBySale
                            .getOrDefault(inv.getArgument(0, Long.class), List.of());
                    return new ArrayList<>(lines.stream()
                            .sorted(java.util.Comparator.comparing(ItemSaleDetail::getLineNo))
                            .toList());
                });

        service = new ItemSaleService(saleRepository, detailRepository, stockClient,
                projectionService, allergyService, interactionService, STORE_ID);
        ReflectionTestUtils.setField(service, "em", em);
    }

    private void stubStockOk(long expectedItemId, int expectedQty) {
        itemServer.expect(requestTo(ITEM_BASE + "/api/v1/stock/movements"))
                .andExpect(method(POST))
                .andExpect(jsonPath("$.storeId").value((int) STORE_ID))
                .andExpect(jsonPath("$.itemId").value((int) expectedItemId))
                .andExpect(jsonPath("$.movementType").value(4))     // SALE
                .andExpect(jsonPath("$.operation").value(2))        // SUBTRACT
                .andExpect(jsonPath("$.refDocType").value("PHARMACY_SALE"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"quantity\":" + expectedQty)))
                .andRespond(withSuccess(
                        "{\"id\":51,\"storeId\":" + STORE_ID + ",\"itemId\":" + expectedItemId
                                + ",\"batchId\":null,\"qtyInHand\":" + (1000 - expectedQty)
                                + ",\"availableQty\":" + (1000 - expectedQty) + "}",
                        APPLICATION_JSON));
    }

    // ------------------------------------------------------------- creation guards

    @Test
    void create_requiresPatientId_bexa003() {
        assertThatThrownBy(() -> service.create(null, null, List.of(line(501, 2))))
                .isInstanceOf(InvalidSaleRequestException.class)
                .extracting(e -> ((InvalidSaleRequestException) e).getErrorCode())
                .isEqualTo("BEXA003");
        assertThat(salesById).isEmpty();
    }

    @Test
    void create_requiresAtLeastOneLine_bexa003() {
        assertThatThrownBy(() -> service.create(11L, null, List.of()))
                .isInstanceOf(InvalidSaleRequestException.class)
                .hasMessageContaining("At least one sale line");
        assertThatThrownBy(() -> service.create(11L, null, null))
                .isInstanceOf(InvalidSaleRequestException.class)
                .hasMessageContaining("At least one sale line");
        assertThat(salesById).isEmpty();
    }

    @Test
    void create_rejectsMissingItemId_andNonPositiveQty_bexa003() {
        assertThatThrownBy(() -> service.create(11L, null,
                        List.of(new ItemSaleService.LineInput(null, null, 2))))
                .isInstanceOf(InvalidSaleRequestException.class)
                .hasMessageContaining("itemId");

        assertThatThrownBy(() -> service.create(11L, null, List.of(line(501, 0))))
                .isInstanceOf(InvalidSaleRequestException.class)
                .hasMessageContaining("qty must be > 0");

        assertThatThrownBy(() -> service.create(11L, null, List.of(line(501, -3))))
                .isInstanceOf(InvalidSaleRequestException.class)
                .hasMessageContaining("qty must be > 0");
        assertThat(salesById).isEmpty();
    }

    @Test
    void create_startsPendingWithPerLinePendingQty_noProjectionWithoutPrescription() {
        ItemSale sale = service.create(11L, null, List.of(line(501, 5), line(502, 2)));

        assertThat(sale.getStatusCode()).isEqualTo(ItemSale.STATUS_PENDING);
        assertThat(sale.getDispensed()).isFalse();          // BR-PHAR-001 header starts false
        assertThat(sale.getDetails())
                .extracting(ItemSaleDetail::getPendingQty)
                .containsExactly(5, 2);                     // pending_qty seeded = qty
        assertThat(sale.getDetails())
                .extracting(ItemSaleDetail::getDispensed)
                .containsExactly(false, false);
        verifyNoInteractions(projectionService);            // no prescriptionId -> nothing projected

        // structural negative-outbox guarantee: no event component exists on this service
        // (asserted row-wise by PharmacyServiceIntegrationTest).
    }

    @Test
    void create_withPrescription_seedsLocalProjection() {
        service.create(11L, 555L, List.of(line(501, 3)));
        ArgumentCaptor<List<ItemSaleDetail>> captor = ArgumentCaptor.forClass(List.class);
        verify(projectionService, times(1)).projectSaleLines(eq(555L), captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getItemId()).isEqualTo(501L);
    }

    // ------------------------------------------------------------- BR-PHAR-001

    @Test
    void brPhar001_fullDispense_singleLine_closesSale() {
        stubStockOk(501, 5);
        ItemSale sale = service.create(11L, null, List.of(line(501, 5)));

        ItemSale result = service.dispense(sale.getSaleId(),
                List.of(new ItemSaleService.DispenseLine(1L, 5)));   // boundary: qty == pending

        assertThat(result.getStatusCode()).isEqualTo(ItemSale.STATUS_FULLY_DISPENSED);
        assertThat(result.getDispensed()).isTrue();                 // header flag flips
        assertThat(result.getDetails().get(0).getDispensed()).isTrue();
        assertThat(result.getDetails().get(0).getPendingQty()).isZero();
        itemServer.verify();
    }

    @Test
    void brPhar001_partialDispense_keepsPartiallyDispensed_andDecrementsPendingQty() {
        // both stock expectations queued upfront (FIFO): MockRestServiceServer
        // forbids adding expectations once actual requests have been made.
        stubStockOk(501, 1);
        stubStockOk(501, 2);
        ItemSale sale = service.create(11L, null, List.of(line(501, 3)));

        ItemSale result = service.dispense(sale.getSaleId(),
                List.of(new ItemSaleService.DispenseLine(1L, 1)));

        assertThat(result.getStatusCode()).isEqualTo(ItemSale.STATUS_PARTIALLY_DISPENSED);
        assertThat(result.getDispensed()).isFalse();                // header flag stays false
        assertThat(result.getDetails().get(0).getPendingQty()).isEqualTo(2);
        assertThat(result.getDetails().get(0).getDispensed()).isFalse();
        assertThat(result.getDetails().get(0).getPartialDispense()).isTrue();

        // second partial hits the closing boundary exactly
        result = service.dispense(sale.getSaleId(),
                List.of(new ItemSaleService.DispenseLine(1L, 2)));
        assertThat(result.getStatusCode()).isEqualTo(ItemSale.STATUS_FULLY_DISPENSED);
        assertThat(result.getDispensed()).isTrue();
        assertThat(result.getDetails().get(0).getPendingQty()).isZero();
        itemServer.verify();
    }

    @Test
    void brPhar001_boundary_lastLineClosing_flipsHeaderExactlyOnce() {
        // both stock expectations queued upfront (FIFO) for the two dispense calls.
        stubStockOk(501, 2);
        stubStockOk(502, 1);
        ItemSale sale = service.create(11L, null, List.of(line(501, 2), line(502, 1)));

        ItemSale afterFirst = service.dispense(sale.getSaleId(),
                List.of(new ItemSaleService.DispenseLine(1L, 2)));
        assertThat(afterFirst.getStatusCode()).isEqualTo(ItemSale.STATUS_PARTIALLY_DISPENSED);
        assertThat(afterFirst.getDispensed()).isFalse();

        ItemSale afterLast = service.dispense(sale.getSaleId(),
                List.of(new ItemSaleService.DispenseLine(2L, 1)));   // last line closes the sale
        assertThat(afterLast.getStatusCode()).isEqualTo(ItemSale.STATUS_FULLY_DISPENSED);
        assertThat(afterLast.getDispensed()).isTrue();
        itemServer.verify();
    }

    // ------------------------------------------------------------- BR-PHAR-002

    @Test
    void brPhar002_fullQtyLine_closesProjectionDetail_pendingZero() {
        stubStockOk(501, 2);
        ItemSale sale = service.create(11L, 555L, List.of(line(501, 2)));

        service.dispense(sale.getSaleId(), List.of(new ItemSaleService.DispenseLine(1L, 2)));

        verify(projectionService, times(1))
                .applyDispenseOutcome(eq(555L), eq(1L), eq(true), eq(2));
    }

    @Test
    void brPhar002_partialLine_decrementsProjectionPending_staysOpen() {
        stubStockOk(501, 1);
        ItemSale sale = service.create(11L, 555L, List.of(line(501, 3)));

        service.dispense(sale.getSaleId(), List.of(new ItemSaleService.DispenseLine(1L, 1)));

        verify(projectionService, times(1))
                .applyDispenseOutcome(eq(555L), eq(1L), eq(false), eq(1));   // PendingQty -= qty
    }

    @Test
    void brPhar002_salesWithoutPrescription_neverTouchProjection() {
        stubStockOk(501, 1);
        ItemSale sale = service.create(11L, null, List.of(line(501, 1)));

        service.dispense(sale.getSaleId(), List.of(new ItemSaleService.DispenseLine(1L, 1)));

        verify(projectionService, never())
                .applyDispenseOutcome(anyLong(), anyLong(), anyBoolean(), anyInt());
    }

    // ------------------------------------------------------------- lifecycle guards

    @Test
    void dispense_fullyDispensedSale_rejected_phIllegalTransition() {
        stubStockOk(501, 1);
        ItemSale sale = service.create(11L, null, List.of(line(501, 1)));
        service.dispense(sale.getSaleId(), List.of(new ItemSaleService.DispenseLine(1L, 1)));

        assertThatThrownBy(() -> service.dispense(sale.getSaleId(),
                        List.of(new ItemSaleService.DispenseLine(1L, 1))))
                .isInstanceOf(IllegalSaleStateException.class)
                .extracting(e -> ((IllegalSaleStateException) e).getErrorCode())
                .isEqualTo("PH_ILLEGAL_TRANSITION");
    }

    @Test
    void dispense_unknownLine_orEmptyRequest_orQtyBeyondPending_rejected_bexa003() {
        ItemSale sale = service.create(11L, null, List.of(line(501, 2)));

        assertThatThrownBy(() -> service.dispense(sale.getSaleId(),
                        List.of(new ItemSaleService.DispenseLine(9L, 1))))
                .isInstanceOf(InvalidSaleRequestException.class)
                .hasMessageContaining("Unknown lineNo 9");

        assertThatThrownBy(() -> service.dispense(sale.getSaleId(), List.of()))
                .isInstanceOf(InvalidSaleRequestException.class)
                .hasMessageContaining("At least one dispense line");

        assertThatThrownBy(() -> service.dispense(sale.getSaleId(),
                        List.of(new ItemSaleService.DispenseLine(1L, 3))))   // > pending_qty=2
                .isInstanceOf(InvalidSaleRequestException.class)
                .hasMessageContaining("exceeds pending_qty 2");

        assertThatThrownBy(() -> service.dispense(sale.getSaleId(),
                        List.of(new ItemSaleService.DispenseLine(1L, 0))))
                .isInstanceOf(InvalidSaleRequestException.class)
                .hasMessageContaining("must be > 0");
        itemServer.verify();     // NO stock call was ever made for rejected requests
    }

    // ------------------------------------------------------------- stock call paths

    @Test
    void stock_failure_rollsBackEverything_nothingMarkedDispensed() {
        ItemSale sale = service.create(11L, null, List.of(line(501, 2)));

        itemServer.expect(requestTo(ITEM_BASE + "/api/v1/stock/movements"))
                .andExpect(method(POST))
                .andRespond(withServerError()
                        .body("{\"status\":\"ERROR\",\"errorCode\":\"BEXA003\","
                                + "\"message\":\"Insufficient stock\"}"));

        assertThatThrownBy(() -> service.dispense(sale.getSaleId(),
                        List.of(new ItemSaleService.DispenseLine(1L, 2))))
                .isInstanceOf(StockIssueException.class);

        // transaction rolled back in production; here we pin that NOTHING was
        // persisted beyond creation: header untouched, detail untouched.
        ItemSale untouched = salesById.get(sale.getSaleId());
        assertThat(untouched.getStatusCode()).isEqualTo(ItemSale.STATUS_PENDING);
        assertThat(untouched.getDispensed()).isFalse();
        assertThat(detailsBySale.get(sale.getSaleId())).hasSize(1);
        assertThat(detailsBySale.get(sale.getSaleId()).get(0).getPendingQty()).isEqualTo(2);
        assertThat(detailsBySale.get(sale.getSaleId()).get(0).getDispensed()).isFalse();
        itemServer.verify();
    }

    @Test
    void stock_batchRef_forwardedOnlyWhenNumeric() throws Exception {
        ItemSale sale = service.create(11L, null,
                List.of(new ItemSaleService.LineInput(501L, "42", 1),
                        new ItemSaleService.LineInput(502L, "BATCH-X", 1)));

        itemServer.expect(requestTo(ITEM_BASE + "/api/v1/stock/movements"))
                .andExpect(method(POST))
                .andExpect(jsonPath("$.batchId").value(42))            // numeric forwarded
                .andRespond(withSuccess("{\"id\":1,\"storeId\":7,\"itemId\":501,"
                        + "\"qtyInHand\":999}", APPLICATION_JSON));
        itemServer.expect(requestTo(ITEM_BASE + "/api/v1/stock/movements"))
                .andExpect(method(POST))
                .andExpect(jsonPath("$.batchId").doesNotExist())       // non-numeric omitted
                .andRespond(withSuccess("{\"id\":2,\"storeId\":7,\"itemId\":502,"
                        + "\"qtyInHand\":999}", APPLICATION_JSON));

        service.dispense(sale.getSaleId(), List.of(
                new ItemSaleService.DispenseLine(1L, 1),
                new ItemSaleService.DispenseLine(2L, 1)));
        itemServer.verify();
    }

    // ------------------------------------------------------------- queries

    @Test
    void get_unknownSale_mapsTo404Code() {
        assertThatThrownBy(() -> service.get(404L))
                .isInstanceOf(SaleNotFoundException.class)
                .extracting(e -> ((SaleNotFoundException) e).getErrorCode())
                .isEqualTo("NOT_FOUND");
    }

    // ------------------------------------------------------------- helpers

    private static ItemSaleService.LineInput line(long itemId, int qty) {
        return new ItemSaleService.LineInput(itemId, null, qty);
    }
}
