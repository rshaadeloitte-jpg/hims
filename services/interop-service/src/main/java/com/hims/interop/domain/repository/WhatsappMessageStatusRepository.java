package com.hims.interop.domain.repository;

import com.hims.interop.domain.model.WhatsappMessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WhatsappMessageStatusRepository extends JpaRepository<WhatsappMessageStatus, Long> {

    List<WhatsappMessageStatus> findByMessageIdOrderByTsAsc(Long messageId);
}
