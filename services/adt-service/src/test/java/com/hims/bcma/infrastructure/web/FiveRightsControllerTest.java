package com.hims.bcma.infrastructure.web;

import com.hims.bcma.application.FiveRightsVerificationService;
import com.hims.bcma.domain.model.BcmaVerification;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FiveRightsControllerTest {

    @Test
    void controllerExists() {
        FiveRightsVerificationService svc = mock(FiveRightsVerificationService.class);
        FiveRightsController ctrl = new FiveRightsController(svc);
        when(svc.verifyRightPatient(1L, 10L, "W", true)).thenReturn(new BcmaVerification());
        ResponseEntity<BcmaVerification> resp = ctrl.verifyPatient(1L, 10L, "W", true);
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
    }
}
