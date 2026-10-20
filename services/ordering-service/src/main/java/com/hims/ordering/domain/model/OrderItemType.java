package com.hims.ordering.domain.model;

/**
 * Clinical order item type (BR-ORD-002). Selects the downstream saga consumer:
 * LAB -> Lab service books the path order; RADIOLOGY -> Rad service books the
 * scan; NURSING -> Nursing service schedules the procedure; CONSULTATION needs
 * only the Billing step (the legacy mega-txn always billed every line).
 */
public enum OrderItemType {
    LAB, RADIOLOGY, NURSING, CONSULTATION;

    /** Parses a raw string into a type or throws BEXA003-style IllegalArgumentException. */
    public static OrderItemType of(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("itemType is required per line");
        }
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unknown itemType: " + raw + " (expected LAB|RADIOLOGY|NURSING|CONSULTATION)");
        }
    }
}
