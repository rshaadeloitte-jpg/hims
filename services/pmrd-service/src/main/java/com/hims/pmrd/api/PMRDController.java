package com.hims.pmrd.api;

import com.hims.pmrd.application.PMRDService;
import com.hims.pmrd.domain.model.JsyFormData;
import com.hims.pmrd.domain.model.PMRDEncounter;
import com.hims.pmrd.domain.model.PatientDocument;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/pmrd")
public class PMRDController {

    private final PMRDService service;

    public PMRDController(PMRDService service) {
        this.service = service;
    }

    // ---- Patient encounters ----

    @GetMapping("/patient-encounters")
    public List<PMRDEncounter> patientEncounters(@RequestParam(required = false) Long patientId,
                                                 @RequestParam(required = false) String encounterType,
                                                 @RequestParam(required = false) String fromDate,
                                                 @RequestParam(required = false) String toDate,
                                                 @RequestParam(required = false) Integer page,
                                                 @RequestParam(required = false) Integer size) {
        return service.getEncounters(patientId, encounterType, fromDate, toDate);
    }

    @GetMapping("/patient-encounters/{id}")
    public ResponseEntity<PMRDEncounter> patientEncounter(@PathVariable Long id) {
        return service.getEncounterById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ---- JSY forms ----

    @GetMapping("/jsy-forms")
    public List<JsyFormData> jsyForms(@RequestParam(required = false) String formType,
                                     @RequestParam(required = false) Long patientId) {
        return service.getJsyForms(formType, patientId);
    }

    @GetMapping("/jsy-forms/{id}")
    public ResponseEntity<JsyFormData> jsyForm(@PathVariable Long id) {
        return service.getJsyFormById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/jsy-forms")
    public ResponseEntity<JsyFormData> saveJsyForm(@RequestBody Map<String, Object> body) {
        JsyFormData saved = service.saveJsyForm(body);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ---- Documents ----

    @GetMapping("/documents")
    public List<PatientDocument> documents(@RequestParam(required = false) Long patientId) {
        if (patientId == null) return List.of();
        return service.getPatientDocumentsForPMRD(patientId);
    }

    @PostMapping("/documents")
    public ResponseEntity<PatientDocument> uploadDocument(@RequestBody PatientDocument doc) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.saveDocument(doc));
    }

    @DeleteMapping("/documents/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        service.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }
}