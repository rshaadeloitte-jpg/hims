package com.hims.rmrs.api;

import com.hims.rmrs.application.CollectionService;
import com.hims.rmrs.application.ServiceWiseSummaryRow;
import com.hims.rmrs.application.UserWiseSummaryRow;
import com.hims.rmrs.domain.model.RmrsCollection;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * RMRS collections API (register WP-R5). Creation validates head/ledger
 * existence + head-ledger consistency and writes the collection + balanced
 * postings in the SAME tx. CollectionPosted is PROPOSED — NO outbox writes.
 * List pagination is limit-offset (legacy ROWNUM pagination :1361-1366 replaced).
 */
@RestController
@RequestMapping("/api/v1/collections")
@Tag(name = "Collections (WP-R5)", description = "RMRS & PMRD (Catalog #24): cash collections with receipt uniqueness, balanced double-entry postings (SUM(debit)=SUM(credit) per collection) in the same tx; service-wise + user-wise summaries; CollectionPosted PROPOSED -> no outbox writes")
public class CollectionController {

    /** Hard cap so limit-offset pagination cannot be abused with size=Integer.MAX. */
    static final int MAX_PAGE_SIZE = 200;

    private final CollectionService service;

    public CollectionController(CollectionService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Create a cash collection with balanced postings",
            description = "Guards: receiptNo required + unique (409 RMRS_DUPLICATE_RECEIPT), collectionDate required, amount > 0, "
                    + "headCode/ledgerCode must exist, head-ledger consistency (400 BEXA003), optional authorityCode must exist. "
                    + "Writes collection + DR/CR postings in ONE tx; emits NOTHING to the outbox (CollectionPosted PROPOSED).")
    public ResponseEntity<RmrsCollection> create(@RequestBody CreateCollectionRequest body) {
        CollectionService.CollectionInput input = new CollectionService.CollectionInput(
                body.receiptNo(), body.collectionDate(), body.patientId(), body.amount(),
                body.headCode(), body.ledgerCode(), body.authorityCode(),
                body.paymentRef(), body.collectedBy());
        RmrsCollection created = service.create(input);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @Operation(summary = "Search collections (paginated)",
            description = "?dateFrom=&dateTo=&headId= filters; limit-offset pagination (?page=&size=, size capped at 200) "
                    + "replacing legacy ROWNUM pagination (:1361-1366).")
    public Page<RmrsCollection> list(@RequestParam(required = false) LocalDate dateFrom,
                                     @RequestParam(required = false) LocalDate dateTo,
                                     @RequestParam(required = false) Long headId,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        return service.search(dateFrom, dateTo, headId, pageable(page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Full collection with its accounting entries")
    public RmrsCollection get(@PathVariable long id) {
        return service.get(id);
    }

    @GetMapping("/summary/service-wise")
    @Operation(summary = "Service-wise collection summary (s_servicewisecollection analog)",
            description = "Totals grouped by treasury head: SUM(amount) and COUNT per head in ?dateFrom..&dateTo.")
    public List<ServiceWiseSummaryRow> summaryServiceWise(@RequestParam(required = false) LocalDate dateFrom,
                                                          @RequestParam(required = false) LocalDate dateTo) {
        return service.summaryServiceWise(dateFrom, dateTo);
    }

    @GetMapping("/summary/user-wise")
    @Operation(summary = "User-wise collection summary (s_userwisecollection analog)",
            description = "Totals grouped by collectedBy: SUM(amount) and COUNT per user in ?dateFrom..&dateTo.")
    public List<UserWiseSummaryRow> summaryUserWise(@RequestParam(required = false) LocalDate dateFrom,
                                                    @RequestParam(required = false) LocalDate dateTo) {
        return service.summaryUserWise(dateFrom, dateTo);
    }

    // ------------------------------------------------------------------ helpers

    private static Pageable pageable(int page, int size) {
        if (page < 0) {
            page = 0;
        }
        if (size < 1) {
            size = 1;
        }
        if (size > MAX_PAGE_SIZE) {
            size = MAX_PAGE_SIZE;
        }
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "collectionDate", "collectionId"));
    }

    public record CreateCollectionRequest(String receiptNo, LocalDate collectionDate, Long patientId,
                                          java.math.BigDecimal amount, String headCode, String ledgerCode,
                                          String authorityCode, String paymentRef, String collectedBy) {}
}
