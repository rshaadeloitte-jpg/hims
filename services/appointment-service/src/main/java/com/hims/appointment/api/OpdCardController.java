package com.hims.appointment.api;

import com.hims.appointment.application.OpdCardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * OPD card print API (M02 gap).
 * Generates HTML representation of OPD card for printing.
 * Full Velocity template parity (201 .vm templates) deferred.
 */
@RestController
@RequestMapping("/api/v1/appointments")
@Tag(name = "OPD Card (M02)",
        description = "OPD card print service. Generates HTML representation for printing. "
                + "Full Velocity template parity deferred.")
public class OpdCardController {

    private final OpdCardService opdCardService;

    public OpdCardController(OpdCardService opdCardService) {
        this.opdCardService = opdCardService;
    }

    @GetMapping(value = "/{id}/opd-card", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get OPD card data as JSON")
    public Map<String, Object> getOpdCardData(@PathVariable("id") long appointmentId) {
        return opdCardService.generateOpdCard(appointmentId);
    }

    @GetMapping(value = "/{id}/opd-card/html", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "Get OPD card as HTML for printing")
    public String getOpdCardHtml(@PathVariable("id") long appointmentId) {
        return opdCardService.generateOpdCardHtml(appointmentId);
    }
}
