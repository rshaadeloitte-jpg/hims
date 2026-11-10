package com.hims.govt.api;

import com.hims.govt.application.BeneficiaryService;
import com.hims.govt.domain.model.BeneficiaryEnrolment;
import com.hims.govt.domain.model.ReferenceTid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Chiranjeevi beneficiary API (GAP-REGISTER-6 WP-T4, Catalog #28): enrol
 * (409 dup enrolmentNo) + TID linkage (404 unknown enrolmentNo; 409 dup tid).
 * Emits NOTHING to the outbox — external Chiranjeevi-Yojana pushes are
 * OUT OF SCOPE (comment-only).
 */
@RestController
@RequestMapping("/api/v1/beneficiaries")
@Tag(name = "Beneficiaries (WP-T4)", description = "Chiranjeevi enrolment + TID linkage (Catalog #28): scheme pinned to CHIRANJEEVI; dup enrolmentNo/tid -> 409. No events — PCTS/Chiranjeevi-Yojana pushes OUT OF SCOPE.")
public class BeneficiaryController {

    private final BeneficiaryService service;

    public BeneficiaryController(BeneficiaryService service) {
        this.service = service;
    }

    @PostMapping("/enrol")
    @Operation(summary = "Enrol a patient into the Chiranjeevi scheme",
            description = "{patientId, scheme, enrolmentNo, enrolledOn}; scheme must be CHIRANJEEVI; "
                    + "duplicate enrolmentNo -> 409 GOVT_DUPLICATE_ENROLMENT_NO. Emits nothing.")
    public ResponseEntity<BeneficiaryEnrolment> enrol(@RequestBody EnrolRequest body) {
        BeneficiaryEnrolment created =
                service.enrol(body.patientId(), body.scheme(), body.enrolmentNo(), body.enrolledOn());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{enrolmentNo}/tid")
    @Operation(summary = "Link a scheme-side TID to an enrolment",
            description = "{tid}; unknown enrolmentNo -> 404; duplicate tid -> 409 GOVT_DUPLICATE_TID. Emits nothing.")
    public ResponseEntity<ReferenceTid> linkTid(@PathVariable String enrolmentNo,
                                                @RequestBody LinkTidRequest body) {
        ReferenceTid linked = service.linkTid(enrolmentNo, body.tid());
        return ResponseEntity.status(HttpStatus.CREATED).body(linked);
    }

    public record EnrolRequest(Long patientId, String scheme, String enrolmentNo, LocalDate enrolledOn) {}

    public record LinkTidRequest(String tid) {}
}
