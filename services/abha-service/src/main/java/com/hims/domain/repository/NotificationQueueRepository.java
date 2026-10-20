package com.hims.domain.repository;

import com.hims.domain.model.NotificationQueue;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationQueueRepository extends JpaRepository<NotificationQueue, Long> {
}