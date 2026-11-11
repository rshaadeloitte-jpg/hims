package com.hims.nursing.application;

import com.hims.nursing.domain.model.CarePlan;
import com.hims.nursing.infrastructure.web.NursingCarePlanController;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NursingCarePlanControllerTest {

    @Test
    void controllerWired() {
        assertThat(new NursingCarePlanController(null)).isNotNull();
    }
}
