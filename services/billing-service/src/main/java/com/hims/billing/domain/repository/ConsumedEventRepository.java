package com.hims.billing.domain.repository;

import com.hims.billing.domain.model.ConsumedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConsumedEventRepository extends JpaRepository<ConsumedEvent, Long> {

    Optional<ConsumedEvent> findByEventId(String eventId);
}
