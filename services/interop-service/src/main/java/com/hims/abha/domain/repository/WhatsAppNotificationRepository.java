package com.hims.abha.domain.repository;

import com.hims.abha.domain.model.WhatsAppNotification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WhatsAppNotificationRepository extends JpaRepository<WhatsAppNotification, Long> {
}