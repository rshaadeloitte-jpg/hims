package com.hims.patient.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.hims.patient.application.PatientRegistrationService;
import com.hims.patient.application.exceptions.InvalidPatientRequestException;
import com.hims.patient.domain.model.PatientAadhaarVerification;
import com.hims.patient.domain.model.PatientDocument;
import com.hims.patient.domain.model.PatientMlc;
import com.hims.patient.domain.model.PatientRegistration;
import com.hims.patient.domain.model.PatientRelation;
import com.hims.patient.domain.model.PatientSocialAssessment;
import com.hims.patient.domain.model.PatientSponsor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Patient registration API (MIG-030 / S01 kickoff). Emits PatientRegistered /
 * PatientUpdated via the transactional outbox to ihms.patient.registration.
 */
@RestController
@RequestMapping("/api/v1/patients")
@Tag(name = "Patient Registration (MIG-030)",
        description = "Registration with BR-PAT-001 MRN generation + BR-PAT-002 duplicate matrix, demographics update, document linkage")
public class PatientController {

    private final PatientRegistrationService service;

    public PatientController(PatientRegistrationService service) {
        this.service = service;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a patient",
            description = "Server assigns patientId (patient_reg_seq) and derives the permanent MRN "
                    + "(ddMMYY + 9-digit id, BR-PAT-001) — mrno in the payload is rejected as server-owned. "
                    + "Duplicate matrix (BR-PAT-002): mobile+name+dob / Aadhaar/VID / Janaadhaar hit → BERA011; "
                    + "ABHA healthIdNumber → BEXA040 with existing MRN. Emits PatientRegistered.")
    public ResponseEntity<PatientRegistration> register(@RequestBody(required = false) JsonNode body,
                                                        @RequestParam(required = false) Long unitId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.register(body, unitId));
    }

    @GetMapping("/{id}")
    public PatientRegistration get(@PathVariable long id) {
        return service.get(id);
    }

    @GetMapping("/search")
    @Operation(summary = "Search by identity", description = "healthIdNumber (exact) or aadhaar (exact) or mobileNo.")
    public List<PatientRegistration> search(@RequestParam(required = false) String mobile,
                                            @RequestParam(required = false) String aadhaar,
                                            @RequestParam(required = false) String healthIdNumber) {
        return service.search(mobile, aadhaar, healthIdNumber);
    }

    @GetMapping
    @Operation(summary = "List patients (M01 gap: COVID filter)",
            description = "GET /patients?covid=true returns patients with covid_status tracked; &covidStatus=POSITIVE narrows. Ported from CoronaStats.covidStatus.")
    public List<PatientRegistration> list(@RequestParam(required = false) Boolean covid,
                                          @RequestParam(required = false) String covidStatus) {
        if (Boolean.TRUE.equals(covid) || (covidStatus != null && !covidStatus.isBlank())) {
            return service.listCovidPatients(covidStatus);
        }
        return List.of();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update demographics",
            description = "NonNull fields only; id/mrno/unitId are server-owned and never copied. "
                    + "Re-runs the duplicate matrix when identity fields change. Emits PatientUpdated.")
    public PatientRegistration update(@PathVariable long id, @RequestBody(required = false) JsonNode body) {
        return service.update(id, body);
    }

    @PostMapping("/{id}/documents")
    @Operation(summary = "Link a document to the patient (metadata; blob storage MIG-021)")
    public ResponseEntity<PatientDocument> linkDocument(@PathVariable long id,
                                                        @RequestBody(required = false) JsonNode body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.linkDocument(id, body));
    }

    @GetMapping("/{id}/documents")
    public List<PatientDocument> documents(@PathVariable long id) {
        return service.documents(id);
    }

    @PostMapping("/{id}/sponsors")
    @Operation(summary = "Link a sponsor/scheme to the patient",
            description = "T_PATIENTSPONSOR clean target (RGHS/Chiranjeevi/insurer). sponsorType is required.")
    public ResponseEntity<PatientSponsor> linkSponsor(@PathVariable long id,
                                                      @RequestBody(required = false) JsonNode body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.linkSponsor(id, body));
    }

    @GetMapping("/{id}/sponsors")
    public List<PatientSponsor> sponsors(@PathVariable long id) {
        return service.sponsors(id);
    }

    // --------------------------------------------------------------------- MLC

    @PostMapping("/{id}/mlc")
    @Operation(summary = "Create an MLC record for the patient (M01 gap: MLC capture)",
            description = "Ported from old IHMS PatDetails.mlcCase/mlcType. mlcCase is required.")
    public ResponseEntity<PatientMlc> saveMlc(@PathVariable long id,
                                              @RequestBody(required = false) JsonNode body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.saveMlc(id, body));
    }

    @GetMapping("/{id}/mlc")
    @Operation(summary = "List MLC records for a patient")
    public List<PatientMlc> mlcRecords(@PathVariable long id) {
        return service.mlcRecords(id);
    }

    // --------------------------------------------------------------- Relations

    @PostMapping("/{id}/relations")
    @Operation(summary = "Link a patient relation (M01 gap: mother-child linking)",
            description = "Supported types: MOTHER, CHILD, SPOUSE, FATHER, GUARDIAN. relatedPatientId and relationType are required.")
    public ResponseEntity<PatientRelation> linkRelation(@PathVariable long id,
                                                        @RequestBody(required = false) JsonNode body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.linkRelation(id, body));
    }

    @GetMapping("/{id}/relations")
    @Operation(summary = "List patient relations")
    public List<PatientRelation> relations(@PathVariable long id) {
        return service.relations(id);
    }

    // -------------------------------------------------------------- M01 remaining

    @PostMapping("/{id}/aadhaar/otp")
    @Operation(summary = "Aadhaar OTP/biometric verify (M01 gap)",
            description = "Ported from IntegrationServiceImpl.aadhaarAuthenticate + biometricDetailsJson. "
                    + "12-digit Aadhaar validated (^[2-9][0-9]{11}$). "
                    + "OTP: generates 6-digit 5-min TTL (max 3 attempts); include {otp} to verify. "
                    + "BIOMETRIC: verificationType=BIOMETRIC + deviceId → immediate verified audit.")
    public ResponseEntity<PatientAadhaarVerification> aadhaarOtp(@PathVariable long id,
                                                                 @RequestBody(required = false) JsonNode body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.requestAadhaarOtp(id, body));
    }

    @GetMapping("/{id}/aadhaar/verifications")
    @Operation(summary = "List Aadhaar verifications for a patient")
    public List<PatientAadhaarVerification> aadhaarVerifications(@PathVariable long id) {
        return service.aadhaarVerifications(id);
    }

    @PostMapping("/{id}/covid-status")
    @Operation(summary = "Set COVID status (M01 gap)",
            description = "Ported from CoronaStats.covidStatus via PatientServiceHelper:4493. covidStatus required (POSITIVE/NEGATIVE/SUSPECTED/RECOVERED).")
    public PatientRegistration covidStatus(@PathVariable long id, @RequestBody(required = false) JsonNode body) {
        return service.updateCovidStatus(id, body);
    }

    @PostMapping("/{id}/social-worker-assessment")
    @Operation(summary = "Create Social Worker assessment (M01 gap: isSocialAssessment → Social Assesment)",
            description = "Ported from QuetionnaireServiceImpl isSocialAssessment flag. Body: {assessmentData:{...}, socialWorkerId, remarks}. Stored as JSONB.")
    public ResponseEntity<PatientSocialAssessment> socialAssessment(@PathVariable long id,
                                                                    @RequestBody(required = false) JsonNode body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createSocialAssessment(id, body));
    }

    @GetMapping("/{id}/social-worker-assessments")
    @Operation(summary = "List Social Worker assessments")
    public List<PatientSocialAssessment> socialAssessments(@PathVariable long id) {
        return service.socialAssessments(id);
    }

    // ------------------------------------------------------------------ Merge

    @PostMapping("/merge")
    @Operation(summary = "Merge a duplicate source patient into a target",
            description = "Re-points document and sponsor rows to the target, marks the source status=false, "
                    + "and emits PatientMerged via the transactional outbox (partition key = target patientId). "
                    + "sourceId == targetId is rejected; an already-inactive source is rejected with BEXA012.")
    public PatientMergeResult merge(@RequestBody(required = false) JsonNode body) {
        if (body == null || !body.hasNonNull("sourceId") || !body.hasNonNull("targetId")) {
            throw new InvalidPatientRequestException("BEXA003", "sourceId and targetId are required");
        }
        return service.merge(body.get("sourceId").asLong(), body.get("targetId").asLong());
    }
}
