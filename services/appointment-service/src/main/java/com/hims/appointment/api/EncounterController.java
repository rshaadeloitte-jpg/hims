package com.hims.appointment.api;

import com.hims.appointment.application.EncounterService;
import com.hims.appointment.domain.model.Encounter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Encounter read + split-brain compatibility API (MIG-031 extension endpoints;
 * the contract paths of openapi/appointment-service.yaml are all served by
 * AppointmentController / DoctorScheduleController). The billing/dispense
 * operations exist so payments/pharmacy slices (and ops tooling) can drive the
 * legacy VisitStatus 11/12 advance guards against THIS aggregate until those
 * domains consume the events directly.
 */
@RestController
@RequestMapping("/api/v1/encounters")
@Tag(name = "Encounters (MIG-031)",
        description = "Encounter lifecycle: check-in creation via appointments, VisitStatus 11/12 "
                + "split-brain resolution, daily closing job, queue/kiosk compatible token fields")
public class EncounterController {

    private final EncounterService service;

    public EncounterController(EncounterService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an encounter", description = "Includes effectiveLegacyVisitStatus — the resolved "
            + "VisitStatus across the appointment and encounter sides of the visit (split-brain read parity).")
    public Encounter get(@PathVariable long id) {
        return service.get(id);
    }

    /** Legacy addBillPayment parity: advances VisitStatus to 11 only if <11 (BR-OPD-007). */
    @PostMapping("/{id}/billing-paid")
    @Operation(summary = "Mark bill paid (legacy VisitStatus 11 write, guard: only advances if <11)")
    public Encounter billingPaid(@PathVariable long id) {
        return service.markBilled(id);
    }

    /** Legacy dispenseItemSale parity: 11 -> 12 only when no undispensed items remain (BR-PHAR-001). */
    @PostMapping("/{id}/dispensed")
    @Operation(summary = "Mark pharmacy fully dispensed (legacy VisitStatus 12 write; blocked while undispensed items remain)")
    public Encounter dispensed(@PathVariable long id,
                               @RequestParam(required = false, defaultValue = "false") boolean pendingItems) {
        return service.markDispensed(id, pendingItems);
    }
}
