package com.hims.interop.domain.repository;

import com.hims.interop.domain.model.SmsNotificationQueue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SmsNotificationQueueRepository extends JpaRepository<SmsNotificationQueue, Long> {

    List<SmsNotificationQueue> findByStatusOrderByIdDesc(Short status);

    List<SmsNotificationQueue> findAllByOrderByIdDesc();
}
