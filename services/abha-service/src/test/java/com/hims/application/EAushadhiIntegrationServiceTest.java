package com.hims.application;

import com.hims.domain.model.EAushadhiDrug;
import com.hims.domain.repository.EAushadhiDrugRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EAushadhiIntegrationServiceTest {
    @Mock
    private EAushadhiDrugRepository repository;

    @InjectMocks
    private EAushadhiIntegrationService service;

    @Test
    void addDrug_returnsSavedEntity() {
        EAushadhiDrug entity = EAushadhiDrug.builder().drugCode("DRG001").drugName("Paracetamol").build();
        when(repository.save(any())).thenReturn(entity);

        EAushadhiDrug result = service.addDrug(entity);

        assertNotNull(result);
        assertEquals("DRG001", result.getDrugCode());
        verify(repository, times(1)).save(entity);
    }
}