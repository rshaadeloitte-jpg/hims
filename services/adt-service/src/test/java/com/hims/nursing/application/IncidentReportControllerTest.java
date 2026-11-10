package com.hims.nursing.application;

import com.hims.nursing.infrastructure.web.IncidentReportController;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IncidentReportControllerTest {
    @Test
    void controllerWired() {
        assertThat(new IncidentReportController(null)).isNotNull();
    }
}
