package com.hims.pharmacy.infrastructure.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Read-mostly access to the outbox table. No writer exists in this service:
 * the repository exists so tests can assert the register-mandated NEGATIVE
 * (zero rows after every flow) and so a future catalog-approved event type has
 * its infrastructure ready without schema churn.
 */
public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {
}
