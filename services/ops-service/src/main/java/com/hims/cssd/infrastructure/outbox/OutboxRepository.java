package com.hims.cssd.infrastructure.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Read-side access to the outbox for tests / ops queries ONLY. There is no relay
 * and no emitter in phase 1 (events REJECTED per KAFKA-EVENT-CATALOG); the
 * integration test uses count() to assert NO outbox row is ever produced.
 */
public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {
}
