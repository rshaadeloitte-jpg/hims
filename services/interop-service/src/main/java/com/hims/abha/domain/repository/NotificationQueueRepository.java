package com.hims.abha.domain.repository;

import com.hims.abha.domain.model.NotificationQueue;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationQueueRepository extends JpaRepository<NotificationQueue, Long> {
}