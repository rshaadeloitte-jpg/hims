package com.hims.item.api;

import com.hims.item.application.BatchAvailabilityView;
import com.hims.item.application.ItemEnquiryService;
import com.hims.item.application.ItemEnquiryView;
import com.hims.item.domain.model.ItemStockBalance;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/items")
@Tag(name = "Item Masters (MIG-012)", description = "Item / Store / Batch master CRUD + MasterDataChanged outbox + M13 item enquiry")
public class ItemEnquiryController {

    private final ItemEnquiryService enquiryService;

    public ItemEnquiryController(ItemEnquiryService enquiryService) {
        this.enquiryService = enquiryService;
    }

    @GetMapping("/search")
    @Operation(summary = "Item enquiry search (M13, V3 read-side)")
    public List<ItemEnquiryView.SearchRow> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false, defaultValue = "true") boolean includeZeroStock,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "50") int size) {
        return enquiryService.search(name, storeId, includeZeroStock, page, size);
    }

    @GetMapping("/{id}/batches")
    @Operation(summary = "FEFO batch availability for one item in one store")
    public List<BatchAvailabilityView> batches(@PathVariable Long id,
                                               @RequestParam Long storeId) {
        return enquiryService.getBatches(id, storeId);
    }

    @GetMapping("/{id}/stock")
    @Operation(summary = "Current stock balances for an item, optionally filtered by store")
    public List<ItemStockBalance> stock(@PathVariable Long id,
                                        @RequestParam(required = false) Long storeId) {
        return enquiryService.getStock(id, storeId);
    }

    @GetMapping("/{id}/suppliers")
    @Operation(summary = "Item-supplier links joined with the supplier master")
    public List<ItemEnquiryView.SupplierLink> suppliers(@PathVariable Long id) {
        return enquiryService.getSuppliers(id);
    }
}
