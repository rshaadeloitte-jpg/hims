package com.hims.bcma.application;

import com.hims.bcma.domain.model.IvpAdministration;
import com.hims.bcma.domain.model.IvpDrugConfig;
import com.hims.bcma.domain.repository.IvpAdministrationRepository;
import com.hims.bcma.domain.repository.IvpDrugConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IvpAdministrationServiceTest {

    private IvpAdministrationService service;
    private IvpAdministrationRepository ivpRepo;
    private IvpDrugConfigRepository configRepo;

    @BeforeEach
    void setUp() {
        ivpRepo = mock(IvpAdministrationRepository.class);
        configRepo = mock(IvpDrugConfigRepository.class);
        service = new IvpAdministrationService(ivpRepo, configRepo);

        when(ivpRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(configRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void recordIVPRequiresPatientAndDrug() {
        IvpAdministration ivp = service.recordIVP(1L, "Dopamine", BigDecimal.valueOf(10), "mg", BigDecimal.valueOf(5), true, BigDecimal.valueOf(10), 101L, 1L);
        assertThat(ivp.getPatientId()).isEqualTo(1L);
        assertThat(ivp.getAdministrationStatus()).isEqualTo("IN_PROGRESS");
    }

    @Test
    void verifyDilutionThrowsIfRequiredNotVerified() {
        IvpDrugConfig cfg = new IvpDrugConfig();
        cfg.setRequiresDilution(true); cfg.setDrugNdc("DOPA");
        when(configRepo.findByDrugNdc("DOPA")).thenReturn(Optional.of(cfg));
        IvpAdministration ivp = new IvpAdministration(); ivp.setIvpId(1L); ivp.setDrugName("DOPA");
        when(ivpRepo.findByIvpId(1L)).thenReturn(Optional.of(ivp));
        assertThatThrownBy(() -> service.verifyDilution(1L, false)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void completeIVPSuccess() {
        IvpAdministration ivp = new IvpAdministration(); ivp.setIvpId(2L); ivp.setAdministrationStatus("IN_PROGRESS");
        when(ivpRepo.findByIvpId(2L)).thenReturn(Optional.of(ivp));
        IvpAdministration done = service.completeIVP(2L, 99L);
        assertThat(done.getAdministrationStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void findByPatientReturnsEmpty() {
        when(ivpRepo.findByPatientId(5L)).thenReturn(Collections.emptyList());
        assertThat(service.findByPatient(5L)).isEmpty();
    }
}
