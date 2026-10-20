package com.hims.interop.domain.repository;

import com.hims.interop.domain.model.WhatsappMessageQueue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WhatsappMessageQueueRepository extends JpaRepository<WhatsappMessageQueue, Long> {

    List<WhatsappMessageQueue> findByStatusOrderByIdDesc(Short status);

    List<WhatsappMessageQueue> findAllByOrderByIdDesc();
}
