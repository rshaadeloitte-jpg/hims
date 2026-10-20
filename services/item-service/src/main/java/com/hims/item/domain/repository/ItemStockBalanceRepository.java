package com.hims.item.domain.repository;

import com.hims.item.domain.model.ItemStockBalance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ItemStockBalanceRepository extends JpaRepository<ItemStockBalance, Long> {

    /**
     * D-1 single-writer: the stock API locks the balance row (SELECT ... FOR UPDATE)
     * so concurrent movements on the same (store,item,batch) serialize.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT b FROM ItemStockBalance b
            WHERE b.storeId = :storeId AND b.itemId = :itemId
              AND ((:batchId IS NULL AND b.batchId IS NULL) OR b.batchId = :batchId)
            """)
    Optional<ItemStockBalance> findByStoreItemBatchForUpdate(@Param("storeId") Long storeId,
                                                             @Param("itemId") Long itemId,
                                                             @Param("batchId") Long batchId);

    List<ItemStockBalance> findByStoreIdAndItemId(Long storeId, Long itemId);

    List<ItemStockBalance> findByStoreId(Long storeId);

    /**
     * M13 item enquiry (read-side, V3): all balance rows for an item across
     * stores, ordered by store for a stable UI listing. Used by
     * {@code GET /api/v1/items/{id}/stock?storeId=...} when {@code storeId} is
     * omitted. With {@code storeId} set the controller delegates to
     * {@link #findByStoreIdAndItemId} (single-store) for the same shape.
     */
    List<ItemStockBalance> findByItemIdOrderByStoreIdAsc(Long itemId);

    /**
     * FEFO batch availability (MIG-012b, UNKNOWN-012 resolved from legacy procs).
     * Parity with {@code CIMS_GETITEMBATCHLISTFORSEARCH_NEWONE_OUTER_IMPROVED}:
     * <ul>
     *   <li>only batch-scoped balance rows with {@code AvailableStock > 0}</li>
     *   <li>not expired: {@code ExpiryDate IS NULL OR ExpiryDate > today}
     *       (a batch expiring today is NOT available — legacy {@code > cast(sysdate as date)})</li>
     *   <li>not freeze-recalled: {@code COALESCE(IsFreeze_Recall,0)=0}</li>
     *   <li>FEFO order: {@code ORDER BY ExpiryDate, BatchId} — null expiry last</li>
     * </ul>
     */
    @Query("""
            SELECT new com.hims.item.application.BatchAvailabilityView(
                ib.id, ib.batchCode, ib.expiryDate, b.availableQty,
                ib.mrp, ib.purchaseRate, ib.vatPercentage, ib.vatAmount,
                ib.discountOnSale, ib.conversionFactor, ib.isFreezeRecall,
                i.id, i.itemName, i.itemCode)
            FROM ItemStockBalance b
            JOIN ItemBatch ib ON ib.id = b.batchId
            JOIN Item i ON i.id = b.itemId
            WHERE b.storeId = :storeId AND b.itemId = :itemId
              AND b.availableQty > 0
              AND (ib.expiryDate IS NULL OR ib.expiryDate > CURRENT_DATE)
              AND COALESCE(ib.isFreezeRecall, false) = false
            ORDER BY ib.expiryDate ASC NULLS LAST, ib.id ASC
            """)
    List<com.hims.item.application.BatchAvailabilityView> findAvailableBatchesFefo(
            @Param("storeId") Long storeId, @Param("itemId") Long itemId);
}
