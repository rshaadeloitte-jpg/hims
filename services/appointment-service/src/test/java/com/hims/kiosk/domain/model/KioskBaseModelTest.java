package com.hims.kiosk.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KioskBaseModelTest {
    @Test
    void kioskRegistrationFieldsWork() {
        KioskRegistration r = new KioskRegistration();
        r.setId(1L); r.setPatientName("Alice"); r.setContactPhone("9876543210");
        r.setStatus(true); r.setRegistrationToken("T01");
        assertThat(r.getId()).isEqualTo(1L);
        assertThat(r.getStatus()).isTrue();
        assertThat(r.getRegistrationToken()).isEqualTo("T01");
    }

    @Test
    void kioskAppointmentStatusDefaults() {
        KioskAppointment a = new KioskAppointment();
        assertThat(a.getStatusCode()).isEqualTo((short) 1);
        assertThat(a.getVisitType()).isEqualTo("SELF_APPOINTMENT");
    }

    @Test
    void kioskQueueDefaults() {
        KioskQueue q = new KioskQueue();
        assertThat(q.getQueueStatus()).isEqualTo((short) 1);
        assertThat(q.getPriority()).isEqualTo((short) 1);
    }

    @Test
    void kioskLabResultDefaults() {
        KioskLabResult lr = new KioskLabResult();
        assertThat(lr.getVerified()).isFalse();
    }
}
