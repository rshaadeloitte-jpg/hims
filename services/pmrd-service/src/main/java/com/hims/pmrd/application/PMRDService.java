package com.hims.pmrd.application;

import com.hims.pmrd.domain.model.*;
import com.hims.pmrd.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PMRDService {
    private final PMRDEncounterRepository encounterRepo;
    private final PatientDocumentRepository docRepo;
    private final JsyFormDataRepository jsyRepo;
    private final ObjectMapper mapper = new ObjectMapper();

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

    public List<PMRDEncounter> getEncounters(Long patientId, String encounterType, String fromDate, String toDate) {
        List<PMRDEncounter> all = (patientId != null) ? encounterRepo.findByPatientId(patientId) : encounterRepo.findAll();
        if (encounterType != null && !encounterType.isBlank()) {
            all.removeIf(e -> !encounterType.equalsIgnoreCase(e.getType()));
        }
        return all;
    }

    public Optional<PMRDEncounter> getEncounterById(Long id) {
        return encounterRepo.findById(id);
    }

    public List<PatientDocument> getPatientDocumentsForPMRD(Long patientId) {
        return docRepo.findByPatientId(patientId);
    }

    public PatientDocument saveDocument(PatientDocument doc) {
        if (doc.getUploadDate() == null) doc.setUploadDate(LocalDateTime.now());
        return docRepo.save(doc);
    }

    public void deleteDocument(Long id) {
        docRepo.deleteById(id);
    }

    public Optional<PatientDocument> getDocumentById(Long id) {
        return docRepo.findById(id);
    }

    public List<JsyFormData> getJsyForms(String formType, Long patientId) {
        if (formType != null && !formType.isBlank() && patientId != null) {
            return jsyRepo.findByPatientIdAndFormType(patientId, formType);
        }
        if (formType != null && !formType.isBlank()) {
            return jsyRepo.findByFormType(formType);
        }
        if (patientId != null) {
            return jsyRepo.findByPatientId(patientId);
        }
        return jsyRepo.findAll();
    }

    public Optional<JsyFormData> getJsyFormById(Long id) {
        return jsyRepo.findById(id);
    }

    public JsyFormData saveJsyForm(Map<String, Object> payload) {
        JsyFormData f = new JsyFormData();
        Object ft = payload.get("formType");
        f.setFormType(ft == null ? null : String.valueOf(ft));
        Object pid = payload.get("patientId");
        if (pid != null) {
            if (pid instanceof Number) f.setPatientId(((Number) pid).longValue());
            else f.setPatientId(Long.parseLong(String.valueOf(pid)));
        }
        Object bn = payload.get("beneficiaryName");
        f.setBeneficiaryName(bn == null ? null : String.valueOf(bn));
        Object amt = payload.get("amount");
        if (amt != null) {
            f.setAmount(new BigDecimal(String.valueOf(amt)));
        }
        Object sd = payload.get("submittedDate");
        f.setSubmittedDate(sd == null ? null : String.valueOf(sd));
        f.setStatus("SUBMITTED");
        try {
            f.setFormData(mapper.writeValueAsString(payload));
        } catch (Exception e) {
            f.setFormData(String.valueOf(payload));
        }
        return jsyRepo.save(f);
    }
}