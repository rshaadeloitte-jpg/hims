package com.hims.appointment.domain.model;

/**
 * Legacy VisitStatus 11/12 split-brain resolution (MIG-031; BR-OPD-007 /
 * BR-PHAR-001; STATUS-CODE-DICTIONARY §1 RISK R1).
 *
 * Legacy wrote the SAME numeric codes to TWO tables:
 *   - T_Appointment.VisitStatus = 11 "bill paid"  (AddPaymentServiceHelper:477,820)
 *     with guard "only advances if <11";
 *   - T_Appointment.VisitStatus = 12 "pharmacy fully dispensed"
 *     (MedicationServiceImpl:812), only reachable from 11 and only when no
 *     undispensed item sales remain;
 *   while the clinical flow wrote other codes to T_Encounter, and kiosk/queue
 *   getLastVisitStatus read only one side (EmergencyAndWalkinVisitServiceImpl:1861).
 *
 * Target: both sides of a visit are one aggregate in this service, so the two
 * tables cannot disagree anymore. These helpers keep the exact legacy advance
 * guards on the mirrored column and resolve the effective value for consumers
 * (queue/kiosk parity in event payloads).
 */
public final class LegacyVisitStatus {

    /** Bill paid / encounter checked out (billing side of the split brain). */
    public static final short BILLED = 11;
    /** Pharmacy fully dispensed for the visit (terminal). */
    public static final short DISPENSED = 12;

    private LegacyVisitStatus() {
    }

    /**
     * BR-OPD-007: a bill payment advances the visit to BILLED(11) but NEVER
     * regresses a higher value ("only advances if &lt;11"). Null-safe (no prior
     * write). Returns the new effective value.
     */
    public static short advanceBillPayment(Short current) {
        if (current == null || current < BILLED) {
            return BILLED;
        }
        return current;
    }

    /**
     * BR-PHAR-001: dispensing advances BILLED(11) -&gt; DISPENSED(12) only when no
     * undispensed item sales remain; anything else leaves the value unchanged.
     * Returns null when the advance did not apply (caller decides semantics).
     */
    public static Short advanceDispense(Short current, boolean undispensedItemsRemain) {
        if (current == null || current < BILLED) {
            return null;                    // legacy: 12 unreachable before 11
        }
        if (current >= DISPENSED) {
            return current;                 // already terminal — pending-item gate no longer applies
        }
        if (undispensedItemsRemain) {
            return null;                    // blocked while undispensed item sales remain
        }
        return DISPENSED;
    }

    /**
     * Split-brain read resolution (getLastVisitStatus parity): the effective
     * legacy visit state is the higher of the appointment-side mirror and the
     * encounter-side value. In the target both live in one aggregate so they
     * converge by construction; this keeps the read contract for queue/kiosk.
     */
    public static Short resolveEffective(Short appointmentSide, Short encounterSide) {
        if (appointmentSide == null) {
            return encounterSide;
        }
        if (encounterSide == null) {
            return appointmentSide;
        }
        return appointmentSide > encounterSide ? appointmentSide : encounterSide;
    }
}
