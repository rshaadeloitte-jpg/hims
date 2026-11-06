package com.hims.rmrs.application;

import java.math.BigDecimal;

/**
 * Projection row of GET /api/v1/collections/summary/service-wise — the clean
 * analog of legacy s_servicewisecollection (register WP-R5).
 * Constructor expression target of RmrsCollectionRepository.summaryServiceWise.
 */
public record ServiceWiseSummaryRow(Long headId, String headCode, String headName,
                                    BigDecimal totalAmount, Long collectionCount) {}
