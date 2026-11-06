package com.hims.rmrs.application;

import java.math.BigDecimal;

/**
 * Projection row of GET /api/v1/collections/summary/user-wise — the clean
 * analog of legacy s_userwisecollection (register WP-R5).
 * Constructor expression target of RmrsCollectionRepository.summaryUserWise.
 */
public record UserWiseSummaryRow(String collectedBy, BigDecimal totalAmount, Long collectionCount) {}
