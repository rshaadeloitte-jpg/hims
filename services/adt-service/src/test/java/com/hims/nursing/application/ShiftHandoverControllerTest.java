package com.hims.nursing.application;

import com.hims.nursing.infrastructure.web.ShiftHandoverController;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ShiftHandoverControllerTest {
    @Test
    void controllerWired() {
        assertThat(new ShiftHandoverController(null)).isNotNull();
    }
}
