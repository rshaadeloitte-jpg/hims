package com.hims.appointment.domain.model;

import java.util.Map;
import java.util.Set;

/**
 * MIG-031 STATUS DICTIONARY LOCK (RT-51): the clean appointment lifecycle is
 * locked BEFORE the port. This is NOT the legacy VisitStatus space (codes 3..12
 * shared between T_Encounter/T_Appointment per STATUS-CODE-DICTIONARY §1) —
 * legacy VisitStatus values live only in {@code legacy_visit_status} columns.
 *
 * Locked dictionary:
 *   BOOKED=1, CHECKED_IN=2, COMPLETED=3, CANCELLED=4, RESCHEDULED=5, NO_SHOW=6
 *
 * Forward-only transition matrix (tests assert transitions, never raw values):
 *   BOOKED      -> CHECKED_IN | CANCELLED | RESCHEDULED | NO_SHOW
 *   CHECKED_IN  -> COMPLETED  | CANCELLED                (legacy allowed cancel after check-in)
 *   COMPLETED / CANCELLED / RESCHEDULED / NO_SHOW        terminal
 */
public enum AppointmentStatus {

    BOOKED((short) 1),
    CHECKED_IN((short) 2),
    COMPLETED((short) 3),
    CANCELLED((short) 4),
    RESCHEDULED((short) 5),
    NO_SHOW((short) 6);

    /** Statuses whose slot must stay consumed (historical day roster). */
    public static final Set<AppointmentStatus> SLOT_CONSUMING =
            Set.of(BOOKED, CHECKED_IN, COMPLETED, NO_SHOW);

    private static final Map<AppointmentStatus, Set<AppointmentStatus>> TRANSITIONS = Map.of(
            BOOKED, Set.of(CHECKED_IN, CANCELLED, RESCHEDULED, NO_SHOW),
            CHECKED_IN, Set.of(COMPLETED, CANCELLED),
            COMPLETED, Set.of(),
            CANCELLED, Set.of(),
            RESCHEDULED, Set.of(),
            NO_SHOW, Set.of());

    private final short code;

    AppointmentStatus(short code) {
        this.code = code;
    }

    public short code() {
        return code;
    }

    public boolean canTransitionTo(AppointmentStatus target) {
        return TRANSITIONS.get(this).contains(target);
    }

    public static AppointmentStatus fromCode(short code) {
        for (AppointmentStatus s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown appointment status code: " + code);
    }
}
