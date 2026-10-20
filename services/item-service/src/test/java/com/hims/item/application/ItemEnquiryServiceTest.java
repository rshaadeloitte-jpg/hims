package com.hims.item.application;

import com.hims.item.application.exceptions.InvalidItemRequestException;
import com.hims.item.application.exceptions.ItemNotFoundException;
import com.hims.item.domain.model.ItemStockBalance;
import com.hims.item.domain.repository.ItemRepository;
import com.hims.item.domain.repository.ItemStockBalanceRepository;
import com.hims.item.domain.repository.ItemSupplierRepository;
import com.hims.item.domain.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ItemEnquiryServiceTest {

    @Mock private ItemRepository itemRepository;
    @Mock private ItemStockBalanceRepository balanceRepository;
    @Mock private ItemSupplierRepository itemSupplierRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private StockService stockService;

    private ItemEnquiryService service;

    @BeforeEach
    void setUp() {
        service = new ItemEnquiryService(itemRepository, balanceRepository,
                itemSupplierRepository, storeRepository, stockService);
    }

    @Test
    void search_nameOnly_returnsRowsWithTotalCount() {
        java.util.List<Object[]> row1 = java.util.Collections.singletonList(searchRow(1001L, "CEF1", "Ceftriaxone 1g",
                "ACME", "1g", 1L, 11L,
                new BigDecimal("23.50"), new BigDecimal("28.00"), new BigDecimal("28.00")));
        when(itemRepository.countByName("cef")).thenReturn(7L);
        when(itemRepository.searchByNameRaw(eq("cef"), anyInt(), anyInt())).thenReturn(row1);

        List<ItemEnquiryView.SearchRow> rows = service.search("cef", null, true, 0, 50);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).itemId()).isEqualTo(1001L);
        assertThat(rows.get(0).itemName()).isEqualTo("Ceftriaxone 1g");
        assertThat(rows.get(0).brandName()).isEqualTo("ACME");
        assertThat(rows.get(0).totalRows()).isEqualTo(7L);
        assertThat(rows.get(0).storeId()).isNull();
        assertThat(rows.get(0).availableQty()).isNull();
    }

    @Test
    void search_blankName_isTreatedAsNull() {
        when(itemRepository.countByName(null)).thenReturn(0L);
        when(itemRepository.searchByNameRaw(eq(null), anyInt(), anyInt()))
                .thenReturn(java.util.Collections.emptyList());
        assertThat(service.search("   ", null, true, 0, 50)).isEmpty();
    }

    @Test
    void search_withStoreId_aggregatesAvailableQty() {
        java.util.List<Object[]> row2 = java.util.Collections.singletonList(searchRow(1001L, "CEF1", "Ceftriaxone 1g",
                null, "1g", 1L, 11L, null, null, null));
        when(itemRepository.countByName("cef")).thenReturn(1L);
        when(itemRepository.searchByNameRaw(eq("cef"), anyInt(), anyInt())).thenReturn(row2);
        when(storeRepository.existsById(10L)).thenReturn(true);

        ItemStockBalance b1 = new ItemStockBalance();
        b1.setItemId(1001L);
        b1.setStoreId(10L);
        b1.setAvailableQty(new BigDecimal("40"));
        ItemStockBalance b2 = new ItemStockBalance();
        b2.setItemId(1001L);
        b2.setStoreId(10L);
        b2.setAvailableQty(new BigDecimal("2"));
        ItemStockBalance other = new ItemStockBalance();
        other.setItemId(2002L);
        other.setStoreId(10L);
        other.setAvailableQty(new BigDecimal("99"));
        when(balanceRepository.findByStoreId(10L)).thenReturn(List.of(b1, b2, other));

        List<ItemEnquiryView.SearchRow> rows = service.search("cef", 10L, true, 0, 50);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).storeId()).isEqualTo(10L);
        assertThat(rows.get(0).availableQty()).isEqualByComparingTo("42");
    }

    @Test
    void search_includeZeroStockFalse_dropsZeroQtyRows() {
        java.util.List<Object[]> rows3 = java.util.Arrays.asList(
                searchRow(1001L, "Z1", "Out of stock", null, null, 1L, null, null, null, null),
                searchRow(1002L, "I1", "In stock", null, null, 1L, null, null, null, null));
        when(itemRepository.countByName("x")).thenReturn(2L);
        when(itemRepository.searchByNameRaw(eq("x"), anyInt(), anyInt())).thenReturn(rows3);
        when(storeRepository.existsById(10L)).thenReturn(true);
        when(balanceRepository.findByStoreId(10L)).thenReturn(List.of(balance(1002L, 10L, "5")));

        List<ItemEnquiryView.SearchRow> rows = service.search("x", 10L, false, 0, 50);
        assertThat(rows).extracting(ItemEnquiryView.SearchRow::itemId).containsExactly(1002L);
    }

    @Test
    void search_unknownStore_returnsNotFound() {
        java.util.List<Object[]> row = java.util.Collections.singletonList(searchRow(1001L, "X1", "X", null, null, 1L, null, null, null, null));
        when(itemRepository.countByName("x")).thenReturn(1L);
        when(itemRepository.searchByNameRaw(eq("x"), anyInt(), anyInt())).thenReturn(row);
        when(storeRepository.existsById(99L)).thenReturn(false);
        assertThatThrownBy(() -> service.search("x", 99L, true, 0, 50))
                .isInstanceOf(ItemNotFoundException.class);
    }

    @Test
    void search_pageSize_clampedToMax() {
        when(itemRepository.countByName(null)).thenReturn(0L);
        when(itemRepository.searchByNameRaw(eq(null), anyInt(), anyInt()))
                .thenReturn(java.util.Collections.emptyList());
        service.search(null, null, true, 0, 10000);
    }

    @Test
    void batches_missingStoreId_isBadRequest() {
        assertThatThrownBy(() -> service.getBatches(1001L, null))
                .isInstanceOf(InvalidItemRequestException.class);
    }

    @Test
    void batches_unknownItem_isNotFound() {
        when(itemRepository.existsById(1001L)).thenReturn(false);
        assertThatThrownBy(() -> service.getBatches(1001L, 10L))
                .isInstanceOf(ItemNotFoundException.class);
    }

    @Test
    void batches_delegatesToStockService() {
        when(itemRepository.existsById(1001L)).thenReturn(true);
        BatchAvailabilityView view = new BatchAvailabilityView(
                301L, "NEAR", java.sql.Date.valueOf("2027-01-31"),
                new BigDecimal("100"), null, null, null, null, null, null, false,
                1001L, "Ceftriaxone 1g", "CEF1");
        when(stockService.getAvailableBatches(10L, 1001L)).thenReturn(List.of(view));
        List<BatchAvailabilityView> rows = service.getBatches(1001L, 10L);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).batchCode()).isEqualTo("NEAR");
    }

    @Test
    void stock_unknownItem_isNotFound() {
        when(itemRepository.existsById(1001L)).thenReturn(false);
        assertThatThrownBy(() -> service.getStock(1001L, null))
                .isInstanceOf(ItemNotFoundException.class);
    }

    @Test
    void stock_noStoreId_returnsAllStores() {
        when(itemRepository.existsById(1001L)).thenReturn(true);
        when(balanceRepository.findByItemIdOrderByStoreIdAsc(1001L))
                .thenReturn(List.of(balance(1001L, 10L, "5"), balance(1001L, 20L, "3")));
        List<ItemStockBalance> rows = service.getStock(1001L, null);
        assertThat(rows).hasSize(2);
        assertThat(rows).extracting(ItemStockBalance::getStoreId).containsExactly(10L, 20L);
    }

    @Test
    void stock_withStoreId_delegatesToStoreItemQuery() {
        when(itemRepository.existsById(1001L)).thenReturn(true);
        when(storeRepository.existsById(10L)).thenReturn(true);
        when(balanceRepository.findByStoreIdAndItemId(10L, 1001L))
                .thenReturn(List.of(balance(1001L, 10L, "5")));
        List<ItemStockBalance> rows = service.getStock(1001L, 10L);
        assertThat(rows).hasSize(1);
    }

    @Test
    void stock_withUnknownStore_isNotFound() {
        when(itemRepository.existsById(1001L)).thenReturn(true);
        when(storeRepository.existsById(99L)).thenReturn(false);
        assertThatThrownBy(() -> service.getStock(1001L, 99L))
                .isInstanceOf(ItemNotFoundException.class);
    }

    @Test
    void suppliers_unknownItem_isNotFound() {
        when(itemRepository.existsById(1001L)).thenReturn(false);
        assertThatThrownBy(() -> service.getSuppliers(1001L))
                .isInstanceOf(ItemNotFoundException.class);
    }

    @Test
    void suppliers_emptyListWhenNoLinks() {
        when(itemRepository.existsById(1001L)).thenReturn(true);
        when(itemSupplierRepository.findSuppliersForItem(1001L)).thenReturn(java.util.Collections.emptyList());
        assertThat(service.getSuppliers(1001L)).isEmpty();
    }

    @Test
    void suppliers_delegatesToRepository() {
        when(itemRepository.existsById(1001L)).thenReturn(true);
        ItemEnquiryView.SupplierLink link = new ItemEnquiryView.SupplierLink(
                9001L, 501L, "ACME", "Acme Pharma", true, false, true, 11L);
        when(itemSupplierRepository.findSuppliersForItem(1001L)).thenReturn(List.of(link));

        List<ItemEnquiryView.SupplierLink> rows = service.getSuppliers(1001L);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).supplierCode()).isEqualTo("ACME");
    }

    private static Object[] searchRow(Long id, String code, String name,
                                      String brand, String strength,
                                      Long groupId, Long categoryId,
                                      BigDecimal purchaseRate, BigDecimal mrp, BigDecimal salePrice) {
        return new Object[]{id, code, name, brand, strength, groupId, categoryId, purchaseRate, mrp, salePrice};
    }

    private static ItemStockBalance balance(long itemId, long storeId, String qty) {
        ItemStockBalance b = new ItemStockBalance();
        b.setItemId(itemId);
        b.setStoreId(storeId);
        b.setAvailableQty(new BigDecimal(qty));
        return b;
    }
}
