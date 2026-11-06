package com.hims.equipment.api;

import java.util.List;

/**
 * AMC / warranty count summary (GET /api/v1/equipment/warranties/counts).
 * All figures cover ACTIVE rows only; {@code expired} counts covers whose
 * end_date has already lapsed, {@code expiringIn30Days} the standard alert
 * window starting today. {@code byType} breaks active covers down per
 * warranty_type dictionary value (WARRANTY, AMC, ...).
 */
public record WarrantyCounts(long totalActive,
                             long expired,
                             long expiringIn30Days,
                             List<TypeCount> byType) {

    /** One warranty_type bucket of the breakdown. */
    public record TypeCount(String warrantyType, long count) {
    }
}
