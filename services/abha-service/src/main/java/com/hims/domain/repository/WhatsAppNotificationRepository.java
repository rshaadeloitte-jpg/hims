package com.hims.domain.repository;

import com.hims.domain.model.WhatsAppNotification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WhatsAppNotificationRepository extends JpaRepository<WhatsAppNotification, Long> {
}