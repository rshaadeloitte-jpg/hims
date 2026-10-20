package com.hims.bcma.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class IvpAdministrationTest {

    @Test
    void ivpFields() {
        IvpAdministration ivp = new IvpAdministration();
        ivp.setIvpId(1L); ivp.setDrugName("Dopamine");
        ivp.setDoseAmount(BigDecimal.valueOf(5)); ivp.setDoseUnit("mg");
        ivp.setRateMlPerMin(BigDecimal.valueOf(2)); ivp.setDilutionRequired(true);
        ivp.setAdministrationStatus("PENDING");
        assertThat(ivp.getDrugName()).isEqualTo("Dopamine");
        assertThat(ivp.getAdministrationStatus()).isEqualTo("PENDING");
    }
}
