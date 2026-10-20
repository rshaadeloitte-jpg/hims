package com.hims.interop.domain.repository;

import com.hims.interop.domain.model.SmsNotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SmsNotificationStatusRepository extends JpaRepository<SmsNotificationStatus, Long> {

    List<SmsNotificationStatus> findByMessageIdOrderByTsAsc(Long messageId);
}
