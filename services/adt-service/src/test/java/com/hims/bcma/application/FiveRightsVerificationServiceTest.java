package com.hims.bcma.application;

import com.hims.bcma.domain.model.BcmaVerification;
import com.hims.bcma.domain.model.VerificationLog;
import com.hims.bcma.domain.repository.BcmaVerificationRepository;
import com.hims.bcma.domain.repository.VerificationLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FiveRightsVerificationServiceTest {

    private FiveRightsVerificationService service;
    private BcmaVerificationRepository bcmaRepo;
    private VerificationLogRepository logRepo;

    @BeforeEach
    void setUp() {
        bcmaRepo = mock(BcmaVerificationRepository.class);
        logRepo = mock(VerificationLogRepository.class);
        service = new FiveRightsVerificationService(bcmaRepo, logRepo);

        jakarta.persistence.EntityManager em = mock(jakarta.persistence.EntityManager.class);
        jakarta.persistence.Query seq = mock(jakarta.persistence.Query.class);
        java.util.concurrent.atomic.AtomicLong seqVal = new java.util.concurrent.atomic.AtomicLong(0);
        when(seq.getSingleResult()).thenAnswer(inv -> seqVal.incrementAndGet());
        when(em.createNativeQuery(org.mockito.ArgumentMatchers.contains("nextval"))).thenReturn(seq);
        ReflectionTestUtils.setField(service, "em", em);

        when(bcmaRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(logRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void verifyRightPatientRequiresPatientId() {
        assertThatThrownBy(() -> service.verifyRightPatient(1L, null, "W123", true))
                .isInstanceOf(Exception.class);
    }

    @Test
    void verifyRightPatientSuccess() {
        BcmaVerification v = service.verifyRightPatient(1L, 100L, "BARCODE-001", true);
        assertThat(v.getRightPatient()).isTrue();
        assertThat(v.getWristbandBarcode()).isEqualTo("BARCODE-001");
    }

    @Test
    void verifyRightDrugCreatesRecord() {
        BcmaVerification v = service.verifyRightDrug(2L, "NDC-123", true);
        assertThat(v.getRightDrug()).isTrue();
        assertThat(v.getDrugNdc()).isEqualTo("NDC-123");
    }

    @Test
    void verifyRightDosePass() {
        BcmaVerification v = service.verifyRightDose(3L, true);
        assertThat(v.getRightDose()).isTrue();
    }

    @Test
    void verifyRightRoutePass() {
        BcmaVerification v = service.verifyRightRoute(4L, true);
        assertThat(v.getRightRoute()).isTrue();
    }

    @Test
    void verifyRightTimePass() {
        BcmaVerification v = service.verifyRightTime(5L, true);
        assertThat(v.getRightTime()).isTrue();
    }

    @Test
    void finalizeSetsAllPassedWhenAllTrue() {
        BcmaVerification base = new BcmaVerification();
        base.setBcmaId(10L); base.setPatientId(1L); base.setRightPatient(true); base.setRightDrug(true);
        base.setRightDose(true); base.setRightRoute(true); base.setRightTime(true);
        when(bcmaRepo.findByBcmaId(10L)).thenReturn(Optional.of(base));
        BcmaVerification finalV = service.finalizeVerification(10L);
        assertThat(finalV.getAllRightsPassed()).isTrue();
        assertThat(finalV.getAdministered()).isTrue();
    }

    @Test
    void findByPatientReturnsList() {
        when(bcmaRepo.findByPatientId(99L)).thenReturn(Collections.emptyList());
        assertThat(service.findByPatient(99L)).isEmpty();
    }
}
