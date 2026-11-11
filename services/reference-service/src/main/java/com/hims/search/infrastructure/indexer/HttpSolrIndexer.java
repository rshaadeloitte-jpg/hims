package com.hims.search.infrastructure.indexer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Real-Solr backend (register WP-T3): plain RestClient to SOLR_BASE_URL (the
 * Solr ROOT url, e.g. http://solr:8983 — the /solr context is appended here).
 * Enabled when app.search.backend=solr (prod profile sets SEARCH_BACKEND=solr).
 *
 * Document model: generic dynamic-field mapping — every payload value travels as
 * a string dynamic field {@code <key>_s}, all values are folded into a catch-all
 * {@code text_t} for edismax matching, and doc_id becomes the Solr uniqueKey
 * {@code id}. Writes commit synchronously (legacy relied on autoSoftCommit —
 * 07 §1.6 risk #1; explicit commit is the modernized default per 07 §1.7).
 *
 * edismax qf boosts for the patient collection per 07 §1.7 come from config
 * (app.search.patient-qf) — COMMENT-ONLY for exact legacy weights (mrno^18,
 * mobile^16, civilid^16, janaadhar^17, name^8-12): modernized field names make
 * 1:1 weight carry-over unverifiable until the managed schema lands.
 */
public class HttpSolrIndexer implements SearchIndexer {

    private final RestClient restClient;
    private final String patientQf;
    private final ObjectMapper mapper = new ObjectMapper();

    public HttpSolrIndexer(RestClient restClient, String patientQf) {
        this.restClient = restClient;
        this.patientQf = patientQf;
    }

    @Override
    public void index(String collection, String docId, Map<String, Object> payload) {
        Map<String, Object> solrDoc = toSolrDoc(docId, payload);
        postUpdate(collection, Map.of("add", List.of(Map.of("doc", solrDoc))));
    }

    @Override
    public void delete(String collection, String docId) {
        postUpdate(collection, Map.of("delete", Map.of("id", docId)));
    }

    @Override
    public SearchResultPage search(String collection, String q, int rows, int start) {
        String qf = "patient".equals(collection) ? patientQf : "text_t^1";
        JsonNode body;
        try {
            body = restClient.get()
                    .uri(uri -> uri.path("/solr/{c}/select")
                            .queryParam("defType", "edismax")
                            .queryParam("q", q)
                            .queryParam("qf", qf)
                            .queryParam("rows", rows)
                            .queryParam("start", start)
                            .queryParam("wt", "json")
                            .build(collection))
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            throw new IndexerException("Solr select failed on " + collection + ": " + e.getMessage(), e);
        }
        if (body == null) {
            throw new IndexerException("Solr select returned no body for " + collection, null);
        }
        long numFound = body.path("response").path("numFound").asLong(0);
        List<Map<String, Object>> docs = new ArrayList<>();
        for (JsonNode d : body.path("response").path("docs")) {
            Map<String, Object> doc = new LinkedHashMap<>();
            d.properties().forEach(entry ->
                    doc.put(entry.getKey(), mapper.convertValue(entry.getValue(), Object.class)));
            docs.add(doc);
        }
        return new SearchResultPage(numFound, docs);
    }

    // ------------------------------------------------------------------ helpers

    private void postUpdate(String collection, Object updateBody) {
        try {
            restClient.post()
                    .uri(uri -> uri.path("/solr/{c}/update")
                            .queryParam("commit", "true")
                            .build(collection))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(updateBody)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            throw new IndexerException("Solr update failed on " + collection + ": " + e.getMessage(), e);
        }
    }

    private Map<String, Object> toSolrDoc(String docId, Map<String, Object> payload) {
        StringBuilder text = new StringBuilder();
        Map<String, Object> doc = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : payload.entrySet()) {
            if (e.getValue() == null) {
                continue;
            }
            String value = String.valueOf(e.getValue());
            doc.put(e.getKey() + "_s", value);
            if (text.length() > 0) {
                text.append(' ');
            }
            text.append(value);
        }
        doc.put("text_t", text.toString());
        doc.put("id", docId);
        return doc;
    }
}
