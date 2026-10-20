package com.hims.interop.application;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * L0 — HL7 v2 <-> HAPI FHIR R4 minimal converter (MIG-055). Smoke tests that
 * do not require complex HL7 fixtures: round-trip the gender map and verify
 * the bundle assembly path with a hand-built Hl7Message.
 */
class FhirR4ConverterTest {

    @Test
    void fhirPatientToHl7Pid_genderMapping_roundTrip() {
        // FHIR gender -> HL7 PID-8 single char.
        for (String[] pair : new String[][]{{"male", "M"}, {"female", "F"}, {"other", "O"}, {"unknown", "U"}}) {
            String fhirGender = pair[0];
            String hl7Char = pair[1];
            Map<String, Object> patient = Map.of(
                    "resourceType", "Patient",
                    "identifier", List.of(Map.of("value", "X")),
                    "gender", fhirGender);
            String pid = FhirR4Converter.fhirPatientToHl7Pid(patient, '|');
            assertThat(pid).as("%s -> %s", fhirGender, hl7Char).endsWith(hl7Char);
        }
    }

    @Test
    void fhirPatientToHl7Pid_startsWithPidMarker() {
        Map<String, Object> patient = Map.of(
                "resourceType", "Patient",
                "identifier", List.of(Map.of("value", "MRN-9")));
        String pid = FhirR4Converter.fhirPatientToHl7Pid(patient, '|');
        // PID|1||MRN-9|| (set-id=1, patient-id empty, MRN=MRN-9, alt-id empty, family empty, ...)
        assertThat(pid).startsWith("PID|1||MRN-9|");
    }

    @Test
    void fhirPatientToHl7Pid_rejectsNonPatientResource() {
        Map<String, Object> notPatient = Map.of("resourceType", "Encounter");
        assertThatThrownBy(() -> FhirR4Converter.fhirPatientToHl7Pid(notPatient, '|'))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expected resourceType=Patient");
    }

    @Test
    void adtToFhirBundle_rejectsNonAdtMessage() {
        // Build a minimal ORM message and verify the converter rejects it.
        Hl7V2Parser.Hl7Message m = new Hl7V2Parser.Hl7Message(
                "ORM", "O01",
                "MSG-1", "GHH", "ELABs", "RIH", "src",
                "20240815120000", "P",
                new java.util.LinkedHashMap<>(), new java.util.LinkedHashMap<>(), new java.util.LinkedHashMap<>(),
                new java.util.LinkedHashMap<>(), new java.util.LinkedHashMap<>(), new java.util.LinkedHashMap<>(),
                java.util.List.of("MSH|^~\\&|GHH|ELABs|RIH|src|20240815120000||ORM^O01|MSG-1|P|2.5"),
                '|', '^', '~', '\\');
        assertThatThrownBy(() -> FhirR4Converter.adtToFhirBundle(m))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("only supports ADT");
    }

    @Test
    void adtToFhirBundle_patientOnlyWhenPv1Empty_buildsValidBundle() {
        // Build a minimal ADT message with PID but no PV1.
        java.util.Map<String, String> pid = new java.util.LinkedHashMap<>();
        pid.put("0", "PID");
        pid.put("3", "MRN-1");
        pid.put("5", "DOE^JOHN");
        pid.put("7", "19800101");
        pid.put("8", "M");
        Hl7V2Parser.Hl7Message m = new Hl7V2Parser.Hl7Message(
                "ADT", "A01",
                "MSG-1", "GHH", "ELABs", "RIH", "src",
                "20240815120000", "P",
                new java.util.LinkedHashMap<>(), pid, new java.util.LinkedHashMap<>(),
                new java.util.LinkedHashMap<>(), new java.util.LinkedHashMap<>(), new java.util.LinkedHashMap<>(),
                java.util.List.of("MSH|^~\\&|GHH|ELABs|RIH|src|20240815120000||ADT^A01|MSG-1|P|2.5"),
                '|', '^', '~', '\\');
        Map<String, Object> bundle = FhirR4Converter.adtToFhirBundle(m);
        assertThat(bundle.get("resourceType")).isEqualTo("Bundle");
        List<Map<String, Object>> entries = (List<Map<String, Object>>) bundle.get("entry");
        // only Patient, no Encounter (PV1 was empty)
        assertThat(entries).hasSize(1);
        Map<String, Object> patient = (Map<String, Object>) entries.get(0).get("resource");
        assertThat(patient.get("resourceType")).isEqualTo("Patient");
        assertThat(patient.get("birthDate")).isEqualTo("1980-01-01");
        assertThat(patient.get("gender")).isEqualTo("male");
    }
}
