package com.hims.billing.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Transactional outbox publisher (separate bean so {@code @Transactional} applies from
 * the scheduled relay). CAS claim -> send catalog envelope -> mark PUBLISHED;
 * on failure attempts++ until maxAttempts then DLQ topic + DLQ row.
 * Envelope shape per KAFKA-EVENT-CATALOG §4 item 3.
 */
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxRepository repository;
    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;
    private final String dlqTopic;
    private final int maxAttempts;

    public OutboxPublisher(OutboxRepository repository,
                           KafkaTemplate<Object, Object> kafkaTemplate,
                           ObjectMapper objectMapper,
                           @Value("${app.kafka.topic.billing-events}") String topic,
                           @Value("${app.kafka.topic.dlq}") String dlqTopic,
                           @Value("${app.outbox.max-attempts:5}") int maxAttempts) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
        this.dlqTopic = dlqTopic;
        this.maxAttempts = maxAttempts;
    }

    @Transactional
    public void publish(OutboxEvent event) {
        int nextAttempts = event.getAttempts() + 1;
        if (repository.claim(event.getId(), nextAttempts) == 0) {
            return;
        }
        Map<String, Object> envelope = new LinkedHashMap<>();
        try {
            envelope.put("eventId", event.getEventId().toString());
            envelope.put("eventType", event.getEventType());
            envelope.put("version", event.getVersion());
            envelope.put("occurredAt", event.getOccurredAt().toString());
            envelope.put("producerService", event.getProducerService());
            envelope.put("entityId", event.getAggregateId());
            envelope.put("partitionKey", event.getPartitionKey());
            envelope.put("idempotencyKey", event.getIdempotencyKey());
            envelope.put("schemaName", schemaOf(event.getEventType()));
            envelope.put("payload", objectMapper.readTree(event.getPayload()));
            kafkaTemplate.send(topic, event.getPartitionKey(), envelope)
                    .get(10, TimeUnit.SECONDS);
            repository.markPublished(event.getId(), Timestamp.from(Instant.now()));
        } catch (Exception e) {
            log.error("Outbox publish failed for event id={} type={}", event.getId(), event.getEventType(), e);
            if (nextAttempts >= maxAttempts) {
                publishToDlq(event, envelope, e);
                repository.markDlq(event.getId(), nextAttempts);
            } else {
                repository.markFailed(event.getId(), nextAttempts);
            }
        }
    }

    /** Catalog schema names per event type. */
    static String schemaOf(String eventType) {
        return switch (eventType) {
            case BillingEvents.INVOICE_GENERATED -> "invoice-generated.v1";
            case BillingEvents.PAYMENT_RECEIVED -> "payment-received.v1";
            case BillingEvents.BILL_SETTLED -> "bill-settled.v1";
            case BillingEvents.CONCESSION_APPROVED -> "concession-approved.v1";
            case BillingEvents.SPONSOR_CHANGED -> "sponsor-changed.v1";
            case BillingEvents.PACKAGE_SUBSCRIBED -> "package-subscribed.v1";
            default -> "unknown.v1";
        };
    }

    /** Exhausted events go to the DLQ topic so no event is silently lost. */
    private void publishToDlq(OutboxEvent event, Map<String, Object> envelope, Exception cause) {
        Map<String, Object> dlqEnvelope = new LinkedHashMap<>(envelope);
        dlqEnvelope.put("dlqReason", cause.getClass().getSimpleName() + ": " + String.valueOf(cause.getMessage()));
        dlqEnvelope.put("dlqAfterAttempts", event.getAttempts() + 1);
        try {
            kafkaTemplate.send(dlqTopic, event.getPartitionKey(), dlqEnvelope).get(10, TimeUnit.SECONDS);
            log.warn("Outbox event id={} type={} routed to DLQ topic {}", event.getId(), event.getEventType(), dlqTopic);
        } catch (Exception dlqError) {
            log.error("DLQ topic publish failed for event id={} (row is DLQ-status, replay job will retry)", event.getId(), dlqError);
        }
    }

    /** Exact event TYPE strings per KAFKA-EVENT-CATALOG. */
    public static final class BillingEvents {
        public static final String INVOICE_GENERATED = "InvoiceGenerated";   // E-07
        public static final String PAYMENT_RECEIVED = "PaymentReceived";      // E-08
        public static final String BILL_SETTLED = "BillSettled";             // BR-BILL-006 settlement complete
        public static final String CONCESSION_APPROVED = "ConcessionApproved"; // concession applied to interim bill
        public static final String SPONSOR_CHANGED = "SponsorChanged";       // M04 sponsor change stub
        public static final String PACKAGE_SUBSCRIBED = "PackageSubscribed"; // M04 package subscription

        private BillingEvents() {}
    }
}
