package com.hims.application;

import com.hims.domain.model.WhatsAppNotification;
import com.hims.domain.repository.WhatsAppNotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WhatsAppNotificationServiceTest {
    @Mock
    private WhatsAppNotificationRepository repository;

    @InjectMocks
    private WhatsAppNotificationService service;

    @Test
    void sendNotification_returnsSavedEntity() {
        WhatsAppNotification entity = WhatsAppNotification.builder()
            .mobile("+919876543210")
            .templateId("otp_01")
            .build();
        when(repository.save(any())).thenReturn(entity);

        WhatsAppNotification result = service.sendNotification(entity);

        assertNotNull(result);
        assertEquals("+919876543210", result.getMobile());
        verify(repository, times(1)).save(entity);
    }
}