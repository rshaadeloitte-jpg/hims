package com.hims.reference.infrastructure.outbox;

import com.hims.reference.ReferenceServiceApplication;
import com.hims.reference.domain.model.OutboxEvent;
import com.hims.reference.domain.repository.OutboxRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * L2 integration: outbox append (same business tx) -> relay publishes the
 * KAFKA-EVENT-CATALOG envelope to Kafka -> row marked PUBLISHED.
 * Requires Docker daemon (BLK-003); skipped automatically when unavailable.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = ReferenceServiceApplication.class)
class OutboxKafkaIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @Autowired
    private OutboxService outboxService;

    @Autowired
    private OutboxRepository repository;

    @Autowired
    private OutboxPublisher publisher;

    @Test
    void appendThenPublishRoundTrip() throws Exception {
        OutboxEvent saved = outboxService.append(
                "MasterDataChanged", "UnitMaster", 1L, "unitmaster",
                "{\"masterType\":\"unitmaster\",\"masterId\":1,\"op\":\"create\"}", "u-1-create");

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(OutboxEvent.OutboxStatus.PENDING);

        publisher.publish(saved);

        OutboxEvent finalState = null;
        long deadline = System.currentTimeMillis() + 30000;
        while (System.currentTimeMillis() < deadline) {
            finalState = repository.findById(saved.getId()).orElseThrow();
            if (finalState.getStatus() == OutboxEvent.OutboxStatus.PUBLISHED) {
                break;
            }
            Thread.sleep(500);
        }
        assertThat(finalState).isNotNull();
        assertThat(finalState.getStatus()).isEqualTo(OutboxEvent.OutboxStatus.PUBLISHED);
        assertThat(finalState.getPublishedAt()).isNotNull();
    }
}
