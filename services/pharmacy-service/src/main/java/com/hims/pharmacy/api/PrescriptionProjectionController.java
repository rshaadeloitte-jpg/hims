package com.hims.pharmacy.api;

import com.hims.pharmacy.application.PrescriptionDispenseService;
import com.hims.pharmacy.domain.model.PrescriptionDispense;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * EMR-number stamping on the pharmacy-local prescription dispensing projection
 * (register WP-R2 API: PUT /api/v1/prescriptions/{id}/emr-no). Unknown
 * prescription -&gt; 404 envelope.
 */
@RestController
@Tag(name = "Prescription Projection (WP-R2)", description = "Pharmacy-local T_PRESCRIPTIONDISPENSE projection (ordering-service keeps truth via E-06): EMR number stamping")
public class PrescriptionProjectionController {

    private final PrescriptionDispenseService service;

    public PrescriptionProjectionController(PrescriptionDispenseService service) {
        this.service = service;
    }

    @PutMapping("/api/v1/prescriptions/{id}/emr-no")
    @Operation(summary = "Stamp the EMR number on a prescription's pharmacy dispensing record",
            description = "emrNo required; unknown prescription -> 404 NOT_FOUND envelope.")
    public PrescriptionDispense setEmrNo(@PathVariable long id,
                                         @RequestBody Map<String, Object> body) {
        Object emrNo = body.get("emrNo");
        return service.setEmrNo(id, emrNo == null ? null : String.valueOf(emrNo));
    }
}
