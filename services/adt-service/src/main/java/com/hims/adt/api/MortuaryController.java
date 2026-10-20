package com.hims.adt.api;

import com.hims.adt.application.MortuaryService;
import com.hims.adt.domain.model.MortuaryChamber;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chambers")
public class MortuaryController {

    private final MortuaryService mortuaryService;

    public MortuaryController(MortuaryService mortuaryService) {
        this.mortuaryService = mortuaryService;
    }

    @GetMapping
    public Page<MortuaryChamber> list(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        return mortuaryService.list(page, size);
    }

    @PostMapping("/{id}/allocate")
    public MortuaryChamber allocate(@PathVariable Long id, @RequestBody MortuaryChamber chamber) {
        return mortuaryService.allocate(id, chamber);
    }

    @PatchMapping("/{id}/release")
    public MortuaryChamber release(@PathVariable Long id) {
        return mortuaryService.release(id);
    }
}
