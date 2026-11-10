package com.hims.item.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.hims.item.application.StockService;
import com.hims.item.domain.model.ItemStockBalance;
import com.hims.item.domain.model.ItemStockLedger;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * Single-writer stock API (D-1). The ONLY stock mutation surface in the platform:
 * POST a movement -> balance row locked, ledger appended, event emitted (one tx).
 */
@RestController
@RequestMapping("/api/v1/stock")
@Tag(name = "Stock (MIG-012, D-1)", description = "Single-writer stock movements, balances, ledger")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @PostMapping("/movements")
    @Operation(summary = "Record a stock movement (single writer)",
            description = "operation: 1=ADD 2=SUBTRACT; movementType 1..30 (1=OPENING_BALANCE 2=GRN/RECEIPT 3=ISSUE 4=SALE 5=RETURN 6=TRANSFER_IN 7=TRANSFER_OUT 8=ADJUSTMENT 9=EXPIRY 10=DAMAGE 11=RECALL). "
                    + "Same tx: lock balance (FOR UPDATE) + ledger append + balance update + InventoryUpdated outbox event.")
    public ItemStockBalance movement(@RequestBody(required = false) JsonNode body) {
        return stockService.postMovement(
                body == null ? null : body.path("storeId").asLong(0) == 0 ? null : body.path("storeId").asLong(),
                body == null ? null : body.path("itemId").asLong(0) == 0 ? null : body.path("itemId").asLong(),
                body == null ? null : body.path("batchId").isMissingNode() || body.path("batchId").isNull()
                        ? null : body.path("batchId").asLong(),
                body == null ? null : body.path("departmentId").isMissingNode() || body.path("departmentId").isNull()
                        ? null : body.path("departmentId").asLong(),
                body == null ? null : body.path("movementType").isMissingNode() ? null : body.path("movementType").shortValue(),
                body == null ? null : body.path("operation").isMissingNode() ? null : body.path("operation").shortValue(),
                body == null ? null : body.path("quantity").isMissingNode() ? null : body.path("quantity").decimalValue(),
                body == null ? null : textOrNull(body, "refDocType"),
                body == null ? null : body.path("refDocId").isMissingNode() || body.path("refDocId").isNull()
                        ? null : body.path("refDocId").asLong(),
                body == null ? null : textOrNull(body, "remarks"));
    }

    @GetMapping("/balances")
    public List<ItemStockBalance> balances(@RequestParam(required = false) Long storeId,
                                           @RequestParam(required = false) Long itemId) {
        return stockService.getBalances(storeId, itemId);
    }

    @GetMapping("/ledger")
    public List<ItemStockLedger> ledger(@RequestParam Long storeId, @RequestParam Long itemId) {
        return stockService.getLedger(storeId, itemId);
    }

    @GetMapping("/available-batches")
    @Operation(summary = "FEFO batch availability for dispensing/issue (UNKNOWN-012 resolved)",
            description = "Batches of one item in one store with availableQty > 0, not expired "
                    + "(expiryDate IS NULL OR > today), not freeze-recalled, ordered FEFO: "
                    + "expiryDate ASC NULLS LAST, batchId ASC. Legacy parity: "
                    + "CIMS_GETITEMBATCHLISTFORSEARCH_NEWONE_OUTER_IMPROVED (Order By ExpiryDate,BatchId).")
    public List<com.hims.item.application.BatchAvailabilityView> availableBatches(@RequestParam Long storeId,
                                                                                  @RequestParam Long itemId) {
        return stockService.getAvailableBatches(storeId, itemId);
    }

    @PostMapping("/adjustments")
    @Operation(summary = "Stock adjustment (M13)",
            description = "M13 via ItemStockLedger movementType 8 ADJUSTMENT (legacy stockAdjusmentDetails.vm). "
                    + "operation 1=ADD (excess found) 2=SUBTRACT (shortage), quantity >0, reason required. "
                    + "Reuses D-1 postMovement + ledger + InventoryUpdated event.")
    public ItemStockBalance adjust(@RequestBody(required = false) JsonNode body) {
        return stockService.adjustStock(
                body == null ? null : body.path("storeId").asLong(0) == 0 ? null : body.path("storeId").asLong(),
                body == null ? null : body.path("itemId").asLong(0) == 0 ? null : body.path("itemId").asLong(),
                body == null ? null : body.path("batchId").isMissingNode() || body.path("batchId").isNull() ? null : body.path("batchId").asLong(),
                body == null ? null : body.path("quantity").isMissingNode() ? null : body.path("quantity").decimalValue(),
                body == null ? null : body.path("operation").isMissingNode() ? null : body.path("operation").shortValue(),
                body == null ? null : textOrNull(body, "reason"));
    }

    @PostMapping("/consumptions")
    @Operation(summary = "Stock consumption (M13 ward consumption)",
            description = "M13 consumption via movementType 13 CONSUMPTION (subtract). quantity >0.")
    public ItemStockBalance consume(@RequestBody(required = false) JsonNode body) {
        return stockService.consumeStock(
                body == null ? null : body.path("storeId").asLong(0) == 0 ? null : body.path("storeId").asLong(),
                body == null ? null : body.path("itemId").asLong(0) == 0 ? null : body.path("itemId").asLong(),
                body == null ? null : body.path("batchId").isMissingNode() || body.path("batchId").isNull() ? null : body.path("batchId").asLong(),
                body == null ? null : body.path("quantity").isMissingNode() ? null : body.path("quantity").decimalValue(),
                body == null ? null : textOrNull(body, "reason"));
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return (v.isMissingNode() || v.isNull()) ? null : v.asText();
    }
}
