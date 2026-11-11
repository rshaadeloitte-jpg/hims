package com.hims.item.application;

import com.hims.item.application.exceptions.InvalidItemRequestException;
import com.hims.item.application.exceptions.ItemNotFoundException;
import com.hims.item.domain.model.ItemStockBalance;
import com.hims.item.domain.repository.ItemRepository;
import com.hims.item.domain.repository.ItemStockBalanceRepository;
import com.hims.item.domain.repository.ItemSupplierRepository;
import com.hims.item.domain.repository.StoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ItemEnquiryService {

    private static final Logger log = LoggerFactory.getLogger(ItemEnquiryService.class);

    public static final int DEFAULT_PAGE_SIZE = 50;
    public static final int MAX_PAGE_SIZE = 500;

    private final ItemRepository itemRepository;
    private final ItemStockBalanceRepository balanceRepository;
    private final ItemSupplierRepository itemSupplierRepository;
    private final StoreRepository storeRepository;
    private final StockService stockService;

    public ItemEnquiryService(ItemRepository itemRepository,
                              ItemStockBalanceRepository balanceRepository,
                              ItemSupplierRepository itemSupplierRepository,
                              StoreRepository storeRepository,
                              StockService stockService) {
        this.itemRepository = itemRepository;
        this.balanceRepository = balanceRepository;
        this.itemSupplierRepository = itemSupplierRepository;
        this.storeRepository = storeRepository;
        this.stockService = stockService;
    }

    @Transactional(readOnly = true)
    public List<ItemEnquiryView.SearchRow> search(String name, Long storeId,
                                                  boolean includeZeroStock,
                                                  int page, int size) {
        String trimmed = name == null ? null : name.trim();
        if (trimmed != null && trimmed.isEmpty()) {
            trimmed = null;
        }
        int pageIdx = normalisePage(page);
        int sizeN = normaliseSize(size);

        long total = itemRepository.countByName(trimmed);
        List<Object[]> rows = itemRepository.searchByNameRaw(trimmed, sizeN, pageIdx * sizeN);
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, BigDecimal> stockByItem = Collections.emptyMap();
        if (storeId != null) {
            if (!storeRepository.existsById(storeId)) {
                throw new ItemNotFoundException("NOT_FOUND", "Store not found");
            }
            stockByItem = balanceRepository.findByStoreId(storeId).stream()
                    .filter(b -> b.getItemId() != null)
                    .collect(Collectors.toMap(
                            ItemStockBalance::getItemId,
                            b -> b.getAvailableQty() == null ? BigDecimal.ZERO : b.getAvailableQty(),
                            BigDecimal::add));
        }

        List<ItemEnquiryView.SearchRow> out = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            Long itemId = asLong(r[0]);
            BigDecimal qty = storeId == null ? null : stockByItem.getOrDefault(itemId, BigDecimal.ZERO);
            if (storeId != null && !includeZeroStock && qty.signum() == 0) {
                continue;
            }
            out.add(new ItemEnquiryView.SearchRow(
                    itemId,
                    asString(r[1]),
                    asString(r[2]),
                    asString(r[3]),
                    asString(r[4]),
                    asLong(r[5]),
                    asLong(r[6]),
                    asDecimal(r[7]),
                    asDecimal(r[8]),
                    asDecimal(r[9]),
                    storeId,
                    qty,
                    total));
        }
        log.debug("Item enquiry search name='{}' storeId={} includeZero={} page={}/{} -> {} rows (of {} total)",
                trimmed, storeId, includeZeroStock, page, size, out.size(), total);
        return out;
    }

    @Transactional(readOnly = true)
    public List<BatchAvailabilityView> getBatches(Long itemId, Long storeId) {
        if (storeId == null) {
            throw new InvalidItemRequestException("BEXA003", "storeId is required");
        }
        if (!itemRepository.existsById(itemId)) {
            throw new ItemNotFoundException("NOT_FOUND", "Item not found");
        }
        return stockService.getAvailableBatches(storeId, itemId);
    }

    @Transactional(readOnly = true)
    public List<ItemStockBalance> getStock(Long itemId, Long storeId) {
        if (!itemRepository.existsById(itemId)) {
            throw new ItemNotFoundException("NOT_FOUND", "Item not found");
        }
        if (storeId != null) {
            if (!storeRepository.existsById(storeId)) {
                throw new ItemNotFoundException("NOT_FOUND", "Store not found");
            }
            return balanceRepository.findByStoreIdAndItemId(storeId, itemId);
        }
        return balanceRepository.findByItemIdOrderByStoreIdAsc(itemId);
    }

    @Transactional(readOnly = true)
    public List<ItemEnquiryView.SupplierLink> getSuppliers(Long itemId) {
        if (!itemRepository.existsById(itemId)) {
            throw new ItemNotFoundException("NOT_FOUND", "Item not found");
        }
        return itemSupplierRepository.findSuppliersForItem(itemId);
    }

    private static int normalisePage(int page) {
        return Math.max(0, page);
    }

    private static int normaliseSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private static Long asLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        return Long.valueOf(o.toString());
    }

    private static String asString(Object o) {
        return o == null ? null : o.toString();
    }

    private static Boolean asBoolean(Object o) {
        if (o == null) return null;
        if (o instanceof Boolean b) return b;
        if (o instanceof Number n) return n.intValue() != 0;
        return Boolean.valueOf(o.toString());
    }

    private static BigDecimal asDecimal(Object o) {
        if (o == null) return null;
        if (o instanceof BigDecimal bd) return bd;
        if (o instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(o.toString());
    }
}
