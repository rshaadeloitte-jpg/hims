package com.hims.bcma.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BCMAExceptionTest {

    @Test
    void exceptionFields() {
        BCMAException exc = new BCMAException();
        exc.setExceptionId(1L); exc.setPatientId(10L); exc.setExceptionType("OVERRIDE");
        exc.setReason("Clinical judgment"); exc.setOverridden(false);
        assertThat(exc.getExceptionType()).isEqualTo("OVERRIDE");
        assertThat(exc.getOverridden()).isFalse();
    }
}
