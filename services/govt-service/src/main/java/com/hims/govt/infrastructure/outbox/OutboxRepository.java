package com.hims.govt.infrastructure.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Read-only access to the outbox for the NEGATIVE OUTBOX assertion (register WP-T4):
 * no writer exists anywhere in this service — the repository is used by tests to
 * prove the table stays EMPTY through every flow (cssd-service convention).
 */
public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {
}
