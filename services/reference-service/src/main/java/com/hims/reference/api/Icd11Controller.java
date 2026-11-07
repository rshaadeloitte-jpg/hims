package com.hims.reference.api;

import com.hims.reference.application.Icd11Service;
import com.hims.reference.domain.model.Icd11Mms;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/icd11")
@Tag(name = "ICD-11 MMS", description = "WHO ICD-11 MMS catalog endpoints")
public class Icd11Controller {

    private final Icd11Service service;

    public Icd11Controller(Icd11Service service) {
        this.service = service;
    }

    @GetMapping("/search")
    @Operation(summary = "Search ICD-11 codes and titles",
            description = "Search by code prefix or title contains, case-insensitive, coded categories first")
    public List<Icd11Mms> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer limit) {
        return service.search(q, limit);
    }

    @GetMapping("/{code}")
    @Operation(summary = "Lookup ICD-11 category by code")
    public Icd11Mms getByCode(@PathVariable String code) {
        return service.getByCode(code);
    }
}
