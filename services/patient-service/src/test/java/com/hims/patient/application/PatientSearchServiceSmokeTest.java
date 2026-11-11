package com.hims.patient.application;

import com.hims.patient.domain.model.PatientRegistration;
import com.hims.patient.domain.repository.PatientRegistrationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientSearchServiceSmokeTest {
    @Mock PatientRegistrationRepository repository;

    @Test
    void serviceUsesMockedSearch() {
        PatientRegistrationService service = new PatientRegistrationService(repository, null, null, null, null, null, null, null, new com.fasterxml.jackson.databind.ObjectMapper());
        // reflection will try Pageable variant first
        when(repository.search(eq("TESTCLAUDE"), eq("ALL"), any(org.springframework.data.domain.Pageable.class)))
            .thenReturn(List.of(new PatientRegistration() {{ setId(1L); setFirstName("TESTCLAUDE"); setStatus(true); }}));
        assertThat(service.search("TESTCLAUDE", "ALL")).hasSize(1);
    }

    @Test
    void serviceUsesMockedSearchNoPageable() {
        PatientRegistrationService service = new PatientRegistrationService(repository, null, null, null, null, null, null, null, new com.fasterxml.jackson.databind.ObjectMapper());
        when(repository.search(eq("MOBILE"), eq("MOBILE"))).thenReturn(List.of(new PatientRegistration() {{ setId(2L); setMobileNo("9000000001"); setStatus(true); }}));
        assertThat(service.search("MOBILE", "MOBILE")).hasSize(1);
    }
}