package com.hims.pmrd.api;

import com.hims.pmrd.application.PMRDService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/pmrd")
public class PMRDController {
    private final PMRDService pmrdService;

    public PMRDController(PMRDService pmrdService) { this.pmrdService = pmrdService; }

    @GetMapping("/encounters/{patientId}")
    public ResponseEntity<Map<String, Object>> getEncounters(@PathVariable Long patientId) {
        return ResponseEntity.ok(pmrdService.getAllPatientEncounter(patientId));
    }

    @GetMapping("/documents/{patientId}")
    public ResponseEntity<?> getDocuments(@PathVariable Long patientId) {
        return ResponseEntity.ok(pmrdService.getPatientDocumentsForPMRD(patientId));
    }

    @GetMapping("/jsy-forms/{patientId}")
    public ResponseEntity<?> getJsy(@PathVariable Long patientId, @RequestParam String formType) {
        return ResponseEntity.ok(pmrdService.getJsyFormDetails(patientId, formType));
    }
}
