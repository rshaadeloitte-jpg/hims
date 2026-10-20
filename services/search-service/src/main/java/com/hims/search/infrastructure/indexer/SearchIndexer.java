package com.hims.search.infrastructure.indexer;

import java.util.List;
import java.util.Map;

/**
 * Search indexer backend abstraction (register WP-T3). TWO implementations:
 * HttpSolrIndexer (real Solr via RestClient; SEARCH_BACKEND=solr) and the
 * deterministic InMemoryIndexer fallback. Docs are indexed with
 * collection + doc_id + payload; search executes against the SAME backend.
 */
public interface SearchIndexer {

    /**
     * Index (upsert) one document into the collection.
     *
     * @throws IndexerException when the backend write fails (caller journals FAILED)
     */
    void index(String collection, String docId, Map<String, Object> payload);

    /** Delete one document from the collection (encounter-retention path, OUT OF SCOPE caller-side). */
    void delete(String collection, String docId);

    /**
     * Execute a free-text search against the SAME backend; returns numFound plus
     * the requested page of documents.
     */
    SearchResultPage search(String collection, String q, int rows, int start);

    /** One result page: total matches in the backend + requested page of docs. */
    record SearchResultPage(long numFound, List<Map<String, Object>> docs) {}

    /** Backend failure wrapper — the ingest pipeline maps it to journal status FAILED. */
    class IndexerException extends RuntimeException {
        public IndexerException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
