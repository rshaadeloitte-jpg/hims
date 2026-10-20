package com.hims.reference.infrastructure.outbox;

import com.hims.reference.domain.model.OutboxEvent;
import com.hims.reference.domain.repository.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Outbox relay scheduler: polls due rows (PENDING/FAILED below attempt cap) and
 * delegates to {@link OutboxPublisher} (separate bean so @Transactional applies —
 * never call the publisher via {@code this}).
 */
@Component
public class KafkaOutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(KafkaOutboxRelay.class);

    private final OutboxRepository repository;
    private final OutboxPublisher publisher;
    private final int maxAttempts;
    private final int batchSize;

    public KafkaOutboxRelay(OutboxRepository repository,
                            OutboxPublisher publisher,
                            @Value("${app.outbox.max-attempts:3}") int maxAttempts) {
        this.repository = repository;
        this.publisher = publisher;
        this.maxAttempts = maxAttempts;
        this.batchSize = 100;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:5000}")
    public void pollAndPublish() {
        List<OutboxEvent> events = repository.findDue(maxAttempts, PageRequest.of(0, batchSize));
        if (events.isEmpty()) {
            return;
        }
        log.info("Outbox relay dispatching {} due events", events.size());
        for (OutboxEvent event : events) {
            publisher.publish(event);
        }
    }
}
