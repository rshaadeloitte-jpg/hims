package com.hims.rmrs.api;

import com.hims.rmrs.application.PmrdService;
import com.hims.rmrs.domain.model.PmrdForm;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/forms")
public class PmrdController {

    private final PmrdService pmrdService;

    public PmrdController(PmrdService pmrdService) {
        this.pmrdService = pmrdService;
    }

    @GetMapping
    public Page<PmrdForm> list(@RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "20") int size) {
        return pmrdService.list(page, size);
    }

    @PostMapping
    public ResponseEntity<PmrdForm> create(@RequestBody PmrdForm form) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pmrdService.create(form));
    }

    @GetMapping("/{id}")
    public PmrdForm get(@PathVariable Long id) {
        return pmrdService.get(id);
    }
}
