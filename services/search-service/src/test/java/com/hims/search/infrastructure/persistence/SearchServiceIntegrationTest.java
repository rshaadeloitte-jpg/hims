package com.hims.search.infrastructure.persistence;

import com.hims.search.SearchServiceApplication;
import com.hims.search.application.SearchIngestService;
import com.hims.search.application.exceptions.InvalidSearchRequestException;
import com.hims.search.application.exceptions.SearchNotFoundException;
import com.hims.search.domain.model.IndexRequest;
import com.hims.search.infrastructure.outbox.OutboxRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * L2 — WP-T3 search-service on real PG16: V1 schema applies (ihms_search:
 * t_index_request journal + convention outbox table), the full ingest pipeline
 * runs against the DB (route -> PENDING -> dispatch -> INDEXED), dedupe via
 * latest-row is real at the persistence level, retry FAILED->PENDING re-dispatches,
 * and — per register WP-T3 EVENT POLICY — the outbox stays EMPTY through EVERY
 * flow (explicit NEGATIVE outbox assertion, cssd-service convention).
 *
 * Backend disclosure: this class runs against the deterministic InMemoryIndexer
 * fallback (default profile wiring); the REAL Solr round-trip against a
 * Testcontainers solr:9 core runs in SearchSolrIntegrationTest.
 * NOTE: requires Docker; skipped automatically when Docker is absent
 * (@Testcontainers(disabledWithoutDocker = true)) — do not remove.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = SearchServiceApplication.class)
@DirtiesContext
@Transactional
class SearchServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private SearchIngestService service;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private EntityManager em;

    @SuppressWarnings("unchecked")
    private List<String> tables() {
        return em.createNativeQuery("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = 'ihms_search' ORDER BY table_name
                """).getResultList();
    }

    @Test
    void flywaySchema_applies_journalAndOutboxPresent() {
        assertThat(tables()).contains("t_index_request", "outbox");
        Number seqCount = (Number) em.createNativeQuery("""
                SELECT count(*) FROM information_schema.sequences
                WHERE sequence_schema = 'ihms_search'
                  AND sequence_name IN ('t_index_request_seq')
                """).getSingleResult();
        assertThat(seqCount.intValue()).isEqualTo(1);
        // EVENT POLICY: nothing has touched the outbox during migration/boot
        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void fullPipeline_routingJournalDispatch_withNegativeOutboxAssertions() throws Exception {
        assertThat(outboxRepository.count()).isZero();

        // ---- guards before anything is written --------------------------------
        assertThatThrownBy(() -> service.ingest(null, null))
                .isInstanceOf(InvalidSearchRequestException.class);
        assertThatThrownBy(() -> service.ingest("PatientRegistered", Map.of()))
                .isInstanceOf(InvalidSearchRequestException.class)
                .hasMessageContaining("docId");
        assertThatThrownBy(() -> service.ingest("InvoiceGenerated", Map.of("docId", "X")))
                .isInstanceOf(InvalidSearchRequestException.class)
                .extracting(e -> ((InvalidSearchRequestException) e).getErrorCode())
                .isEqualTo("SEARCH_UNKNOWN_EVENT_TYPE");   // sibling interop events are NOT routed here
        assertThat(outboxRepository.count()).isZero();   // rejected ingests emit nothing AND journal nothing

        // ---- route + dispatch across all five collections ----------------------
        var patient = service.ingest("PatientRegistered",
                Map.of("docId", "P-1", "mrno", "MRN-77", "name", "Aditya Test")).request();
        assertThat(patient.getStatus()).isEqualTo(IndexRequest.STATUS_INDEXED);
        assertThat(patient.getCollection()).isEqualTo("patient");

        var encounter = service.ingest("EncounterCreated", Map.of("docId", "E-1")).request();
        var admission = service.ingest("PatientAdmitted", Map.of("docId", "E-2")).request();
        var discharge = service.ingest("PatientDischarged", Map.of("docId", "E-3")).request();
        for (var r : List.of(encounter, admission, discharge)) {
            assertThat(r.getCollection()).isEqualTo("encounter");
            assertThat(r.getStatus()).isEqualTo(IndexRequest.STATUS_INDEXED);
        }

        var item = service.ingest("InventoryUpdated", Map.of("docId", "I-1")).request();
        assertThat(item.getCollection()).isEqualTo("item");

        var hf = service.ingest("MasterDataChanged", Map.of("docId", "HF-1")).request();
        assertThat(hf.getCollection()).isEqualTo("healthfactor");
        var icd = service.ingest("MasterDataChanged",
                Map.of("docId", "ICD-1", "collection", "icd10cm")).request();
        assertThat(icd.getCollection()).isEqualTo("icd10cm");

        // journal persisted with payload JSONB and audit columns
        Object addedDatetime = em.createNativeQuery("""
                SELECT added_datetime FROM ihms_search.t_index_request WHERE id = :id
                """).setParameter("id", patient.getId()).getSingleResult();
        assertThat(addedDatetime).isNotNull();

        assertThat(outboxRepository.count()).isZero();   // NEGATIVE OUTBOX: ingest emits NOTHING

        // ---- dedupe at persistence level: same key returns the SAME row --------
        var duplicate = service.ingest("PatientRegistered",
                Map.of("docId", "P-1", "mrno", "MRN-77")).request();
        assertThat(duplicate.getId()).isEqualTo(patient.getId());

        Number rowCount = (Number) em.createNativeQuery("""
                SELECT count(*) FROM ihms_search.t_index_request
                 WHERE collection = 'patient' AND doc_id = 'P-1'
                """).getSingleResult();
        assertThat(rowCount.intValue()).isEqualTo(1);   // no duplicate row written

        assertThat(outboxRepository.count()).isZero();

        // ---- search round trip through the InMemoryIndexer fallback ------------
        var page = service.search("patient", "aditya", 10, 0);
        assertThat(page.numFound()).isEqualTo(1);
        assertThat(page.docs().get(0)).containsEntry("id", "P-1");

        assertThat(outboxRepository.count()).isZero();

        // ---- retry flow: FAILED -> PENDING -> re-dispatch ----------------------
        IndexRequest failed = new IndexRequest();
        failed.setId(nextSeq());
        failed.setCollection("item");
        failed.setAction(IndexRequest.ACTION_INDEX);
        failed.setDocId("I-FAILED");
        failed.setPayload("{\"docId\":\"I-FAILED\"}");
        failed.setStatus(IndexRequest.STATUS_FAILED);
        failed.setAttempts(1);
        em.persist(failed);
        em.flush();

        assertThatThrownBy(() -> service.retry(failed.getId() + 999_999))
                .isInstanceOf(SearchNotFoundException.class);   // unknown id -> 404 path

        IndexRequest retried = service.retry(failed.getId());
        assertThat(retried.getStatus()).isEqualTo(IndexRequest.STATUS_INDEXED);

        // non-FAILED rows cannot be retried
        assertThatThrownBy(() -> service.retry(patient.getId()))
                .isInstanceOf(InvalidSearchRequestException.class)
                .extracting(e -> ((InvalidSearchRequestException) e).getErrorCode())
                .isEqualTo("SEARCH_ILLEGAL_TRANSITION");

        assertThat(outboxRepository.count()).isZero();

        // ---- journal listing filters -------------------------------------------
        assertThat(service.list("encounter", null)).extracting(IndexRequest::getDocId)
                .containsExactlyInAnyOrder("E-1", "E-2", "E-3");
        assertThat(service.list(null, (short) IndexRequest.STATUS_PENDING)).isEmpty();

        // FINAL NEGATIVE OUTBOX ASSERTION: the entire pipeline produced ZERO rows
        assertThat(outboxRepository.count()).isZero();
    }

    private Long nextSeq() {
        Number n = (Number) em.createNativeQuery("SELECT nextval('ihms_search.t_index_request_seq')")
                .getSingleResult();
        return n.longValue();
    }
}
