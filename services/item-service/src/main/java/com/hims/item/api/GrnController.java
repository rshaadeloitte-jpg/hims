package com.hims.item.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.hims.item.application.GrnService;
import com.hims.item.domain.model.Grn;
import com.hims.item.domain.model.GrnDetail;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * GRN posting (WP-2). One POST = one tx: grn header + lines (+ new batches),
 * stock ledger + balance updates through the D-1 StockService path
 * (SELECT ... FOR UPDATE), InventoryUpdated per line + GRNPosted aggregate event.
 */
@RestController
@RequestMapping("/api/v1/grn")
@Tag(name = "Procurement (WP-2)", description = "Supplier master, GRN posting, indents")
public class GrnController {

    private final GrnService grnService;

    public GrnController(GrnService grnService) {
        this.grnService = grnService;
    }

    @PostMapping
    @Operation(summary = "Post a GRN (goods received note)",
            description = "Body: {storeId, supplierId?, grnNo?, invoiceNo?, invoiceDate? (yyyy-MM-dd), "
                    + "receivedDate?, remarks?, lines:[{itemId, quantity, batchId? | batchCode+expiryDate+mrp+purchaseRate}]}. "
                    + "Every line appends a stock-ledger row (movementType=2 GRN/RECEIPT, operation=1 ADD) and updates "
                    + "item_stock_balance under SELECT ... FOR UPDATE; a single GRNPosted outbox event is emitted.")
    public Grn post(@RequestBody(required = false) JsonNode body) {
        return grnService.postGrn(body);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a GRN header by id")
    public Grn get(@PathVariable Long id) {
        return grnService.get(id);
    }

    @GetMapping("/{id}/lines")
    @Operation(summary = "Get GRN lines")
    public List<GrnDetail> lines(@PathVariable Long id) {
        return grnService.getDetails(id);
    }
}
