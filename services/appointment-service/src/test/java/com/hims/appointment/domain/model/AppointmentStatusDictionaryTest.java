package com.hims.appointment.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * L0 — MIG-031 status dictionary lock gate (RT-51): the locked appointment
 * lifecycle codes and the forward-only transition matrix. Tests assert
 * transitions, never scattered magic values.
 */
class AppointmentStatusDictionaryTest {

    @Test
    void dictionary_isLocked() {
        assertThat(AppointmentStatus.BOOKED.code()).isEqualTo((short) 1);
        assertThat(AppointmentStatus.CHECKED_IN.code()).isEqualTo((short) 2);
        assertThat(AppointmentStatus.COMPLETED.code()).isEqualTo((short) 3);
        assertThat(AppointmentStatus.CANCELLED.code()).isEqualTo((short) 4);
        assertThat(AppointmentStatus.RESCHEDULED.code()).isEqualTo((short) 5);
        assertThat(AppointmentStatus.NO_SHOW.code()).isEqualTo((short) 6);
    }

    @Test
    void fromCode_roundTrips_andRejectsUnknown() {
        for (AppointmentStatus s : AppointmentStatus.values()) {
            assertThat(AppointmentStatus.fromCode(s.code())).isSameAs(s);
        }
        assertThatThrownBy(() -> AppointmentStatus.fromCode((short) 99))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void forwardOnly_transitions() {
        assertThat(AppointmentStatus.BOOKED.canTransitionTo(AppointmentStatus.CHECKED_IN)).isTrue();
        assertThat(AppointmentStatus.BOOKED.canTransitionTo(AppointmentStatus.CANCELLED)).isTrue();
        assertThat(AppointmentStatus.BOOKED.canTransitionTo(AppointmentStatus.RESCHEDULED)).isTrue();
        assertThat(AppointmentStatus.BOOKED.canTransitionTo(AppointmentStatus.NO_SHOW)).isTrue();
        assertThat(AppointmentStatus.BOOKED.canTransitionTo(AppointmentStatus.COMPLETED)).isFalse();

        assertThat(AppointmentStatus.CHECKED_IN.canTransitionTo(AppointmentStatus.COMPLETED)).isTrue();
        assertThat(AppointmentStatus.CHECKED_IN.canTransitionTo(AppointmentStatus.CANCELLED)).isTrue();
        assertThat(AppointmentStatus.CHECKED_IN.canTransitionTo(AppointmentStatus.NO_SHOW)).isFalse();
        assertThat(AppointmentStatus.CHECKED_IN.canTransitionTo(AppointmentStatus.BOOKED)).isFalse();

        for (AppointmentStatus terminal : new AppointmentStatus[] {
                AppointmentStatus.COMPLETED, AppointmentStatus.CANCELLED,
                AppointmentStatus.RESCHEDULED, AppointmentStatus.NO_SHOW}) {
            for (AppointmentStatus any : AppointmentStatus.values()) {
                assertThat(terminal.canTransitionTo(any))
                        .as("%s -> %s must be illegal", terminal, any).isFalse();
            }
        }
    }

    @Test
    void entity_transitionGuard_throwsIllegalTransition() {
        Appointment a = new Appointment();
        a.setId(7L);
        a.setApptStatus(AppointmentStatus.NO_SHOW.code());
        assertThatThrownBy(() -> a.transitionTo(AppointmentStatus.CHECKED_IN))
                .isInstanceOf(com.hims.appointment.application.exceptions.InvalidAppointmentRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", "APPT_ILLEGAL_TRANSITION");
    }

    @Test
    void slotConsuming_setMatchesLockedSemantics() {
        // cancelled + rescheduled free the slot; booked/checked_in/completed/no_show keep it.
        assertThat(AppointmentStatus.SLOT_CONSUMING).containsExactlyInAnyOrder(
                AppointmentStatus.BOOKED, AppointmentStatus.CHECKED_IN,
                AppointmentStatus.COMPLETED, AppointmentStatus.NO_SHOW);
    }
}
