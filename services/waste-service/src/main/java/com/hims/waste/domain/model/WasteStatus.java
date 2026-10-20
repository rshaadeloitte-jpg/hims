package com.hims.waste.domain.model;

/**
 * Bio-medical waste request lifecycle (legacy BioMedicalWasteRequestStatus enum,
 * BioMedicalWasteManagementServiceImpl). Request-level status and per-bag
 * bag_status share the same 5-state dictionary:
 *
 * <pre>
 *   1 PENDING    -> raiseBioMedicalWasteRequest (initial)
 *   2 ACCEPTED   -> approveBioRequest
 *   3 HANDOVERED -> handoverBioRequest (handedOverTo stamped)
 *   4 RECEIVED   -> receiveBioRequest (receivedBy stamped)
 *   5 COMPLETED  -> processBioRequest (firmName/contactPerson/contactNo stamped)
 * </pre>
 *
 * Legacy performs raw UPDATEs with no from-status guard, so a request could be
 * moved backwards or re-approved. The clean target adds explicit forward-only
 * guards (documented improvement, IMP-DEC-033): each transition requires the
 * immediately preceding status.
 */
public final class WasteStatus {

    public static final long PENDING = 1L;
    public static final long ACCEPTED = 2L;
    public static final long HANDOVERED = 3L;
    public static final long RECEIVED = 4L;
    public static final long COMPLETED = 5L;

    private WasteStatus() {
    }

    public static boolean isValid(long status) {
        return status >= PENDING && status <= COMPLETED;
    }
}
