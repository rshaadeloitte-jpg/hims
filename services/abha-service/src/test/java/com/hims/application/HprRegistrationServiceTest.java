package com.hims.application;

import com.hims.domain.model.HprRegistration;
import com.hims.domain.repository.HprRegistrationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HprRegistrationServiceTest {
    @Mock
    private HprRegistrationRepository repository;

    @InjectMocks
    private HprRegistrationService service;

    @Test
    void createRegistration_returnsSavedEntity() {
        HprRegistration entity = new HprRegistration();
        entity.setAbhaId("ABHA123");
        HprRegistration saved = new HprRegistration();
        saved.setId(1L);
        saved.setAbhaId("ABHA123");
        when(repository.save(any())).thenReturn(saved);

        HprRegistration result = service.createRegistration(entity);

        assertNotNull(result.getId());
        assertEquals("ABHA123", result.getAbhaId());
        verify(repository, times(1)).save(entity);
    }

    @Test
    void findById_throws_whenNotFound() {
        when(repository.findById(999L)).thenReturn(java.util.Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.findById(999L));
    }
}
