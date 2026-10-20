package com.hims.application;

import com.hims.domain.model.WhatsAppNotification;
import com.hims.domain.repository.WhatsAppNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppNotificationService {
    private final WhatsAppNotificationRepository repository;

    @Transactional(readOnly = true)
    public WhatsAppNotification findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("WhatsAppNotification not found: " + id));
    }

    public WhatsAppNotification sendNotification(WhatsAppNotification notification) {
        log.info("Sending WhatsApp notification to: {}", notification.getMobile());
        return repository.save(notification);
    }

    public Iterable<WhatsAppNotification> findAll() {
        return repository.findAll();
    }
}