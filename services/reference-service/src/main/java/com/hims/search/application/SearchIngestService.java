package com.hims.search.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.search.application.exceptions.InvalidSearchRequestException;
import com.hims.search.application.exceptions.SearchNotFoundException;
import com.hims.search.domain.model.IndexRequest;
import com.hims.search.domain.repository.IndexRequestRepository;
import com.hims.search.infrastructure.indexer.SearchIndexer;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Search &amp; Index slice (GAP-REGISTER-6 WP-T3, Catalog #27). The t_index_request
 * journal is the durable part of the indexing pipeline — Solr document state
 * itself lives in Solr (07 §1).
 *
 * EVENT ROUTING (internal ingress; production would be a Kafka consumer of
 * sibling topics — documented at every site, not modeled):
 *   PatientRegistered                                   -> patient    INDEX
 *   EncounterCreated / PatientAdmitted / PatientDischarged -> encounter INDEX
 *   InventoryUpdated                                    -> item       INDEX
 *   MasterDataChanged                                   -> icd10cm | healthfactor INDEX
 *       (by payload.collection; default healthfactor)
 *   unknown eventType                                   -> 400 SEARCH_UNKNOWN_EVENT_TYPE
 *
 * DEDUPE via LATEST-ROW query (register: UNIQUE(collection,doc_id,action,status)
 * deliberately NOT enforced): an existing PENDING/INDEXED row for the key makes
 * duplicate ingress idempotent (returns the existing row, no new write); an
 * existing FAILED row allows a FRESH PENDING attempt.
 *
 * DISPATCH is synchronous after journaling: success -> 1=INDEXED; backend failure
 * -> 2=FAILED with attempts incremented (row persists — never lost). Retry guard:
 * FAILED-only (2 -> 0) then re-dispatch.
 *
 * EVENT POLICY: this service EMITS NOTHING. There is no outbox emitter anywhere
 * in this codebase and the integration test asserts the outbox stays EMPTY
 * through every flow (negative outbox assertion).
 */
@Service
public class SearchIngestService {

    private static final Logger log = LoggerFactory.getLogger(SearchIngestService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    public static final String EV_PATIENT_REGISTERED = "PatientRegistered";
    public static final String EV_ENCOUNTER_CREATED = "EncounterCreated";
    public static final String EV_PATIENT_ADMITTED = "PatientAdmitted";
    public static final String EV_PATIENT_DISCHARGED = "PatientDischarged";
    public static final String EV_INVENTORY_UPDATED = "InventoryUpdated";
    public static final String EV_MASTER_DATA_CHANGED = "MasterDataChanged";

    public static final String COLL_PATIENT = "patient";
    public static final String COLL_ENCOUNTER = "encounter";
    public static final String COLL_ITEM = "item";
    public static final String COLL_ICD10CM = "icd10cm";
    public static final String COLL_HEALTHFACTOR = "healthfactor";

    private final IndexRequestRepository repository;
    private final SearchIndexer indexer;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<String> liveCollections;

    @jakarta.persistence.PersistenceContext
    private EntityManager em;

    public SearchIngestService(IndexRequestRepository repository,
                               SearchIndexer indexer,
                               @Value("${app.search.live-collections}") String liveCollections) {
        this.repository = repository;
        this.indexer = indexer;
        this.liveCollections = List.of(liveCollections.split(",")).stream()
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    // ------------------------------------------------------------------ ingest

    /**
     * Route one internal event to the journal + dispatch it to the indexer backend.
     * Guards (400): eventType required; payload must be an object with a non-blank
     * docId (<= 60 chars); MasterDataChanged payload.collection must be icd10cm or
     * healthfactor when present. Unknown eventType -> SEARCH_UNKNOWN_EVENT_TYPE.
     */
    @Transactional
    public IngestResult ingest(String eventType, Map<String, Object> payload) {
        if (eventType == null || eventType.isBlank()) {
            throw new InvalidSearchRequestException("BEXA003", "eventType is required");
        }
        if (payload == null) {
            throw new InvalidSearchRequestException("BEXA003", "payload object is required");
        }
        String docId = str(payload.get("docId"));
        if (docId == null || docId.isBlank()) {
            throw new InvalidSearchRequestException("BEXA003",
                    "payload.docId is required for event type " + eventType);
        }
        if (docId.length() > 60) {
            throw new InvalidSearchRequestException("BEXA003",
                    "payload.docId exceeds 60 characters");
        }

        String collection = route(eventType, payload);
        short action = IndexRequest.ACTION_INDEX;   // DELETE routes land with the retention job (OUT OF SCOPE)

        // dedupe via latest-row query — PENDING/INDEXED wins, FAILED allows a fresh row
        var existing = repository.findTopByCollectionAndDocIdAndActionOrderByIdDesc(
                collection, docId, action);
        if (existing.isPresent() && !existing.get().isFailed()) {
            log.info("Duplicate {} for {}/{} — returning existing journal row {} (status {})",
                    eventType, collection, docId, existing.get().getId(),
                    IndexRequest.statusName(existing.get().getStatus()));
            return new IngestResult(existing.get(), false);
        }

        Timestamp nowTs = now();
        IndexRequest request = new IndexRequest();
        request.setId(nextId("t_index_request_seq"));
        request.setCollection(collection);
        request.setAction(action);
        request.setDocId(docId);
        request.setPayload(toJson(payload));
        request.setStatus(IndexRequest.STATUS_PENDING);
        request.setAddedBy(AUDIT_ACTOR_ID);
        request.setAddedDatetime(nowTs);
        repository.save(request);

        dispatch(request);
        return new IngestResult(request, true);
    }

    /** Event-type routing table — see class doc. Returns the target collection. */
    String route(String eventType, Map<String, Object> payload) {
        return switch (eventType.trim()) {
            case EV_PATIENT_REGISTERED -> COLL_PATIENT;
            case EV_ENCOUNTER_CREATED, EV_PATIENT_ADMITTED, EV_PATIENT_DISCHARGED -> COLL_ENCOUNTER;
            case EV_INVENTORY_UPDATED -> COLL_ITEM;
            case EV_MASTER_DATA_CHANGED -> masterDataCollection(payload);
            default -> throw new InvalidSearchRequestException("SEARCH_UNKNOWN_EVENT_TYPE",
                    "Unknown eventType: " + eventType);
        };
    }

    /** MasterDataChanged targets icd10cm or healthfactor by payload.collection; default healthfactor. */
    private String masterDataCollection(Map<String, Object> payload) {
        Object requested = payload.get("collection");
        if (requested == null) {
            return COLL_HEALTHFACTOR;
        }
        String value = String.valueOf(requested).trim().toLowerCase();
        if (COLL_ICD10CM.equals(value)) {
            return COLL_ICD10CM;
        }
        if (COLL_HEALTHFACTOR.equals(value)) {
            return COLL_HEALTHFACTOR;
        }
        throw new InvalidSearchRequestException("BEXA003",
                "MasterDataChanged payload.collection must be '" + COLL_ICD10CM + "' or '"
                        + COLL_HEALTHFACTOR + "' — got: " + requested);
    }

    /** Synchronous dispatch: INDEXED on success, FAILED + attempts++ on backend error. */
    void dispatch(IndexRequest request) {
        try {
            Map<String, Object> payloadDoc = fromJson(request.getPayload());
            indexer.index(request.getCollection(), request.getDocId(), payloadDoc);
            request.setStatus(IndexRequest.STATUS_INDEXED);
        } catch (RuntimeException e) {
            log.error("Indexer dispatch failed for journal row {}/{}", request.getCollection(),
                    request.getDocId(), e);
            request.setStatus(IndexRequest.STATUS_FAILED);
            request.setAttempts(request.getAttempts() + 1);
        }
        request.setUpdatedBy(AUDIT_ACTOR_ID);
        request.setUpdatedDatetime(now());
        repository.save(request);
    }

    // ------------------------------------------------------------------ retry

    /**
     * PUT /api/v1/index-requests/{id}/retry — guard FAILED-only (2 -> 0), then
     * re-dispatch synchronously. Unknown id -> 404; non-FAILED status -> 400
     * SEARCH_ILLEGAL_TRANSITION.
     */
    @Transactional
    public IndexRequest retry(long id) {
        IndexRequest request = repository.findById(id)
                .orElseThrow(() -> new SearchNotFoundException("NOT_FOUND",
                        "Index request not found: " + id));
        if (!request.isFailed()) {
            throw new InvalidSearchRequestException("SEARCH_ILLEGAL_TRANSITION",
                    "Cannot retry index request " + id + " from status "
                            + IndexRequest.statusName(request.getStatus())
                            + " — only FAILED requests can be retried");
        }
        request.setStatus(IndexRequest.STATUS_PENDING);
        request.setUpdatedBy(AUDIT_ACTOR_ID);
        request.setUpdatedDatetime(now());
        repository.save(request);
        dispatch(request);
        log.info("Retried index request {} -> {}", id,
                IndexRequest.statusName(request.getStatus()));
        return request;
    }

    // ------------------------------------------------------------------ queries

    @Transactional(readOnly = true)
    public List<IndexRequest> list(String collection, Short status) {
        return repository.findByFilters(
                collection == null || collection.isBlank() ? null : collection,
                status);
    }

    /**
     * GET /api/v1/search — executes against the SAME backend that ingested docs.
     * Guard: collection must be one of the 5 LIVE collections (the legacy 'service'
     * orphan is dropped per 07 §1.2); q required.
     */
    public SearchIndexer.SearchResultPage search(String collection, String q, int rows, int start) {
        if (collection == null || !liveCollections.contains(collection)) {
            throw new InvalidSearchRequestException("SEARCH_UNKNOWN_COLLECTION",
                    "Unknown collection: " + collection + " — live collections are "
                            + liveCollections);
        }
        if (q == null || q.isBlank()) {
            throw new InvalidSearchRequestException("BEXA003", "q is required");
        }
        int safeRows = Math.min(Math.max(rows, 1), 100);
        int safeStart = Math.max(start, 0);
        return indexer.search(collection, q.trim(), safeRows, safeStart);
    }

    public List<String> liveCollections() {
        return List.copyOf(liveCollections);
    }

    // ------------------------------------------------------------------ helpers

    private String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize journal payload", e);
        }
    }

    private Map<String, Object> fromJson(String json) {
        try {
            if (json == null) {
                return new LinkedHashMap<>();
            }
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize journal payload", e);
        }
    }

    private Long nextId(String sequenceName) {
        Number n = (Number) em.createNativeQuery("SELECT nextval('ihms_search." + sequenceName + "')")
                .getSingleResult();
        return n.longValue();
    }

    private static Timestamp now() {
        return Timestamp.from(Instant.now());
    }

    /** Result of an ingest: the journal row plus whether THIS call created it. */
    public record IngestResult(IndexRequest request, boolean created) {}
}
