package com.hims.search.infrastructure.persistence;

import com.hims.search.SearchServiceApplication;
import com.hims.search.application.SearchIngestService;
import com.hims.search.domain.model.IndexRequest;
import com.hims.search.infrastructure.outbox.OutboxRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * L2 — REAL Solr round-trip (register WP-T3: "search round-trip covered by
 * whichever backend ran — state it"): HttpSolrIndexer against a Testcontainers
 * solr:9 core ("patient" pre-created via solr-precreate), PG16 for the journal.
 * Pipeline under test: ingest PatientRegistered -> t_index_request INDEXED ->
 * GET-search returns the indexed doc FROM SOLR. Also pins the NEGATIVE OUTBOX
 * policy with the Solr backend active.
 * NOTE: requires Docker; skipped automatically when Docker is absent
 * (@Testcontainers(disabledWithoutDocker = true)) — do not remove.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = SearchServiceApplication.class)
@DirtiesContext
@Transactional
class SearchSolrIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static final GenericContainer<?> solr =
            new GenericContainer<>(DockerImageName.parse("solr:9"))
                    .withExposedPorts(8983)
                    .withCommand("solr-precreate", "patient")
                    .waitingFor(org.testcontainers.containers.wait.strategy.Wait
                            .forHttp("/solr/patient/admin/ping?wt=json").forStatusCode(200));

    @DynamicPropertySource
    static void indexerProps(DynamicPropertyRegistry registry) {
        registry.add("app.search.backend", () -> "solr");
        registry.add("app.search.solr.base-url",
                () -> "http://" + solr.getHost() + ":" + solr.getMappedPort(8983));
    }

    @Autowired
    private SearchIngestService service;

    @Autowired
    private OutboxRepository outboxRepository;

    @Test
    void solrRoundTrip_indexThenQuery_returnsDoc_negativeOutbox() {
        assertThat(outboxRepository.count()).isZero();

        // ingest routes + journals + dispatches to the REAL Solr core synchronously
        IndexRequest journalRow = service.ingest("PatientRegistered",
                Map.of("docId", "P-SOLR-1", "mrno", "MRN-9001",
                        "name", "Solr Roundtrip", "mobile", "+91-9820000000")).request();
        assertThat(journalRow.getStatus()).isEqualTo(IndexRequest.STATUS_INDEXED);
        assertThat(journalRow.getAttempts()).isZero();

        // search executes against the SAME backend — the doc comes back from Solr
        var page = service.search("patient", "Roundtrip", 10, 0);
        assertThat(page.numFound()).isEqualTo(1);
        assertThat(page.docs()).hasSize(1);
        assertThat(page.docs().get(0).get("id")).isEqualTo("P-SOLR-1");
        assertThat(page.docs().get(0).get("mrno_s")).isEqualTo("MRN-9001");   // dynamic-field mapping

        var byMrno = service.search("patient", "MRN-9001", 10, 0);
        assertThat(byMrno.numFound()).isEqualTo(1);

        // a second distinct patient indexes independently through the same core
        IndexRequest second = service.ingest("PatientRegistered",
                Map.of("docId", "P-SOLR-2", "name", "Second Patient")).request();
        assertThat(second.getStatus()).isEqualTo(IndexRequest.STATUS_INDEXED);
        assertThat(service.search("patient", "Second Patient", 10, 0).numFound()).isEqualTo(1);

        // NEGATIVE OUTBOX: nothing emitted even with the real backend dispatching
        assertThat(outboxRepository.count()).isZero();
    }
}
