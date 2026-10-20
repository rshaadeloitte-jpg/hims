package com.hims.pmrd.application;

import com.hims.pmrd.domain.model.*;
import com.hims.pmrd.repository.*;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class PMRDService {
    private final PMRDEncounterRepository encounterRepo;
    private final PatientDocumentRepository docRepo;
    private final JsyFormDataRepository jsyRepo;

    public PMRDService(PMRDEncounterRepository encounterRepo, PatientDocumentRepository docRepo, JsyFormDataRepository jsyRepo) {
        this.encounterRepo = encounterRepo;
        this.docRepo = docRepo;
        this.jsyRepo = jsyRepo;
    }

    public Map<String, Object> getAllPatientEncounter(Long patientId) {
        List<PMRDEncounter> all = encounterRepo.findByPatientId(patientId);
        Map<String, List<PMRDEncounter>> grouped = new HashMap<>();
        grouped.put("OP", new ArrayList<>());
        grouped.put("IP", new ArrayList<>());
        for (PMRDEncounter e : all) {
            if ("OP".equals(e.getType())) grouped.get("OP").add(e);
            else if ("IP".equals(e.getType())) grouped.get("IP").add(e);
        }
        Map<String, Object> result = new HashMap<>(grouped);
        result.put("PATIENT DOCUMENTS", docRepo.findByPatientId(patientId));
        return result;
    }

    public List<PatientDocument> getPatientDocumentsForPMRD(Long patientId) {
        return docRepo.findByPatientId(patientId);
    }

    public List<JsyFormData> getJsyFormDetails(Long patientId, String formType) {
        return jsyRepo.findByPatientIdAndFormType(patientId, formType);
    }
}
