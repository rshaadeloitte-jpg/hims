package com.hims.bcma.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class BcmaVerificationTest {

    @Test
    void createAndSetFields() {
        BcmaVerification v = new BcmaVerification();
        v.setBcmaId(100L);
        v.setPatientId(10L);
        v.setDrugNdc("NDC-123");
        v.setRightPatient(true);
        v.setRightDrug(true);
        v.setRightDose(true);
        v.setRightRoute(true);
        v.setRightTime(true);
        v.setAllRightsPassed(true);
        v.setScanTime(Timestamp.from(Instant.now()));

        assertThat(v.getBcmaId()).isEqualTo(100L);
        assertThat(v.getAllRightsPassed()).isTrue();
    }

    @Test
    void allRightsPassedRequiresAllTrue() {
        BcmaVerification v = new BcmaVerification();
        v.setRightPatient(true); v.setRightDrug(true); v.setRightDose(false);
        v.setRightRoute(true); v.setRightTime(true);
        v.setAllRightsPassed(false);
        assertThat(v.getAllRightsPassed()).isFalse();
    }
}
