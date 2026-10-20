package com.hims.reporting.domain.repository;

import com.hims.reporting.domain.model.IngestedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Ingest-idempotency ledger access. existsByEventTypeAndEventKey is THE strict
 * double-ingest guard for accumulating read models (pinned by tests).
 */
public interface IngestedEventRepository extends JpaRepository<IngestedEvent, Long> {

    boolean existsByEventTypeAndEventKey(String eventType, String eventKey);
}
