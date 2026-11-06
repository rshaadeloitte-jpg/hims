package com.hims.linen.domain.model;

/**
 * Linen request lifecycle (legacy LinenRequestStatus enum,
 * LinenLaundryServiceImpl). Request-level status only (linen has no per-bag
 * status — unlike bio-medical waste):
 *
 * <pre>
 *   1 PENDING    -> addNewLinenRequest (initial)
 *   2 ACCEPTED   -> approveLinenRequest
 *   3 HANDOVERED -> handOverLinenItems (handedOverTo stamped)
 *   4 RECEIVED   -> receiveLinenRequestMainStore (receivedQuantity/damagedQuantity/reason stamped)
 *   5 WASHED     -> processLinenRequest
 *   6 DISPATCHED -> dispatchLinenRequest (dispatchedQuantity stamped, damagedQuantity accumulated)
 *   7 COMPLETED  -> receiveLinenRequest (receivedBy + finalQuantity stamped)
 * </pre>
 *
 * Legacy performs raw UPDATEs with no from-status guard, so a request could be
 * moved backwards or re-approved. The clean target adds explicit forward-only
 * guards (documented improvement, IMP-DEC-034): each transition requires the
 * immediately preceding status.
 */
public final class LinenStatus {

    public static final long PENDING = 1L;
    public static final long ACCEPTED = 2L;
    public static final long HANDOVERED = 3L;
    public static final long RECEIVED = 4L;
    public static final long WASHED = 5L;
    public static final long DISPATCHED = 6L;
    public static final long COMPLETED = 7L;

    private LinenStatus() {
    }

    public static final java.util.List<Long> ALL = java.util.List.of(PENDING, ACCEPTED, HANDOVERED, RECEIVED, WASHED, DISPATCHED, COMPLETED);

    public static String label(long status) {
        if (status == PENDING) return "PENDING";
        if (status == ACCEPTED) return "ACCEPTED";
        if (status == HANDOVERED) return "HANDOVERED";
        if (status == RECEIVED) return "RECEIVED";
        if (status == WASHED) return "WASHED";
        if (status == DISPATCHED) return "DISPATCHED";
        if (status == COMPLETED) return "COMPLETED";
        return "UNKNOWN";
    }

    public static boolean isValid(long status) {
        return status >= PENDING && status <= COMPLETED;
    }
}
