package com.hims.forms.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.forms.application.exceptions.FormNotFoundException;
import com.hims.forms.application.exceptions.InvalidFormRequestException;
import com.hims.forms.domain.model.FormDefinition;
import com.hims.forms.domain.model.FormSubmission;
import com.hims.forms.domain.repository.FormDefinitionRepository;
import com.hims.forms.domain.repository.FormSubmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Forms engine service (M40, ported from M_NOTETITLEMASTER + TIU XML JSON-schema engine).
 * ponytail: stores JSON Schema as JSONB text; submission validates minimal (non-empty + parseable JSON)
 * — full JSON Schema validation deferred until frontend renderer contract stabilizes.
 */
@Service
public class FormService {

    private static final Logger log = LoggerFactory.getLogger(FormService.class);
    public static final long AUDIT_ACTOR_ID = 1L;

    private final FormDefinitionRepository formRepository;
    private final FormSubmissionRepository submissionRepository;
    private final ObjectMapper objectMapper;

    public FormService(FormDefinitionRepository formRepository,
                       FormSubmissionRepository submissionRepository,
                       ObjectMapper objectMapper) {
        this.formRepository = formRepository;
        this.submissionRepository = submissionRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<FormDefinition> listForms() {
        return formRepository.findAllActive();
    }

    @Transactional(readOnly = true)
    public List<FormDefinition> listFormsBySpecialty(String specialty) {
        if (specialty == null || specialty.isBlank()) {
            return formRepository.findAllActive();
        }
        return formRepository.findAllActiveBySpecialty(specialty.toUpperCase());
    }

    @Transactional(readOnly = true)
    public List<String> listSpecialties() {
        return formRepository.findDistinctSpecialties();
    }

    @Transactional(readOnly = true)
    public FormDefinition getSchema(long id) {
        return formRepository.findById(id)
                .filter(f -> Boolean.TRUE.equals(f.getStatus()))
                .orElseThrow(() -> new FormNotFoundException("NOT_FOUND", "Form not found: " + id));
    }

    @Transactional(readOnly = true)
    public FormDefinition getSchemaByCode(String code) {
        return formRepository.findByCode(code)
                .filter(f -> Boolean.TRUE.equals(f.getStatus()))
                .orElseThrow(() -> new FormNotFoundException("NOT_FOUND", "Form not found for code: " + code));
    }

    @Transactional
    public FormSubmission submit(long formId, Long patientId, Long encounterId, Object submissionPayload, Long submittedBy) {
        FormDefinition form = getSchema(formId);

        if (submissionPayload == null) {
            throw new InvalidFormRequestException("FORM_REQ", "submission payload is required");
        }

        String submissionJson;
        try {
            // normalize payload to JSON string (covers Map and JsonNode inputs from controller)
            if (submissionPayload instanceof String s) {
                // validate it's JSON
                objectMapper.readTree(s);
                submissionJson = s;
            } else {
                submissionJson = objectMapper.writeValueAsString(submissionPayload);
            }
            // ensure it's a non-empty object/array
            JsonNode node = objectMapper.readTree(submissionJson);
            if (node.isNull() || (node.isObject() && node.isEmpty())) {
                throw new InvalidFormRequestException("FORM_REQ", "submission payload must be a non-empty JSON object");
            }
        } catch (JsonProcessingException e) {
            throw new InvalidFormRequestException("FORM_REQ", "submission payload is not valid JSON: " + e.getOriginalMessage());
        }

        // ponytail: minimal required-field check via schema_json required array (best-effort, not full draft-07)
        validateRequiredFields(form.getSchemaJson(), submissionJson);

        FormSubmission sub = new FormSubmission();
        sub.setFormId(form.getId());
        sub.setPatientId(patientId);
        sub.setEncounterId(encounterId);
        sub.setSubmissionJson(submissionJson);
        sub.setSubmittedBy(submittedBy != null ? submittedBy : AUDIT_ACTOR_ID);
        sub.setSubmittedAt(Timestamp.from(Instant.now()));
        sub.setStatus(true);
        sub.setAddedDatetime(Timestamp.from(Instant.now()));
        sub = submissionRepository.save(sub);
        log.info("Form submit formId={} code={} submissionId={} patient={} encounter={}",
                formId, form.getCode(), sub.getId(), patientId, encounterId);
        return sub;
    }

    @Transactional(readOnly = true)
    public List<FormSubmission> listSubmissions(long formId) {
        getSchema(formId); // 404 if form missing
        return submissionRepository.findByFormId(formId);
    }

    @Transactional(readOnly = true)
    public List<FormSubmission> listSubmissionsByPatient(long patientId) {
        return submissionRepository.findByPatientIdOrderBySubmittedAtDesc(patientId);
    }

    @Transactional(readOnly = true)
    public List<FormSubmission> listSubmissionsByEncounter(long encounterId) {
        return submissionRepository.findByEncounterId(encounterId);
    }

    @Transactional(readOnly = true)
    public FormSubmission getSubmission(long formId, long submissionId) {
        return submissionRepository.findByIdAndFormId(submissionId, formId)
                .orElseThrow(() -> new FormNotFoundException("NOT_FOUND",
                        "Submission " + submissionId + " not found for form " + formId));
    }

    @Transactional
    public FormSubmission softDeleteSubmission(long formId, long submissionId) {
        FormSubmission sub = getSubmission(formId, submissionId);
        sub.setStatus(false);
        sub = submissionRepository.save(sub);
        log.info("Form submission soft-deleted formId={} submissionId={}", formId, submissionId);
        return sub;
    }

    // Lightweight required-field validation: extracts schema required[] and checks keys exist in payload.
    // Full JSON Schema validation (e.g. networknt) deferred until form library stabilizes.
    private void validateRequiredFields(String schemaJson, String submissionJson) {
        try {
            JsonNode schema = objectMapper.readTree(schemaJson);
            JsonNode required = schema.get("required");
            if (required == null || !required.isArray() || required.isEmpty()) return;
            JsonNode payload = objectMapper.readTree(submissionJson);
            for (JsonNode field : required) {
                String key = field.asText();
                if (!payload.has(key) || payload.get(key).isNull()
                        || (payload.get(key).isTextual() && payload.get(key).asText().isBlank())) {
                    throw new InvalidFormRequestException("FORM_VALIDATION", "Missing required field: " + key);
                }
            }
        } catch (JsonProcessingException e) {
            // schema itself invalid — log and skip validation (don't block submit on bad schema seed)
            log.warn("Schema parse failed, skipping required check: {}", e.getMessage());
        }
    }
}
