package com.hims.item.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.hims.item.application.IndentService;
import com.hims.item.domain.model.Indent;
import com.hims.item.domain.model.IndentDetail;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Indent create / approve / list (WP-2). doc_status: 1=PENDING 2=APPROVED. */
@RestController
@RequestMapping("/api/v1/indents")
@Tag(name = "Procurement (WP-2)", description = "Supplier master, GRN posting, indents")
public class IndentController {

    private final IndentService indentService;

    public IndentController(IndentService indentService) {
        this.indentService = indentService;
    }

    @PostMapping
    @Operation(summary = "Create an indent (doc_status=1 PENDING)",
            description = "Body: {storeId, departmentId?, indentNo?, remarks?, lines:[{itemId, quantity, remarks?}]}.")
    public Indent create(@RequestBody(required = false) JsonNode body) {
        return indentService.create(body);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a pending indent",
            description = "doc_status PENDING -> APPROVED; emits an IndentApproved outbox event in the same tx.")
    public Indent approve(@PathVariable Long id) {
        return indentService.approve(id);
    }

    @GetMapping
    @Operation(summary = "List indents, optionally filtered by doc_status",
            description = "status: 1=PENDING 2=APPROVED 3=REJECTED 4=CANCELLED.")
    public List<Indent> list(@RequestParam(required = false) Short status) {
        return indentService.list(status);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an indent header by id")
    public Indent get(@PathVariable Long id) {
        return indentService.get(id);
    }

    @GetMapping("/{id}/lines")
    @Operation(summary = "Get indent lines")
    public List<IndentDetail> lines(@PathVariable Long id) {
        return indentService.getDetails(id);
    }
}
