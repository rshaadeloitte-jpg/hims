package com.hims.item.api;

import com.hims.item.application.ApprovalWorklistService;
import com.hims.item.application.ApprovalWorklistService.ApprovalWorklistItem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * M13 approval worklist (M13 gap). Consolidated read-only queue of
 * actionable inventory documents (indents, GRNs, custom process orders,
 * expiry/damage returns, stock transfers) so a store manager can act on
 * them from one screen.
 *
 * <p>No mutations; the controller only projects the existing
 * PENDING/REQUESTED documents from each source table into a normalised
 * read-model row.
 */
@RestController
@RequestMapping("/api/v1/inventory/approval-worklist")
@Tag(name = "Inventory Approvals (M13)", description = "M13 approval worklist - consolidated "
        + "PENDING queue of indents, GRNs, CPOs, expiry/damage returns, stock transfers")
public class ApprovalWorklistController {

    private final ApprovalWorklistService worklistService;

    public ApprovalWorklistController(ApprovalWorklistService worklistService) {
        this.worklistService = worklistService;
    }

    @GetMapping
    @Operation(summary = "Get the consolidated approval worklist (PENDING only)",
            description = "Returns the actionable (PENDING/REQUESTED) inventory documents "
                    + "across the module as a single normalised queue so a store manager can "
                    + "act on them in one place. docType filters: INDENT, GRN, CPO, EXPIRY_RETURN, "
                    + "STOCK_TRANSFER (omit docType to get all types). storeId is optional; when "
                    + "omitted, results are not restricted to a single store. docStatus on every "
                    + "row is the normalised value: 1=PENDING 2=APPROVED 3=REJECTED/RECEIVED "
                    + "4=CANCELLED. Capped at 200 rows per call.")
    public List<ApprovalWorklistItem> worklist(
            @Parameter(description = "Optional store filter; when null, return rows for all stores",
                    example = "10")
            @RequestParam(required = false) Long storeId,
            @Parameter(description = "Optional document-type filter; one of INDENT, GRN, CPO, "
                    + "EXPIRY_RETURN, STOCK_TRANSFER. When null, all types are returned.",
                    example = "INDENT")
            @RequestParam(required = false) String docType) {
        return worklistService.worklist(storeId, docType);
    }
}
