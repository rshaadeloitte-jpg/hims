package com.hims.search.api;

import com.hims.search.application.SearchIngestService;
import com.hims.search.application.exceptions.InvalidSearchRequestException;
import com.hims.search.domain.model.IndexRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Index-journal API (register WP-T3): filtered journal listing + FAILED-only
 * retry (FAILED -> PENDING, then synchronous re-dispatch).
 */
@RestController
@RequestMapping("/api/v1/index-requests")
@Tag(name = "Index requests (WP-T3)", description = "t_index_request journal queries and retry. Status machine 0=PENDING 1=INDEXED 2=FAILED; retry guard FAILED-only (SEARCH_ILLEGAL_TRANSITION otherwise)")
public class IndexRequestController {

    private final SearchIngestService service;

    public IndexRequestController(SearchIngestService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List index-request journal rows",
            description = "Optional ?collection= and ?status= (0=PENDING 1=INDEXED 2=FAILED) filters, newest first.")
    public List<IndexRequest> list(@RequestParam(required = false) String collection,
                                   @RequestParam(required = false) Short status) {
        if (status != null && (status < 0 || status > 2)) {
            throw new InvalidSearchRequestException("BEXA003",
                    "Unknown status: " + status + " — expected 0=PENDING, 1=INDEXED or 2=FAILED");
        }
        return service.list(collection, status);
    }

    @PutMapping("/{id}/retry")
    @Operation(summary = "Retry a FAILED index request",
            description = "Guard FAILED-only: 2 -> 0=PENDING then immediate re-dispatch "
                    + "(-> 1=INDEXED or back to 2=FAILED with attempts incremented). "
                    + "404 unknown id; 400 SEARCH_ILLEGAL_TRANSITION from any other status.")
    public IndexRequest retry(@PathVariable long id) {
        return service.retry(id);
    }
}
