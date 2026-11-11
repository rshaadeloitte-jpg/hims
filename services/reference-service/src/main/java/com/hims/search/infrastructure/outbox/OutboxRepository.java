package com.hims.search.infrastructure.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Read-side access to the outbox for tests / ops queries ONLY. There is no relay
 * and no emitter in this service (search EMITS NOTHING per register WP-T3); the
 * integration test uses count() to assert NO outbox row is ever produced.
 */
public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {
}
