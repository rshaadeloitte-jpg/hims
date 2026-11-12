package com.hims.ordering.infrastructure.eventbus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * PostgreSQL-native event bus producer (replaces KafkaTemplate). Appends one
 * envelope to ihms_eventbus.event_log (iam V5__pg_eventbus.sql); the fan-out
 * trigger queues a delivery row per subscribed consumer group and NOTIFYs.
 *
 * Joins the caller's transaction, so the outbox row is marked PUBLISHED in the
 * same commit that makes the event visible: no dual-write window. A replay of
 * the same (topic, eventId) is a no-op via ON CONFLICT DO NOTHING.
 */
@Component
public class PgEventBus {

    private static final String INSERT_SQL = """
            INSERT INTO ihms_eventbus.event_log (topic, event_id, event_type, partition_key, envelope)
            VALUES (?, ?, ?, ?, CAST(? AS jsonb))
            ON CONFLICT (topic, event_id) DO NOTHING
            """;

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public PgEventBus(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    /** Publish one catalog envelope; returns false when it was already on the topic. */
    public boolean send(String topic, String partitionKey, Map<String, Object> envelope) {
        String json;
        try {
            json = objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Envelope is not serializable", e);
        }
        UUID eventId = UUID.fromString(String.valueOf(envelope.get("eventId")));
        Object eventType = envelope.get("eventType");
        return jdbc.update(INSERT_SQL, topic, eventId,
                eventType == null ? null : eventType.toString(), partitionKey, json) == 1;
    }
}
