package com.hims.search.infrastructure.indexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Deterministic in-process indexer fallback (register WP-T3): default backend
 * for tests/local. Matching semantics: case-insensitive substring match of the
 * query against ANY stringified payload value; deterministic ordering by
 * doc_id; rows/start pagination. NOT a Solr replacement — no analysis,
 * no scoring — but round-trip deterministic (index then query returns doc).
 */
public class InMemoryIndexer implements SearchIndexer {

    /** collection -> (docId -> stored document copy), sorted by docId for determinism. */
    private final Map<String, Map<String, Map<String, Object>>> collections = new ConcurrentHashMap<>();

    @Override
    public void index(String collection, String docId, Map<String, Object> payload) {
        requireCollection(collection);
        if (docId == null || docId.isBlank()) {
            throw new IndexerException("docId required by the indexer backend", null);
        }
        Map<String, Object> doc = new TreeMap<>();
        if (payload != null) {
            payload.forEach((k, v) -> {
                if (v != null) {
                    doc.put(k, v);
                }
            });
        }
        doc.put("id", docId);
        collections.computeIfAbsent(collection, c -> new ConcurrentHashMap<>())
                .put(docId, doc);
    }

    @Override
    public void delete(String collection, String docId) {
        requireCollection(collection);
        Map<String, Map<String, Object>> docs = collections.get(collection);
        if (docs != null) {
            docs.remove(docId);
        }
    }

    @Override
    public SearchResultPage search(String collection, String q, int rows, int start) {
        requireCollection(collection);
        if (q == null || q.isBlank()) {
            throw new IndexerException("q must be non-blank", null);
        }
        Map<String, Map<String, Object>> docs = collections.getOrDefault(collection, Map.of());
        List<Map<String, Object>> hits = new ArrayList<>();
        for (String docId : docs.keySet().stream().sorted().toList()) {   // deterministic order
            Map<String, Object> doc = docs.get(docId);
            if (matches(doc, q)) {
                hits.add(doc);
            }
        }
        long numFound = hits.size();
        int from = Math.max(0, start);
        int to = Math.min(hits.size(), from + Math.max(0, rows));
        return new SearchResultPage(numFound, List.copyOf(hits.subList(from, to)));
    }

    /** Case-insensitive substring match against any stringified non-null value. */
    private boolean matches(Map<String, Object> doc, String q) {
        String needle = q.trim().toLowerCase();
        return doc.values().stream()
                .map(String::valueOf)
                .anyMatch(v -> v.toLowerCase().contains(needle));
    }

    private void requireCollection(String collection) {
        if (collection == null || collection.isBlank()) {
            throw new IndexerException("collection required by the indexer backend", null);
        }
    }

    /** Test/ops helper: number of live docs in a collection (not part of the SPI). */
    public int docCount(String collection) {
        return collections.getOrDefault(collection, Map.of()).size();
    }
}
