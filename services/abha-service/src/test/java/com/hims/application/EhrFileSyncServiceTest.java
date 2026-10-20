package com.hims.application;

import com.hims.domain.model.EhrFile;
import com.hims.domain.repository.EhrFileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EhrFileSyncServiceTest {
    @Mock
    private EhrFileRepository repository;

    @InjectMocks
    private EhrFileSyncService service;

    @Test
    void syncFile_returnsSavedEntity() {
        EhrFile entity = EhrFile.builder().abhaId("ABHA123").documentType("LabReport").build();
        when(repository.save(any())).thenReturn(entity);

        EhrFile result = service.syncFile(entity);

        assertNotNull(result);
        assertEquals("ABHA123", result.getAbhaId());
        verify(repository, times(1)).save(entity);
    }
}