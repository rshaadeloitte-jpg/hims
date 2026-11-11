package com.hims.search.application;

import com.hims.search.application.exceptions.InvalidSearchRequestException;
import com.hims.search.application.exceptions.SearchNotFoundException;
import com.hims.search.domain.model.IndexRequest;
import com.hims.search.domain.repository.IndexRequestRepository;
import com.hims.search.infrastructure.indexer.SearchIndexer;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * L0 — event routing, dedupe latest-row semantics, dispatch state machine and
 * retry guard, no Spring context. Register WP-T3 assertions pinned here:
 *   - routing table (patient / encounter / item / icd10cm|healthfactor; unknown -> 400)
 *   - dedupe via latest-row: existing PENDING/INDEXED row is returned idempotently,
 *     existing FAILED row allows a FRESH PENDING attempt
 *   - dispatch: success -> INDEXED, backend failure -> FAILED + attempts++
 *   - retry: FAILED-only (SEARCH_ILLEGAL_TRANSITION otherwise), 404 unknown id
 *   - EVENT POLICY: the service has NO outbox dependency at all — nothing can be
 *     emitted on any path (additionally asserted at L2 with a negative outbox check).
 */
class SearchIngestServiceTest {

    private IndexRequestRepository repository;
    private SearchIndexer indexer;
    private EntityManager em;
    private SearchIngestService service;
    private final AtomicIds ids = new AtomicIds();

    static class AtomicIds {
        private long next = 100;
        Long nextVal() { return next++; }
    }

    @BeforeEach
    void setUp() {
        repository = mock(IndexRequestRepository.class);
        indexer = mock(SearchIndexer.class);
        em = mock(EntityManager.class);
        service = new SearchIngestService(repository, indexer,
                "patient,encounter,item,icd10cm,healthfactor");
        // emulate sequence nextval for every save path
        jakarta.persistence.Query seqQuery = mock(jakarta.persistence.Query.class);
        when(seqQuery.getSingleResult()).thenAnswer(inv -> ids.nextVal());
        when(em.createNativeQuery(org.mockito.ArgumentMatchers.contains("nextval")))
                .thenReturn(seqQuery);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "em", em);
    }

    // ------------------------------------------------------------------ routing

    @Test
    void routes_patientRegistered_toPatientCollection() {
        stubNoExistingRow();
        var result = service.ingest("PatientRegistered",
                Map.of("docId", "P-1", "mrno", "MRN-1"));
        assertThat(result.request().getCollection()).isEqualTo("patient");
        assertThat(result.created()).isTrue();
    }

    @Test
    void routes_encounterAdmissionDischarge_toEncounterCollection() {
        for (String eventType : List.of("EncounterCreated", "PatientAdmitted", "PatientDischarged")) {
            stubNoExistingRow();
            var result = service.ingest(eventType, Map.of("docId", "E-" + eventType));
            assertThat(result.request().getCollection()).isEqualTo("encounter");
        }
    }

    @Test
    void routes_inventoryUpdated_toItemCollection() {
        stubNoExistingRow();
        assertThat(service.ingest("InventoryUpdated", Map.of("docId", "I-1"))
                .request().getCollection()).isEqualTo("item");
    }

    @Test
    void masterDataChanged_targetsByPayloadCollection_defaultHealthFactor() {
        stubNoExistingRow();
        assertThat(service.ingest("MasterDataChanged", Map.of("docId", "M-1"))
                .request().getCollection()).isEqualTo("healthfactor");
        stubNoExistingRow();
        assertThat(service.ingest("MasterDataChanged",
                        Map.of("docId", "M-2", "collection", "icd10cm"))
                .request().getCollection()).isEqualTo("icd10cm");
    }

    @Test
    void masterDataChanged_withForeignCollection_rejected400() {
        assertThatThrownBy(() -> service.ingest("MasterDataChanged",
                        Map.of("docId", "M-3", "collection", "service")))
                .isInstanceOf(InvalidSearchRequestException.class)
                .extracting(e -> ((InvalidSearchRequestException) e).getErrorCode())
                .isEqualTo("BEXA003");
    }

    @Test
    void unknownEventType_rejected400WithRoutingCode() {
        assertThatThrownBy(() -> service.ingest("InvoiceGenerated", Map.of("docId", "X")))
                .isInstanceOf(InvalidSearchRequestException.class)
                .extracting(e -> ((InvalidSearchRequestException) e).getErrorCode())
                .isEqualTo("SEARCH_UNKNOWN_EVENT_TYPE");
    }

    // ------------------------------------------------------------------ guards

    @Test
    void blank_eventType_or_missingPayloadOrDocId_rejected400() {
        assertThatThrownBy(() -> service.ingest(" ", Map.of("docId", "X")))
                .isInstanceOf(InvalidSearchRequestException.class).hasMessageContaining("eventType");
        assertThatThrownBy(() -> service.ingest("PatientRegistered", null))
                .isInstanceOf(InvalidSearchRequestException.class).hasMessageContaining("payload");
        assertThatThrownBy(() -> service.ingest("PatientRegistered", Map.of("mrno", "MRN-1")))
                .isInstanceOf(InvalidSearchRequestException.class).hasMessageContaining("docId");
        assertThatThrownBy(() -> service.ingest("PatientRegistered", Map.of("docId", "")))
                .isInstanceOf(InvalidSearchRequestException.class).hasMessageContaining("docId");
        verify(repository, never()).save(any());
    }

    @Test
    void oversizedDocId_rejected400() {
        String tooLong = "x".repeat(61);
        assertThatThrownBy(() -> service.ingest("PatientRegistered", Map.of("docId", tooLong)))
                .isInstanceOf(InvalidSearchRequestException.class)
                .hasMessageContaining("60 characters");
    }

    // ------------------------------------------------------------------ journal + dispatch

    @Test
    void ingest_journalsPending_thenDispatchIndexed_inOneFlow() {
        stubNoExistingRow();
        var request = service.ingest("PatientRegistered",
                Map.of("docId", "P-9", "name", "Test Patient")).request();

        assertThat(request.getStatus()).isEqualTo(IndexRequest.STATUS_INDEXED);
        assertThat(request.getAction()).isEqualTo(IndexRequest.ACTION_INDEX);
        assertThat(request.getAttempts()).isZero();
        // dispatch() saves twice (PENDING journal write + post-dispatch status write);
        // both saves carry the SAME entity instance, so only the count is asserted here.
        verify(repository, org.mockito.Mockito.times(2)).save(any());
        verify(indexer).index(eq("patient"), eq("P-9"), any());
    }

    @Test
    void dispatch_backendFailure_marksFailedAndIncrementsAttempts_rowSurvives() {
        stubNoExistingRow();
        org.mockito.Mockito.doThrow(new SearchIndexer.IndexerException("solr down", null))
                .when(indexer).index(anyString(), anyString(), any());

        var request = service.ingest("PatientRegistered", Map.of("docId", "P-FAIL")).request();
        assertThat(request.getStatus()).isEqualTo(IndexRequest.STATUS_FAILED);
        assertThat(request.getAttempts()).isEqualTo(1);
        verify(repository, org.mockito.Mockito.times(2)).save(any());   // pending then failed — never lost
    }

    // ------------------------------------------------------------------ dedupe (latest-row)

    @Test
    void duplicateIngress_whenLatestRowPending_returnsExistingRow_noNewWrite() {
        IndexRequest pending = row(7L, IndexRequest.STATUS_PENDING, 0);
        when(repository.findTopByCollectionAndDocIdAndActionOrderByIdDesc(
                "patient", "P-DUP", IndexRequest.ACTION_INDEX))
                .thenReturn(Optional.of(pending));

        var result = service.ingest("PatientRegistered", Map.of("docId", "P-DUP"));
        assertThat(result.created()).isFalse();
        assertThat(result.request().getId()).isEqualTo(7L);
        verify(repository, never()).save(any());   // dedupe = zero writes
        verify(indexer, never()).index(anyString(), anyString(), any());
    }

    @Test
    void duplicateIngress_whenLatestRowIndexed_returnsExistingRow_noNewWrite() {
        IndexRequest indexed = row(8L, IndexRequest.STATUS_INDEXED, 1);
        when(repository.findTopByCollectionAndDocIdAndActionOrderByIdDesc(
                "encounter", "E-DUP", IndexRequest.ACTION_INDEX))
                .thenReturn(Optional.of(indexed));

        var result = service.ingest("EncounterCreated", Map.of("docId", "E-DUP"));
        assertThat(result.created()).isFalse();
        assertThat(result.request().getId()).isEqualTo(8L);
        verify(repository, never()).save(any());
    }

    @Test
    void failedLatestRow_allowsFreshPendingAttempt_newRowCreated() {
        IndexRequest failed = row(9L, IndexRequest.STATUS_FAILED, 2);
        when(repository.findTopByCollectionAndDocIdAndActionOrderByIdDesc(
                "patient", "P-RETRY", IndexRequest.ACTION_INDEX))
                .thenReturn(Optional.of(failed));

        var result = service.ingest("PatientRegistered", Map.of("docId", "P-RETRY"));
        assertThat(result.created()).isTrue();
        assertThat(result.request().getId()).isNotEqualTo(9L);
        assertThat(result.request().getStatus()).isEqualTo(IndexRequest.STATUS_INDEXED);
    }

    // ------------------------------------------------------------------ search guards

    @Test
    void search_unknownCollection_rejected400_liveCollectionsOnly() {
        assertThatThrownBy(() -> service.search("service", "x", 10, 0))   // orphan dropped per 07 §1.2
                .isInstanceOf(InvalidSearchRequestException.class)
                .extracting(e -> ((InvalidSearchRequestException) e).getErrorCode())
                .isEqualTo("SEARCH_UNKNOWN_COLLECTION");
        assertThatThrownBy(() -> service.search(null, "x", 10, 0))
                .isInstanceOf(InvalidSearchRequestException.class);
    }

    @Test
    void search_blankQ_rejected400_andPaginationClamped() {
        assertThatThrownBy(() -> service.search("patient", " ", 10, 0))
                .isInstanceOf(InvalidSearchRequestException.class)
                .hasMessageContaining("q is required");

        when(indexer.search("patient", "abc", 100, 0))
                .thenReturn(new SearchIndexer.SearchResultPage(0, List.of()));
        service.search("patient", "abc", 5000, -20);   // rows clamped to <=100, start >= 0
        verify(indexer).search("patient", "abc", 100, 0);
    }

    @Test
    void liveCollections_returnsTheFiveLiveCollections() {
        assertThat(service.liveCollections()).containsExactly(
                "patient", "encounter", "item", "icd10cm", "healthfactor");
    }

    // ------------------------------------------------------------------ retry

    @Test
    void retry_unknownId_mapsTo404() {
        when(repository.findById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.retry(404L))
                .isInstanceOf(SearchNotFoundException.class)
                .extracting(e -> ((SearchNotFoundException) e).getErrorCode())
                .isEqualTo("NOT_FOUND");
    }

    @Test
    void retry_nonFailedStatus_illegalTransition400() {
        for (short status : List.of(IndexRequest.STATUS_PENDING, IndexRequest.STATUS_INDEXED)) {
            when(repository.findById(5L)).thenReturn(Optional.of(row(5L, status, 0)));
            assertThatThrownBy(() -> service.retry(5L))
                    .isInstanceOf(InvalidSearchRequestException.class)
                    .extracting(e -> ((InvalidSearchRequestException) e).getErrorCode())
                    .isEqualTo("SEARCH_ILLEGAL_TRANSITION");
        }
    }

    @Test
    void retry_failedRow_resetsToPendingThenRedispatches() {
        IndexRequest failed = row(6L, IndexRequest.STATUS_FAILED, 1);
        when(repository.findById(6L)).thenReturn(Optional.of(failed));

        IndexRequest retried = service.retry(6L);
        assertThat(retried.getStatus()).isEqualTo(IndexRequest.STATUS_INDEXED);
        assertThat(retried.getAttempts()).isEqualTo(1);   // attempts unchanged by a SUCCESSFUL re-dispatch
        verify(indexer).index(eq("patient"), eq("D-6"), any());
    }

    @Test
    void retry_failedRow_secondBackendFailure_failsAgain_attemptsIncremented() {
        IndexRequest failed = row(11L, IndexRequest.STATUS_FAILED, 0);
        when(repository.findById(11L)).thenReturn(Optional.of(failed));
        org.mockito.Mockito.doThrow(new SearchIndexer.IndexerException("still down", null))
                .when(indexer).index(anyString(), anyString(), any());

        IndexRequest retried = service.retry(11L);
        assertThat(retried.getStatus()).isEqualTo(IndexRequest.STATUS_FAILED);
        assertThat(retried.getAttempts()).isEqualTo(1);
    }

    // ------------------------------------------------------------------ helpers

    private void stubNoExistingRow() {
        when(repository.findTopByCollectionAndDocIdAndActionOrderByIdDesc(
                anyString(), anyString(), eq(IndexRequest.ACTION_INDEX)))
                .thenReturn(Optional.empty());
    }

    private IndexRequest row(long id, short status, int attempts) {
        IndexRequest r = new IndexRequest();
        r.setId(id);
        r.setCollection("patient");
        r.setAction(IndexRequest.ACTION_INDEX);
        r.setDocId("D-" + id);
        r.setPayload("{\"docId\":\"D-" + id + "\"}");
        r.setStatus(status);
        r.setAttempts(attempts);
        return r;
    }
}
