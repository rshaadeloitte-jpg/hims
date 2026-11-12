package com.hims.billing.infrastructure.eventbus;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.billing.application.BillingSagaIngressService;
import com.hims.billing.application.exceptions.InvalidBillingRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * PostgreSQL event-bus consumer for order events -> billing saga ingress
 * (E-13 OrderCreated). Replaces the Kafka listener: consumer group
 * billing-saga subscribes to ihms.ordering.events in ihms_eventbus.subscription
 * and only sees events published after that (former auto.offset.reset=latest).
 *
 * Each tick claims due delivery rows in one statement (FOR UPDATE SKIP LOCKED,
 * PENDING -> PROCESSING), so several billing instances split the queue. A row
 * stuck in PROCESSING past the lock timeout (instance died mid-batch) is
 * reclaimed. Retryable failures go back to PENDING with exponential backoff;
 * after max-attempts, or on InvalidBillingRequestException (not retryable, as
 * before), the row is FAILED — the dead letter. Duplicate deliveries are safe:
 * the ingress is idempotent per eventId via t_consumed_event.
 */
@Component
@ConditionalOnProperty(name = "app.eventbus.consumer.enabled", havingValue = "true", matchIfMissing = true)
public class BillingSagaEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(BillingSagaEventConsumer.class);

    /** Event type ordering-service actually publishes (SagaService.emitOrderInitiated). */
    public static final String EV_ORDER_INITIATED = "OrderInitiated";
    public static final String CONSUMER_GROUP = "billing-saga";

    private static final String CLAIM_SQL = """
            UPDATE ihms_eventbus.delivery d
            SET status = 'PROCESSING', locked_at = NOW()
            FROM ihms_eventbus.event_log e
            WHERE e.id = d.event_log_id
              AND d.id IN (SELECT id FROM ihms_eventbus.delivery
                           WHERE consumer_group = ?
                             AND ((status = 'PENDING' AND next_attempt_at <= NOW())
                               OR (status = 'PROCESSING' AND locked_at < NOW() - make_interval(secs => ?)))
                           ORDER BY id
                           LIMIT ?
                           FOR UPDATE SKIP LOCKED)
            RETURNING d.id, d.retry_count, e.envelope::text AS envelope
            """;

    private final BillingSagaIngressService ingress;
    private final ObjectMapper mapper;
    private final JdbcTemplate jdbc;
    private final String topic;
    private final int batchSize;
    private final int maxAttempts;
    private final int lockTimeoutSeconds;
    private volatile boolean subscribed;

    public BillingSagaEventConsumer(BillingSagaIngressService ingress,
                                    ObjectMapper mapper,
                                    JdbcTemplate jdbc,
                                    @Value("${app.eventbus.topic.ordering-events:ihms.ordering.events}") String topic,
                                    @Value("${app.eventbus.consumer.batch-size:50}") int batchSize,
                                    @Value("${app.eventbus.consumer.max-attempts:5}") int maxAttempts,
                                    @Value("${app.eventbus.consumer.lock-timeout-seconds:300}") int lockTimeoutSeconds) {
        this.ingress = ingress;
        this.mapper = mapper;
        this.jdbc = jdbc;
        this.topic = topic;
        this.batchSize = batchSize;
        this.maxAttempts = maxAttempts;
        this.lockTimeoutSeconds = lockTimeoutSeconds;
    }

    private record Claimed(long id, int retryCount, String envelope) {}

    /** Idempotent subscription upsert; a failure is retried on the next poll. */
    @EventListener(ApplicationReadyEvent.class)
    public void subscribe() {
        try {
            jdbc.update("""
                    INSERT INTO ihms_eventbus.subscription (consumer_group, topic) VALUES (?, ?)
                    ON CONFLICT (consumer_group, topic) DO NOTHING
                    """, CONSUMER_GROUP, topic);
            subscribed = true;
        } catch (Exception e) {
            log.warn("Event-bus subscription {} -> {} not registered yet: {}", CONSUMER_GROUP, topic, e.getMessage());
        }
    }


    @Scheduled(fixedDelayString = "${app.eventbus.consumer.poll-interval-ms:1000}")
    public void poll() {
        if (!subscribed) {
            subscribe();
            if (!subscribed) {
                return;
            }
        }
        List<Claimed> batch = jdbc.query(CLAIM_SQL,
                (rs, i) -> new Claimed(rs.getLong("id"), rs.getInt("retry_count"), rs.getString("envelope")),
                CONSUMER_GROUP, lockTimeoutSeconds, batchSize);
        batch.sort(Comparator.comparingLong(Claimed::id));
        for (Claimed c : batch) {
            try {
                handle(c.envelope());
                jdbc.update("UPDATE ihms_eventbus.delivery SET status = 'DONE', delivered_at = NOW(), "
                        + "locked_at = NULL WHERE id = ?", c.id());
            } catch (InvalidBillingRequestException e) {
                log.error("Event-bus consume rejected (not retryable): delivery={}", c.id(), e);
                markFailed(c, e);
            } catch (Exception e) {
                int attempts = c.retryCount() + 1;
                if (attempts >= maxAttempts) {
                    log.error("Event-bus consume failed after {} attempts: delivery={}", attempts, c.id(), e);
                    markFailed(c, e);
                } else {
                    log.warn("Event-bus consume failed (attempt {}), will retry: delivery={}", attempts, c.id(), e);
                    long delaySeconds = Math.min(300L, 2L << Math.min(attempts, 7));
                    jdbc.update("UPDATE ihms_eventbus.delivery SET status = 'PENDING', retry_count = ?, "
                                    + "next_attempt_at = NOW() + make_interval(secs => ?), locked_at = NULL, "
                                    + "last_error = ? WHERE id = ?",
                            attempts, delaySeconds, String.valueOf(e), c.id());
                }
            }
        }
    }

    private void markFailed(Claimed c, Exception e) {
        jdbc.update("UPDATE ihms_eventbus.delivery SET status = 'FAILED', retry_count = ?, locked_at = NULL, "
                + "last_error = ? WHERE id = ?", c.retryCount() + 1, String.valueOf(e), c.id());
    }

    /**
     * Envelope -> ingress adapter. Unusable messages (blank, malformed, missing
     * eventId/payload, other event types) are skipped; ingress exceptions propagate
     * so the delivery is retried or dead-lettered.
     */
    public void handle(String value) {
        if (value == null || value.isBlank()) {
            log.warn("Received blank event-bus message; skipping");
            return;
        }

        Map<String, Object> envelope;
        try {
            envelope = mapper.readValue(value, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Malformed event-bus message; skipping", e);
            return;
        }
        if (envelope == null) {
            log.warn("Received null event envelope; skipping");
            return;
        }

        String eventId = stringOf(envelope.get("eventId"));
        if (eventId == null || eventId.isBlank()) {
            log.warn("Missing eventId in event-bus message; skipping");
            return;
        }

        String eventType = stringOf(envelope.get("eventType"));

        Object payloadObj = envelope.get("payload");
        if (!(payloadObj instanceof Map<?, ?>)) {
            log.warn("Missing or non-object payload (eventId={}); skipping", eventId);
            return;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) payloadObj;

        if (!EV_ORDER_INITIATED.equals(eventType)) {
            log.debug("Skipping non-OrderInitiated event type={} (eventId={})", eventType, eventId);
            return;
        }

        // Adapt ordering's contract (items) to billing's (lines) without mutating
        // the original payload: shallow-copy the map and alias items -> lines.
        Map<String, Object> adapted = new LinkedHashMap<>(payload);
        adapted.put("lines", payload.get("items"));

        ingress.ingest(eventId, BillingSagaIngressService.EV_ORDER_CREATED, adapted);
    }

    private static String stringOf(Object v) {
        return v == null ? null : String.valueOf(v);
    }
}
