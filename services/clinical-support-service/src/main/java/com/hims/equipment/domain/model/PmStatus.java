package com.hims.equipment.domain.model;

/**
 * PM execution lifecycle dictionary (eqp_preventive_maintenance.pm_status,
 * V3 CHECK-enforced 1..4). Ids ARE the codes stored in the column, same shape
 * as {@link ComplaintStatus} for complaint_status:
 *
 * <pre>
 *   1 SCHEDULED   — planned occurrence, not started
 *   2 IN_PROGRESS — engineer working on the asset
 *   3 COMPLETED   — service done (the "record completion" default)
 *   4 CANCELLED   — aborted / not performed
 * </pre>
 */
public final class PmStatus {

    public static final long SCHEDULED = 1L;
    public static final long IN_PROGRESS = 2L;
    public static final long COMPLETED = 3L;
    public static final long CANCELLED = 4L;

    private PmStatus() {
    }

    public static boolean isValid(Long status) {
        return status != null && status >= SCHEDULED && status <= CANCELLED;
    }
}
