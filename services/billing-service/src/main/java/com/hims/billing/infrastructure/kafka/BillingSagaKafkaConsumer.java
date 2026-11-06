package com.hims.billing.infrastructure.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.billing.application.BillingSagaIngressService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Kafka consumer for order events -> billing saga ingress (E-13 OrderCreated).
 * Group id billing-saga; auto-offset-reset latest. Value deserializer is String.
 */
@Component
public class BillingSagaKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(BillingSagaKafkaConsumer.class);

    /** Event type ordering-service actually publishes (SagaService.emitOrderInitiated). */
    public static final String EV_ORDER_INITIATED = "OrderInitiated";

    private final BillingSagaIngressService ingress;
    private final ObjectMapper mapper;

    public BillingSagaKafkaConsumer(BillingSagaIngressService ingress, ObjectMapper mapper) {
        this.ingress = ingress;
        this.mapper = mapper;
    }

    @KafkaListener(topics = "${app.kafka.topic.ordering-events:ihms.ordering.events}",
            groupId = "billing-saga",
            properties = {"auto.offset.reset=latest"})
    public void listen(ConsumerRecord<String, String> record) {
        String value = (record == null) ? null : record.value();
        if (value == null || value.isBlank()) {
            log.warn("Received blank Kafka message; skipping");
            return;
        }

        Map<String, Object> envelope;
        try {
            envelope = mapper.readValue(value, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Malformed Kafka message; skipping", e);
            return;
        }
        if (envelope == null) {
            log.warn("Received null event envelope; skipping");
            return;
        }

        String eventId = stringOf(envelope.get("eventId"));
        if (eventId == null || eventId.isBlank()) {
            log.warn("Missing eventId in Kafka message; skipping");
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
