package com.hims.reference.infrastructure.outbox;

import com.hims.reference.ReferenceServiceApplication;
import com.hims.reference.domain.model.OutboxEvent;
import com.hims.reference.domain.repository.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * L2 integration: outbox append (same business tx) -> relay publishes the
 * KAFKA-EVENT-CATALOG envelope to ihms_eventbus.event_log -> row marked PUBLISHED.
 * Requires Docker daemon (BLK-003); skipped automatically when unavailable.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = ReferenceServiceApplication.class)
class OutboxEventBusIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    /** ihms_eventbus is owned by iam's Flyway; apply the same idempotent script here. */
    static final Path EVENTBUS_DDL = Path.of("../iam/src/main/resources/db/migration/V5__pg_eventbus.sql");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private OutboxService outboxService;

    @Autowired
    private OutboxRepository repository;

    @Autowired
    private OutboxPublisher publisher;

    @BeforeEach
    void eventBus() throws Exception {
        jdbc.execute(Files.readString(EVENTBUS_DDL));
    }

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
        Integer logged = jdbc.queryForObject(
                "SELECT COUNT(*) FROM ihms_eventbus.event_log WHERE topic = 'ihms.master.master-data-changed' "
                        + "AND event_id = ? AND envelope->>'eventType' = 'MasterDataChanged'",
                Integer.class, saved.getEventId());
        assertThat(logged).isEqualTo(1);
    }
}
