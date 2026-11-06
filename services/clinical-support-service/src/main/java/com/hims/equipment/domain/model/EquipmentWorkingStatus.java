package com.hims.equipment.domain.model;

/**
 * Equipment operational-status dictionary (equipment_working_status_master).
 * Ids ARE the codes stored in equipment.working_status:
 *
 * <pre>
 *   1 WORKING      — asset in service
 *   2 NOT_WORKING  — asset out of service
 *   3 UNDER_REPAIR — asset with engineering / vendor
 *   4 CONDEMNED    — asset condemned; terminal, transfers rejected
 * </pre>
 */
public final class EquipmentWorkingStatus {

    public static final long WORKING = 1L;
    public static final long NOT_WORKING = 2L;
    public static final long UNDER_REPAIR = 3L;
    public static final long CONDEMNED = 4L;

    private EquipmentWorkingStatus() {
    }

    public static boolean isValid(Long status) {
        return status != null && status >= WORKING && status <= CONDEMNED;
    }
}
