package com.hims.equipment.api;

import com.hims.equipment.domain.model.EquipmentUsageDetails;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Equipment usage report (GET /api/v1/equipment/usage/report). Aggregates
 * ACTIVE usage entries whose usage_date falls inside the effective window
 * [{@code from} 00:00, {@code to}+1d 00:00) — both bounds echoed back as the
 * inclusive dates actually applied after defaulting (trailing 30 days when no
 * range is supplied). {@code entries} are chronological (oldest first);
 * {@code totalHours} sums hours_used of exactly those entries (NULL hours
 * contribute nothing). Scope filters are echoed: null = whole register.
 */
public record UsageReport(Long unitId,
                          Long equipmentId,
                          LocalDate from,
                          LocalDate to,
                          int entryCount,
                          BigDecimal totalHours,
                          List<EquipmentUsageDetails> entries) {
}
