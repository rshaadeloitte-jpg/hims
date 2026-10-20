package com.hims.equipment.domain.model;

/**
 * Complaint lifecycle dictionary (complaint_status_master, V1 seed). Ids ARE
 * the codes stored in equipment_complaint.complaint_status:
 *
 * <pre>
 *   1 OPEN        — raised, not yet picked up
 *   2 IN_PROGRESS — assigned / engineer working
 *   3 RESOLVED    — fix applied (resolution recorded)
 *   4 CLOSED      — closed after resolution (terminal)
 * </pre>
 */
public final class ComplaintStatus {

    public static final long OPEN = 1L;
    public static final long IN_PROGRESS = 2L;
    public static final long RESOLVED = 3L;
    public static final long CLOSED = 4L;

    private ComplaintStatus() {
    }

    public static boolean isValid(Long status) {
        return status != null && status >= OPEN && status <= CLOSED;
    }
}
