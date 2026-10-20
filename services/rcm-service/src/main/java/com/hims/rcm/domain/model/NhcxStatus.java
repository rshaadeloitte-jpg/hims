package com.hims.rcm.domain.model;

/**
 * NHCX status enum SMALLINT 0..3 per register WP-R4 (06 §A9 source block):
 * 0=INITIATED 1=SENT 2=ACKNOWLEDGED 3=COMPLETED/ERROR (terminal).
 *
 * DEVIATION NOTE (documented, register WP-R4 report): docs §A9 as captured only
 * evidences 0=ACCEPTED .. 3=FAILED with labels 1..2 UNKNOWN
 * (STATUS-CODE-DICTIONARY "NHCX request status 1–2"); the register's own
 * binding mapping above is authoritative here and pinned by tests.
 *
 * Transition machine (forward-only, monotonic):
 *   0 -> 1|2|3, 1 -> 2|3, 2 -> 3; re-applying the CURRENT status is an
 *   idempotent no-op success (callback contract); anything else is a
 *   400 RCM_ILLEGAL_TRANSITION.
 */
public final class NhcxStatus {

    public static final short INITIATED = 0;
    public static final short SENT = 1;
    public static final short ACKNOWLEDGED = 2;
    public static final short COMPLETED = 3;   // terminal (COMPLETED/ERROR per source)

    private NhcxStatus() {}

    /** True when {@code from -> to} is a legal forward transition or an idempotent repeat. */
    public static boolean canTransition(short from, short to) {
        return to >= from && to >= INITIATED && to <= COMPLETED && from >= INITIATED && from <= COMPLETED;
    }

    public static boolean isValid(short s) {
        return s >= INITIATED && s <= COMPLETED;
    }

    public static String name(short s) {
        return switch (s) {
            case INITIATED -> "INITIATED";
            case SENT -> "SENT";
            case ACKNOWLEDGED -> "ACKNOWLEDGED";
            case COMPLETED -> "COMPLETED";
            default -> String.valueOf(s);
        };
    }
}
