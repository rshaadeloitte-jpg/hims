package com.hims.payments.api;

import com.hims.payments.application.DbtService;
import com.hims.payments.domain.model.DbtClaim;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dbt-claims")
public class DbtController {

    private final DbtService dbtService;

    public DbtController(DbtService dbtService) {
        this.dbtService = dbtService;
    }

    @GetMapping
    public Page<DbtClaim> list(@RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "20") int size) {
        return dbtService.list(page, size);
    }

    @PostMapping
    public ResponseEntity<DbtClaim> create(@RequestBody DbtClaim claim) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dbtService.create(claim));
    }

    @PatchMapping("/{id}/status")
    public DbtClaim updateStatus(@PathVariable Long id, @RequestParam String status) {
        return dbtService.updateStatus(id, status);
    }
}
