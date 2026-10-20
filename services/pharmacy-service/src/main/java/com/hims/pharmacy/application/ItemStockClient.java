package com.hims.pharmacy.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.pharmacy.application.exceptions.StockIssueException;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Synchronous pharmacy -> item-service REST client (GAP-REGISTER-5 WP-R2).
 * STOCK STAYS IN item-service (single-writer D-1); the legacy pharmacy dual-write
 * (CIMS_ADDITEMSTOCK / PharmacyServiceImpl:2140) is retired, NOT replicated here.
 *
 * Wire contract (mirrors services/item-service StockController/StockService):
 *   POST /api/v1/stock/movements
 *   body : {storeId, itemId, batchId?, departmentId?, movementType, operation,
 *           quantity, refDocType?, refDocId?, remarks?}
 *   reply: ItemStockBalance JSON {id, storeId, itemId, batchId, qtyInHand, ...}
 * Pharmacy issues with movementType=4 (SALE), operation=2 (SUBTRACT),
 * refDocType="PHARMACY_SALE", refDocId=saleId.
 * Pharmacy returns with movementType=5 (SALE_RETURN), operation=1 (ADD),
 * refDocType="PHARMACY_RETURN", refDocId=returnId.
 *
 * Any connection error, non-2xx response or unparseable body throws
 * {@link StockIssueException} so the dispense transaction rolls back (sale stays
 * PENDING, nothing marked dispensed). The sale line's batch_id is VARCHAR(50)
 * while the stock API takes a numeric batch reference: it is forwarded when it
 * parses as a Long, otherwise omitted (documented mapping decision — the register
 * does not constrain it and guessing would corrupt the single-writer ledger).
 */
public class ItemStockClient {

    public static final short MOVEMENT_TYPE_SALE = 4;
    public static final short OPERATION_SUBTRACT = 2;
    public static final String REF_DOC_TYPE = "PHARMACY_SALE";

    public static final short MOVEMENT_TYPE_SALE_RETURN = 5;
    public static final short OPERATION_ADD = 1;
    public static final String REF_DOC_TYPE_RETURN = "PHARMACY_RETURN";

    private final RestClient restClient;
    private final ObjectMapper mapper;

    public ItemStockClient(RestClient restClient, ObjectMapper mapper) {
        this.restClient = restClient;
        this.mapper = mapper;
    }

    /**
     * Issue (SUBTRACT) {@code qty} of one sale line against the pharmacy store.
     * Returns the resulting balance view from item-service.
     */
    public BalanceView issueForSale(long storeId, long itemId, String batchId, int qty,
                                    long saleId, long lineNo) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("storeId", storeId);
        request.put("itemId", itemId);
        request.put("batchId", parseBatchRef(batchId));
        request.put("movementType", MOVEMENT_TYPE_SALE);
        request.put("operation", OPERATION_SUBTRACT);
        request.put("quantity", BigDecimal.valueOf(qty));
        request.put("refDocType", REF_DOC_TYPE);
        request.put("refDocId", saleId);
        request.put("remarks", "pharmacy dispense sale " + saleId + " line " + lineNo);
        return postMovement(request, "sale " + saleId + " line " + lineNo, storeId, itemId);
    }

    /**
     * Return (ADD) {@code qty} of a returned sale line back to the pharmacy store.
     * Used by sale returns: BR-PHAR-003 — every APPROVED return POSTs an ADD
     * movement to item-service (single writer D-1) — NO double-write to legacy
     * dual-stock tables.
     */
    public BalanceView returnForSale(long storeId, long itemId, String batchId, int qty,
                                     long returnId, long lineNo) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("storeId", storeId);
        request.put("itemId", itemId);
        request.put("batchId", parseBatchRef(batchId));
        request.put("movementType", MOVEMENT_TYPE_SALE_RETURN);
        request.put("operation", OPERATION_ADD);
        request.put("quantity", BigDecimal.valueOf(qty));
        request.put("refDocType", REF_DOC_TYPE_RETURN);
        request.put("refDocId", returnId);
        request.put("remarks", "pharmacy return " + returnId + " line " + lineNo);
        return postMovement(request, "return " + returnId + " line " + lineNo, storeId, itemId);
    }

    private BalanceView postMovement(Map<String, Object> request, String opDesc, long storeId, long itemId) {
        try {
            String body = restClient.post()
                    .uri("/api/v1/stock/movements")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(String.class);
            if (body == null || body.isBlank()) {
                throw new StockIssueException(
                        "Empty response from item-service stock movement for " + opDesc);
            }
            JsonNode node = mapper.readTree(body);
            if (node.has("status") && "ERROR".equals(node.get("status").asText())) {
                throw new StockIssueException("item-service rejected the stock " + opDesc + ": "
                        + node.path("message").asText("(no message)"));
            }
            return new BalanceView(
                    node.path("id").asLong(0),
                    node.path("storeId").asLong(storeId),
                    node.path("itemId").asLong(itemId),
                    node.path("qtyInHand").asText("0"));
        } catch (StockIssueException e) {
            throw e;
        } catch (RestClientException e) {
            throw new StockIssueException("item-service stock movement call failed for " + opDesc, e);
        } catch (Exception e) {
            throw new StockIssueException("Unparseable item-service stock response for " + opDesc, e);
        }
    }

    /** Numeric batch reference when parseable, else null (documented mapping). */
    static Long parseBatchRef(String batchId) {
        if (batchId == null || batchId.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(batchId.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Subset of the item-service ItemStockBalance this service needs. */
    public record BalanceView(long balanceId, long storeId, long itemId, String qtyInHand) {}
}
