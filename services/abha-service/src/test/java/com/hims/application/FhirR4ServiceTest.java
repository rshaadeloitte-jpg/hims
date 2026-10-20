package com.hims.application;

import com.hims.domain.model.FhirResource;
import com.hims.domain.repository.FhirResourceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FhirR4ServiceTest {
    @Mock
    private FhirResourceRepository repository;

    @InjectMocks
    private FhirR4Service service;

    @Test
    void createResource_returnsSavedEntity() {
        FhirResource entity = FhirResource.builder().resourceType("Patient").resourceId("pat-001").build();
        when(repository.save(any())).thenReturn(entity);

        FhirResource result = service.createResource(entity);

        assertNotNull(result);
        assertEquals("Patient", result.getResourceType());
        verify(repository, times(1)).save(entity);
    }
}