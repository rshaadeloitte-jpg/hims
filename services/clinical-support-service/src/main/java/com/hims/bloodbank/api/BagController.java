package com.hims.bloodbank.api;

import static com.hims.bloodbank.api.DonorController.*;
import com.hims.bloodbank.application.BagService;
import com.hims.bloodbank.domain.model.BloodBag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/** Bag registration (t_bloodbag) so crossmatch/issue have bags to operate on. */
@RestController
@RequestMapping("/api/v1/bags")
@Tag(name = "Bags (WP-9)", description = "Blood bag/component lifecycle (t_bloodbag); components hang off t_bloodcomponent")
public class BagController {

    private final BagService service;

    public BagController(BagService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Register a collected bag")
    public ResponseEntity<BloodBag> create(@RequestBody Map<String, Object> body) {
        LocalDate expiry = null;
        String rawExpiry = str(body, "expirydate");
        if (rawExpiry != null) {
            try {
                expiry = LocalDate.parse(rawExpiry);
            } catch (DateTimeParseException e) {
                throw new com.hims.bloodbank.application.exceptions.InvalidBloodBankRequestException(
                        "BLD_REQ_INVALID", "expirydate must be ISO yyyy-MM-dd");
            }
        }
        BloodBag bag = service.create(longOf(body, "donorId"), str(body, "bloodGroup"),
                intOf(body, "volumeMl"), expiry, longOf(body, "unitId"));
        return ResponseEntity.status(HttpStatus.CREATED).body(bag);
    }

    @GetMapping("/{id}")
    public BloodBag get(@PathVariable long id) {
        return service.get(id);
    }

    @GetMapping
    @Operation(summary = "List bags", description = "?issued=true|false filters.")
    public List<BloodBag> list(@RequestParam(required = false) Boolean issued) {
        return service.list(issued);
    }
}
