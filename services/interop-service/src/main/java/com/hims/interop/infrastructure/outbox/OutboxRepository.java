package com.hims.interop.infrastructure.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Read-side access to the outbox for tests / ops queries ONLY. There is no relay
 * and NO emitter in phase 1 — the interop service emits NOTHING (register WP-T1);
 * the integration test uses count() to assert NO outbox row is ever produced.
 */
public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {
}
