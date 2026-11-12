package com.hims.billing.infrastructure.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Outbox relay scheduler (PostgreSQL transport): each tick hands one
 * claim-and-dispatch batch to {@link OutboxPublisher} (separate bean so
 * @Transactional applies), which locks due rows FOR UPDATE SKIP LOCKED and
 * appends them to ihms_eventbus.event_log in the same transaction.
 */
@Component
public class PostgresOutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(PostgresOutboxRelay.class);

    private final OutboxPublisher publisher;
    private final int batchSize;

    public PostgresOutboxRelay(OutboxPublisher publisher,
                               @Value("${app.outbox.batch-size:100}") int batchSize) {
        this.publisher = publisher;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:5000}")
    public void pollAndPublish() {
        int dispatched = publisher.publishDue(batchSize);
        if (dispatched > 0) {
            log.info("Outbox relay dispatched {} due events", dispatched);
        }
    }
}
