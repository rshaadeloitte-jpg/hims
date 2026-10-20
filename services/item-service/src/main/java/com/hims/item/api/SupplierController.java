package com.hims.item.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.hims.item.application.SupplierService;
import com.hims.item.domain.model.Supplier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Supplier master CRUD (WP-2) over the existing ihms_item.supplier table. */
@RestController
@RequestMapping("/api/v1/suppliers")
@Tag(name = "Procurement (WP-2)", description = "Supplier master, GRN posting, indents")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    @Operation(summary = "List suppliers")
    public List<Supplier> list() {
        return supplierService.list();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a supplier by id")
    public Supplier get(@PathVariable Long id) {
        return supplierService.get(id);
    }

    @PostMapping
    @Operation(summary = "Create a supplier")
    public Supplier create(@RequestBody(required = false) JsonNode body) {
        return supplierService.create(body);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a supplier")
    public Supplier update(@PathVariable Long id, @RequestBody(required = false) JsonNode body) {
        return supplierService.update(id, body);
    }
}
