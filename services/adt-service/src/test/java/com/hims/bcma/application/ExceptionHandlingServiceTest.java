package com.hims.bcma.application;

import com.hims.bcma.domain.model.BCMAException;
import com.hims.bcma.domain.model.OverrideReason;
import com.hims.bcma.domain.repository.BCMAExceptionRepository;
import com.hims.bcma.domain.repository.OverrideReasonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExceptionHandlingServiceTest {

    private ExceptionHandlingService service;
    private BCMAExceptionRepository excRepo;
    private OverrideReasonRepository reasonRepo;

    @BeforeEach
    void setUp() {
        excRepo = mock(BCMAExceptionRepository.class);
        reasonRepo = mock(OverrideReasonRepository.class);
        service = new ExceptionHandlingService(excRepo, reasonRepo);
        when(excRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(reasonRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createExceptionSuccess() {
        BCMAException e = service.createException(1L, "NDC-1", "OVERRIDE", "Clinical need", 10L, 2L, 20L);
        assertThat(e.getExceptionType()).isEqualTo("OVERRIDE");
        assertThat(e.getOverridden()).isFalse();
    }

    @Test
    void applyOverrideSetsOverridden() {
        BCMAException e = new BCMAException(); e.setExceptionId(1L); e.setOverridden(false);
        when(excRepo.findByExceptionId(1L)).thenReturn(Optional.of(e));
        BCMAException updated = service.applyOverride(1L, "Justified", 10L);
        assertThat(updated.getOverridden()).isTrue();
    }

    @Test
    void addReasonCreatesRecord() {
        OverrideReason r = service.addReason("OVR-01", "Patient allergy", "CLINICAL");
        assertThat(r.getCode()).isEqualTo("OVR-01");
        assertThat(r.getCode()).isEqualTo("OVR-01");
    }

    @Test
    void findByPatientReturnsEmpty() {
        when(excRepo.findByPatientId(99L)).thenReturn(Collections.emptyList());
        assertThat(service.findByPatient(99L)).isEmpty();
    }
}
