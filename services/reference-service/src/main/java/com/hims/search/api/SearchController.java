package com.hims.search.api;

import com.hims.search.application.SearchIngestService;
import com.hims.search.infrastructure.indexer.SearchIndexer.SearchResultPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Search API (register WP-T3): free-text query against the SAME backend that
 * ingested docs, plus the live-collections discovery endpoint. edismax qf boosts
 * for the patient collection are encoded in config (app.search.patient-qf);
 * exact legacy weights per 07 §1.4 are comment-only.
 */
@RestController
@RequestMapping("/api/v1/search")
@Tag(name = "Search (WP-T3)", description = "Free-text search over the indexer backend (Solr edismax in prod; deterministic in-memory fallback otherwise) and the 5 live collections per 07 §1.2 ('service' orphan dropped)")
public class SearchController {

    private final SearchIngestService service;

    public SearchController(SearchIngestService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Search a collection",
            description = "?collection=&q=&rows=&start= — collection must be one of the 5 LIVE collections; "
                    + "q required; rows default 10 (clamped 1..100); start default 0. Patient-collection qf "
                    + "boosts per config app.search.patient-qf (legacy weights comment-only).")
    public Map<String, Object> search(@RequestParam String collection,
                                      @RequestParam String q,
                                      @RequestParam(defaultValue = "10") int rows,
                                      @RequestParam(defaultValue = "0") int start) {
        SearchResultPage page = service.search(collection, q, rows, start);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("collection", collection);
        body.put("q", q);
        body.put("start", start);
        body.put("rows", rows);
        body.put("numFound", page.numFound());
        body.put("docs", page.docs());
        return body;
    }

    @GetMapping("/collections")
    @Operation(summary = "List the live collections",
            description = "The 5 live collections: patient, encounter, item, icd10cm, healthfactor. "
                    + "The legacy 'service' collection orphan is dropped per 07 §1.2 (no indexer, no reader).")
    public List<String> collections() {
        return service.liveCollections();
    }
}
