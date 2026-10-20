package com.hims.interop.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * FHIR R4 read surface (CONTRACT-ONLY STUB — see V4__fhir_eausadhi.sql header).
 *
 * These endpoints return the FHIR R4 JSON shape (Patient / Observation / Bundle)
 * so consumers can integrate against the contract while the production HAPI FHIR
 * server (xcg/genserv/FhirServer.java in the monolith — 07 §ABDM evidence) is
 * wired in via the same Kafka consumer that drives InteropEventService.
 *
 * OUT OF SCOPE (comment-only): the actual HAPI FHIR R4 server, persistence of
 * FHIR resources, and the full FHIR search/validation pipeline. The FhirR4Converter
 * (application layer) IS the durable side of the HL7&lt;-&gt;FHIR bridge; these
 * endpoints are the contract disclosure surface only.
 */
@RestController
@RequestMapping("/api/v1/interop/fhir")
@Tag(name = "FHIR R4 (contract-only stub)", description = "FHIR R4 read surface — Patient / Observation / Bundle. CONTRACT-ONLY STUB: returns the FHIR R4 JSON shape so consumers can integrate against the contract while the production HAPI FHIR server is wired in. No persistence, no HAPI FHIR server — comment-only disclosure per register.")
public class FhirController {

    /**
     * Read a FHIR Patient resource by id.
     * CONTRACT-ONLY STUB: returns a minimal FHIR R4 Patient JSON shape.
     */
    @GetMapping("/Patient/{id}")
    @Operation(summary = "[contract-only stub] Read a FHIR Patient by id",
            description = "Returns a minimal FHIR R4 Patient JSON shape. CONTRACT-ONLY STUB — no HAPI FHIR server, no persistence. Production code wires the embedded HAPI FHIR R4 server (xcg/genserv/FhirServer.java in the monolith — 07 §ABDM evidence) into the same Kafka consumer that drives InteropEventService.")
    public ResponseEntity<Map<String, Object>> readPatient(@PathVariable String id) {
        return ResponseEntity.ok(Map.of(
                "resourceType", "Patient",
                "id", id,
                "meta", Map.of(
                        "source", "ihms-interop-service",
                        "profile", List.of("urn:ihms:fhir:patient:contract-stub")),
                "identifier", List.of(Map.of(
                        "system", "urn:ihms:patient:mrn",
                        "value", id)),
                "name", List.of(Map.of(
                        "use", "official",
                        "family", "CONTRACT-STUB",
                        "given", List.of("Patient-" + id))),
                "gender", "unknown",
                "birthDate", "1900-01-01",
                "_gender", Map.of("extension", List.of(Map.of(
                        "url", "http://hl7.org/fhir/StructureDefinition/data-absent-reason",
                        "valueCode", "unknown"))),
                "text", Map.of(
                        "status", "generated",
                        "div", "<div xmlns=\"http://www.w3.org/1999/xhtml\">CONTRACT-STUB Patient/" + id + "</div>")
        ));
    }

    /**
     * Read a FHIR Observation resource by id.
     * CONTRACT-ONLY STUB: returns a minimal FHIR R4 Observation JSON shape.
     */
    @GetMapping("/Observation/{id}")
    @Operation(summary = "[contract-only stub] Read a FHIR Observation by id",
            description = "Returns a minimal FHIR R4 Observation JSON shape. CONTRACT-ONLY STUB — no HAPI FHIR server, no persistence. Production code wires the embedded HAPI FHIR R4 server into the same Kafka consumer that drives InteropEventService.")
    public ResponseEntity<Map<String, Object>> readObservation(@PathVariable String id) {
        return ResponseEntity.ok(Map.of(
                "resourceType", "Observation",
                "id", id,
                "meta", Map.of(
                        "source", "ihms-interop-service",
                        "profile", List.of("urn:ihms:fhir:observation:contract-stub")),
                "status", "final",
                "code", Map.of(
                        "coding", List.of(Map.of(
                                "system", "http://loinc.org",
                                "code", "UNKNOWN",
                                "display", "CONTRACT-STUB Observation"))),
                "subject", Map.of("reference", "Patient/UNKNOWN"),
                "valueString", "CONTRACT-STUB",
                "text", Map.of(
                        "status", "generated",
                        "div", "<div xmlns=\"http://www.w3.org/1999/xhtml\">CONTRACT-STUB Observation/" + id + "</div>")
        ));
    }

    /**
     * Search FHIR Patient resources (minimal Bundle response).
     * CONTRACT-ONLY STUB: returns an empty search-set Bundle.
     */
    @GetMapping("/Patient")
    @Operation(summary = "[contract-only stub] Search FHIR Patient resources",
            description = "Returns an empty FHIR R4 search-set Bundle. CONTRACT-ONLY STUB — no HAPI FHIR server, no persistence. Production code wires the embedded HAPI FHIR R4 server into the same Kafka consumer that drives InteropEventService.")
    public ResponseEntity<Map<String, Object>> searchPatient() {
        return ResponseEntity.ok(Map.of(
                "resourceType", "Bundle",
                "type", "searchset",
                "total", 0,
                "meta", Map.of(
                        "source", "ihms-interop-service",
                        "profile", List.of("urn:ihms:fhir:searchset:contract-stub")),
                "entry", List.of(),
                "text", Map.of(
                        "status", "generated",
                        "div", "<div xmlns=\"http://www.w3.org/1999/xhtml\">CONTRACT-STUB search-set (empty)</div>")
        ));
    }
}
