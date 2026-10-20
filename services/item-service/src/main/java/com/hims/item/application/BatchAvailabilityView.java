package com.hims.item.application;

import java.math.BigDecimal;
import java.sql.Date;

/**
 * FEFO batch availability projection (MIG-012b, UNKNOWN-012 resolved).
 * Legacy parity: {@code CIMS_GETITEMBATCHLISTFORSEARCH_NEWONE_OUTER_IMPROVED} —
 * batches of one item in one store with {@code AvailableStock > 0}, not expired
 * ({@code ExpiryDate IS NULL OR ExpiryDate > today}), not freeze-recalled
 * ({@code COALESCE(IsFreeze_Recall,0)=0}), ordered FEFO:
 * {@code ORDER BY ExpiryDate, BatchId} (null expiry last).
 */
public record BatchAvailabilityView(
        Long batchId,
        String batchCode,
        Date expiryDate,
        BigDecimal availableQty,
        BigDecimal mrp,
        BigDecimal purchaseRate,
        BigDecimal vatPercentage,
        BigDecimal vatAmount,
        BigDecimal discountOnSale,
        BigDecimal conversionFactor,
        Boolean freezeRecall,
        Long itemId,
        String itemName,
        String itemCode) {
}
