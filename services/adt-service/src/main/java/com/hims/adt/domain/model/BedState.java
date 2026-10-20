package com.hims.adt.domain.model;

/**
 * Bed occupancy state machine (L0-pinned). Codes per STATUS-CODE-DICTIONARY §A5
 * plus 2=CLEANING. isrelease (master-driven, BR-IPD-003) governs whether a status
 * allows allocation; the transition table below pins the operational contract so
 * no code path can move a bed into an impossible state.
 */
public enum BedState {

    RESET(0, true),        // available again after cancel
    FREE(1, true),
    CLEANING(2, false),    // post-transfer hold until housekeeping clears it
    FREE_PREDICATE(3, true),
    OCCUPIED(5, false),
    RELEASED(6, true);     // post-discharge release — BR-DIS-003 pinned literal

    private final short code;
    private final boolean releasable;

    BedState(int code, boolean releasable) {
        this.code = (short) code;
        this.releasable = releasable;
    }

    public short code() { return code; }
    public boolean isReleasable() { return releasable; }

    public static BedState fromCode(short code) {
        for (BedState s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("Unknown bed status code: " + code);
    }

    /** Claim for a patient (admission/transfer): only allocation-allowed states -> OCCUPIED. */
    public static boolean canClaim(short current) {
        return fromCode(current).isReleasable();
    }

    /** Transfer releases the vacated bed to CLEANING (BR-DIS-003 sibling rule; task contract). */
    public static short transferReleaseTarget() {
        return CLEANING.code;
    }

    /** Discharge release target: hard-pinned literal 6 (BR-DIS-003 / RISK R5 contract). */
    public static short dischargeReleaseTarget() {
        return RELEASED.code;
    }

    /** Housekeeping clears a CLEANING bed back to FREE. */
    public static boolean canFinishCleaning(short current) {
        return current == CLEANING.code;
    }
}
