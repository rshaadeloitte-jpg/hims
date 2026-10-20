package com.hims.reference.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.reference.domain.model.OutboxEvent;
import com.hims.reference.domain.repository.OutboxRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    private OutboxRepository repository;

    @Mock
    private KafkaTemplate<Object, Object> kafkaTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @SuppressWarnings("unchecked")
    void publish_sendsEnvelopeWithCatalogFields() throws Exception {
        when(repository.claim(any(Long.class), eq(1))).thenReturn(1);
        when(kafkaTemplate.send(eq("ihms.master.master-data-changed"), eq("unitmaster"), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        OutboxPublisher publisher = new OutboxPublisher(repository, kafkaTemplate, objectMapper,
                "ihms.master.master-data-changed", "dlt.ihms.master.master-data-changed", 3);

        OutboxEvent event = new OutboxEvent();
        event.setId(11L);
        event.setEventId(UUID.fromString("123e4567-e89b-42d3-a456-556642440000"));
        event.setEventType("MasterDataChanged");
        event.setAggregateType("UnitMaster");
        event.setAggregateId(1L);
        event.setPartitionKey("unitmaster");
        event.setIdempotencyKey("u-1-create");
        event.setProducerService("reference-service");
        event.setOccurredAt(Timestamp.valueOf("2026-08-14 10:00:00"));
        event.setPayload("{\"masterType\":\"unitmaster\",\"masterId\":1,\"op\":\"create\"}");

        publisher.publish(event);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("ihms.master.master-data-changed"), eq("unitmaster"), captor.capture());
        Map<String, Object> envelope = (Map<String, Object>) captor.getValue();
        assertThat(envelope).containsEntry("eventType", "MasterDataChanged");
        assertThat(envelope).containsEntry("entityId", 1L);
        assertThat(envelope).containsEntry("partitionKey", "unitmaster");
        assertThat(envelope).containsEntry("producerService", "reference-service");
        assertThat(envelope.get("eventId")).isEqualTo("123e4567-e89b-42d3-a456-556642440000");
        assertThat(envelope.get("payload")).isNotNull();
        verify(repository).markPublished(eq(11L), any(Timestamp.class));
    }

    @Test
    void publish_whenKafkaFails_marksFailed() throws Exception {
        when(repository.claim(any(Long.class), eq(1))).thenReturn(1);
        when(kafkaTemplate.send(eq("ihms.master.master-data-changed"), eq("unitmaster"), any()))
                .thenThrow(new RuntimeException("broker down"));

        OutboxPublisher publisher = new OutboxPublisher(repository, kafkaTemplate, objectMapper,
                "ihms.master.master-data-changed", "dlt.ihms.master.master-data-changed", 3);

        OutboxEvent event = new OutboxEvent();
        event.setId(11L);
        event.setEventId(UUID.randomUUID());
        event.setEventType("MasterDataChanged");
        event.setAggregateType("UnitMaster");
        event.setAggregateId(1L);
        event.setPartitionKey("unitmaster");
        event.setOccurredAt(Timestamp.valueOf("2026-08-14 10:00:00"));
        event.setPayload("{}");

        publisher.publish(event);

        verify(repository).markFailed(eq(11L), eq(1), eq(OutboxEvent.OutboxStatus.FAILED));
    }

    @Test
    void publish_whenExceedsMaxAttempts_marksDlq() throws Exception {
        when(repository.claim(any(Long.class), eq(3))).thenReturn(1);
        when(kafkaTemplate.send(any(), any(), any()))
                .thenThrow(new RuntimeException("broker down"));

        OutboxPublisher publisher = new OutboxPublisher(repository, kafkaTemplate, objectMapper,
                "ihms.master.master-data-changed", "dlt.ihms.master.master-data-changed", 3);

        OutboxEvent event = new OutboxEvent();
        event.setId(12L);
        event.setAttempts(2);
        event.setEventId(UUID.randomUUID());
        event.setEventType("MasterDataChanged");
        event.setAggregateType("UnitMaster");
        event.setAggregateId(2L);
        event.setPartitionKey("unitmaster");
        event.setOccurredAt(Timestamp.valueOf("2026-08-14 10:00:00"));
        event.setPayload("{}");

        publisher.publish(event);

        verify(repository).markFailed(eq(12L), eq(3), eq(OutboxEvent.OutboxStatus.DLQ));
    }

    @Test
    @SuppressWarnings("unchecked")
    void publish_dlqAfterMaxAttempts_routesToDlqTopic() throws Exception {
        // F-10 (AGT-10): after the attempt cap the event goes to the configured DLQ topic.
        when(repository.claim(any(Long.class), eq(3))).thenReturn(1);
        when(kafkaTemplate.send(eq("ihms.master.master-data-changed"), eq("unitmaster"), any()))
                .thenThrow(new org.springframework.kafka.KafkaException("broker down"));
        when(kafkaTemplate.send(eq("dlt.ihms.master.master-data-changed"), eq("unitmaster"), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        OutboxPublisher publisher = new OutboxPublisher(repository, kafkaTemplate, objectMapper,
                "ihms.master.master-data-changed", "dlt.ihms.master.master-data-changed", 2);

        OutboxEvent event = new OutboxEvent();
        event.setId(11L);
        event.setEventId(UUID.fromString("123e4567-e89b-42d3-a456-556642440000"));
        event.setEventType("MasterDataChanged");
        event.setAggregateType("UnitMaster");
        event.setAggregateId(1L);
        event.setPartitionKey("unitmaster");
        event.setProducerService("reference-service");
        event.setOccurredAt(Timestamp.valueOf("2026-08-14 10:00:00"));
        event.setPayload("{\"masterType\":\"unitmaster\",\"masterId\":1,\"op\":\"create\"}");
        event.setAttempts(2);   // third attempt -> cap (maxAttempts=2)

        publisher.publish(event);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("dlt.ihms.master.master-data-changed"), eq("unitmaster"), captor.capture());
        Map<?, ?> dlq = (Map<?, ?>) captor.getValue();
        assertThat(dlq.get("dlqAfterAttempts")).isEqualTo(3);
        assertThat(dlq.get("eventId")).isEqualTo("123e4567-e89b-42d3-a456-556642440000");
        verify(repository).markFailed(eq(11L), eq(3), eq(OutboxEvent.OutboxStatus.DLQ));
    }
}
