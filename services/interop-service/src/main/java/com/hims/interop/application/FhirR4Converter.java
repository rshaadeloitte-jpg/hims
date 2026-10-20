package com.hims.interop.application;

import com.hims.interop.application.Hl7V2Parser.Hl7Message;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * HL7 v2 &lt;-&gt; HAPI FHIR R4 minimal resource converter (MIG-055). Pinned to
 * the Patient/Encounter subset we actually need at this slice:
 *   - HL7 ADT (PID + EVN + PV1) -&gt; FHIR Patient + Encounter (Bundle)
 *   - FHIR Patient (R4 JSON shape) -&gt; HL7 PID segment (round-trip support)
 *
 * The converter is INTENTIONALLY MINIMAL — it produces plain LinkedHashMap
 * representations of FHIR resources (and parses FHIR JSON to the same shape)
 * so the service can stay HAPI-free. Production code that needs full
 * HAPI FHIR (validation, parsing, resource handles) wires the embedded
 * HAPI FHIR R4 server (xcg/genserv/FhirServer.java in the monolith —
 * 07 §ABDM evidence) into the same Kafka consumer that drives
 * InteropEventService. The converter IS the durable side of the bridge.
 *
 * Pure function: no Spring, no persistence; unit-tested in isolation.
 */
public final class FhirR4Converter {

    private FhirR4Converter() {}

    /**
     * Convert a parsed HL7 ADT message into a FHIR Bundle containing a Patient
     * and (if PV1 is present) an Encounter. The returned Map is the JSON-ready
     * structure of a Bundle.resource[] = [Patient, Encounter].
     */
    public static Map<String, Object> adtToFhirBundle(Hl7Message m) {
        if (!"ADT".equals(m.messageType())) {
            throw new IllegalArgumentException("adtToFhirBundle only supports ADT, got " + m.messageType());
        }
        Map<String, Object> patient = hl7PidToFhirPatient(m.pid());
        Map<String, Object> encounter = hl7Pv1ToFhirEncounter(m.pv1(), m.messageType() + "^" + m.triggerEvent());

        List<Map<String, Object>> entries = new ArrayList<>();
        entries.add(bundleEntry(patient));
        if (encounter != null) {
            entries.add(bundleEntry(encounter));
        }

        Map<String, Object> bundle = new LinkedHashMap<>();
        bundle.put("resourceType", "Bundle");
        bundle.put("type", "collection");
        bundle.put("meta", Map.of(
                "source", "ihms-interop-service",
                "profile", bundleProfile(m)));
        bundle.put("entry", entries);
        return bundle;
    }

    /**
     * Convert a FHIR Patient JSON (Map) into an HL7 PID segment string. Inverse
     * of {@link #hl7PidToFhirPatient}. Field separator defaults to '|'.
     */
    public static String fhirPatientToHl7Pid(Map<String, Object> fhirPatient, char fieldSep) {
        if (!"Patient".equals(fhirPatient.get("resourceType"))) {
            throw new IllegalArgumentException("expected resourceType=Patient, got " + fhirPatient.get("resourceType"));
        }
        // PID fields (subset): PID-3 (MRN), PID-5 (family), PID-5.1 (given),
        // PID-7 (DOB), PID-8 (gender). We emit a minimal but valid PID.
        String mrn = stringFromFhirIdentifier(fhirPatient, "mrn");
        List<?> name = (List<?>) fhirPatient.getOrDefault("name", List.of());
        String family = "", given = "";
        if (!name.isEmpty()) {
            Map<String, Object> first = (Map<String, Object>) name.get(0);
            family = String.valueOf(first.getOrDefault("family", ""));
            given = String.valueOf(first.getOrDefault("given", List.of())).replaceAll("^\\[|\\]$", "");
        }
        String dob = String.valueOf(fhirPatient.getOrDefault("birthDate", ""));
        // FHIR gender -> HL7 PID-8 single char (M/F/O/U) per the reverse of
        // hl7PidToFhirPatient. We use startsWith to handle the few FHIR systems
        // that emit "male" vs "Male" (case-insensitive).
        String fhirGender = String.valueOf(fhirPatient.getOrDefault("gender", "")).toLowerCase();
        String hl7Gender = "";
        if (!fhirGender.isBlank()) {
            hl7Gender = switch (fhirGender.charAt(0)) {
                case 'm' -> "M";
                case 'f' -> "F";
                case 'o' -> "O";
                default -> "U";
            };
        }
        // PID|<set-id>|<patient-id>|<MRN>|<alternate-id>|<family-name>|<given-name>|<DOB>|<gender>|...
        return "PID" + fieldSep + "1" + fieldSep + fieldSep + mrn + fieldSep + fieldSep
                + family + fieldSep + given + fieldSep + dob + fieldSep + hl7Gender;
    }

    // ------------------------------------------------------------------ internals

    private static Map<String, Object> hl7PidToFhirPatient(Map<String, String> pid) {
        Map<String, Object> patient = new LinkedHashMap<>();
        patient.put("resourceType", "Patient");
        // PID-3 = MRN (4th token in our split: "0"=PID, "1"=setId, "2"=patientId, "3"=MRN, "4"=altId, "5"=family)
        String mrn = pid.getOrDefault("3", "");
        if (!mrn.isBlank()) {
            patient.put("identifier", List.of(Map.of(
                    "system", "urn:ihms:patient:mrn",
                    "value", mrn)));
        }
        // PID-5 = family name (component 1), PID-5.1 = given name (in HL7 the family
        // name comes first, given name second, separated by component separator).
        // Our split preserves the raw "5" field; we re-split on the component sep.
        String nameField = pid.getOrDefault("5", "");
        String family = "", given = "";
        if (!nameField.isBlank()) {
            String[] parts = nameField.split("\\^", -1);
            if (parts.length > 0) family = parts[0];
            if (parts.length > 1) given = parts[1];
        }
        List<Map<String, Object>> names = new ArrayList<>();
        Map<String, Object> name = new LinkedHashMap<>();
        if (!family.isBlank()) name.put("family", family);
        if (!given.isBlank()) {
            // HL7 PID-5 uses ^ as the family/given separator; the given portion
            // may itself be "FIRST^MIDDLE" (HL7 v2.5) — we re-split and put
            // them in the FHIR `given` array.
            name.put("given", List.of(given.split("\\^", -1)));
        }
        if (!name.isEmpty()) {
            name.put("use", "official");
            names.add(name);
        }
        if (!names.isEmpty()) patient.put("name", names);
        // PID-7 = DOB (YYYYMMDD -&gt; YYYY-MM-DD)
        String dob = pid.getOrDefault("7", "");
        if (!dob.isBlank() && dob.length() >= 8) {
            patient.put("birthDate", dob.substring(0, 4) + "-" + dob.substring(4, 6) + "-" + dob.substring(6, 8));
        }
        // PID-8 = gender (M/F/O/U -&gt; male/female/other/unknown)
        String gender = pid.getOrDefault("8", "");
        if (!gender.isBlank()) {
            patient.put("gender", switch (gender.charAt(0)) {
                case 'M' -> "male";
                case 'F' -> "female";
                case 'O' -> "other";
                default -> "unknown";
            });
        }
        return patient;
    }

    private static Map<String, Object> hl7Pv1ToFhirEncounter(Map<String, String> pv1, String typeDisplay) {
        if (pv1.isEmpty()) {
            return null;
        }
        Map<String, Object> enc = new LinkedHashMap<>();
        enc.put("resourceType", "Encounter");
        enc.put("status", "finished");   // legacy HL7 ADT-? -&gt; Encounter default
        // PV1-2 = patient class (I=inpatient, O=outpatient, E=emergency)
        String patientClass = pv1.getOrDefault("2", "");
        enc.put("class", Map.of(
                "system", "http://terminology.hl7.org/CodeSystem/v3-ActCode",
                "code", patientClass.isBlank() ? "AMB" : patientClass,
                "display", typeDisplay));
        // PV1-7 = attending doctor (in raw form; we just pass through the value
        // as a reference placeholder; production code would resolve to a FHIR
        // Practitioner via a downstream consumer).
        String attending = pv1.getOrDefault("7", "");
        if (!attending.isBlank()) {
            enc.put("participant", List.of(Map.of(
                    "individual", Map.of(
                            "reference", "Practitioner/" + stripComponents(attending),
                            "display", stripComponents(attending)))));
        }
        return enc;
    }

    private static Map<String, Object> bundleEntry(Map<String, Object> resource) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("fullUrl", "urn:ihms:" + resource.get("resourceType") + "/" + stripComponents(String.valueOf(resource.get("id"))));
        entry.put("resource", resource);
        return entry;
    }

    private static String stripComponents(String raw) {
        if (raw == null) return "";
        int caret = raw.indexOf('^');
        return caret < 0 ? raw : raw.substring(0, caret);
    }

    private static String stringFromFhirIdentifier(Map<String, Object> fhirPatient, String code) {
        Object ids = fhirPatient.get("identifier");
        if (!(ids instanceof List<?> list) || list.isEmpty()) return "";
        Map<String, Object> first = (Map<String, Object>) list.get(0);
        return String.valueOf(first.getOrDefault("value", ""));
    }

    private static String bundleProfile(Hl7Message m) {
        // The bundle.profile carries the cross-walk identity: ADT message type
        // mapped to a structure-definition-style URL. Production code would
        // resolve this against the HAPI FHIR server's StructureDefinition.
        return "urn:ihms:fhir:adt:" + m.triggerEvent().toLowerCase();
    }
}
