package com.hims.forms.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.forms.application.exceptions.FormNotFoundException;
import com.hims.forms.application.exceptions.InvalidFormRequestException;
import com.hims.forms.domain.model.FormDefinition;
import com.hims.forms.domain.model.FormSubmission;
import com.hims.forms.domain.repository.FormDefinitionRepository;
import com.hims.forms.domain.repository.FormSubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FormServiceTest {

    FormDefinitionRepository formRepo;
    FormSubmissionRepository subRepo;
    ObjectMapper om;
    FormService service;

    @BeforeEach
    void setUp() {
        formRepo = mock(FormDefinitionRepository.class);
        subRepo = mock(FormSubmissionRepository.class);
        om = new ObjectMapper();
        service = new FormService(formRepo, subRepo, om);
        when(subRepo.save(any())).thenAnswer(inv -> {
            var arg = (com.hims.forms.domain.model.FormSubmission) inv.getArgument(0);
            arg.setId(1L);
            return arg;
        });
    }

    private FormDefinition form(long id, String code, String specialty, String schemaJson) {
        FormDefinition f = new FormDefinition();
        f.setId(id);
        f.setCode(code);
        f.setName(code.replace("_", " "));
        f.setSpecialty(specialty);
        f.setSchemaJson(schemaJson);
        f.setVersion(1);
        f.setStatus(true);
        return f;
    }

    private FormDefinition form(long id, String schemaJson) {
        return form(id, "NURSING_INITIAL_ASSESSMENT", "NURSING", schemaJson);
    }

    private FormSubmission submission(long id, long formId, long patientId) {
        FormSubmission s = new FormSubmission();
        s.setId(id);
        s.setFormId(formId);
        s.setPatientId(patientId);
        s.setSubmissionJson("{\"chiefComplaint\":\"fever\"}");
        s.setStatus(true);
        return s;
    }

    String schemaWithRequired = """
            {"type":"object","properties":{"chiefComplaint":{"type":"string"},"painScore":{"type":"integer"}},"required":["chiefComplaint","painScore"]}
            """;

    @Test
    void getSchema_found() {
        when(formRepo.findById(1L)).thenReturn(Optional.of(form(1L, schemaWithRequired)));
        var f = service.getSchema(1L);
        assertThat(f.getCode()).isEqualTo("NURSING_INITIAL_ASSESSMENT");
    }

    @Test
    void getSchema_notFound_throws404() {
        when(formRepo.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getSchema(999L))
                .isInstanceOf(FormNotFoundException.class);
    }

    @Test
    void submit_validPayload_saves() {
        when(formRepo.findById(1L)).thenReturn(Optional.of(form(1L, schemaWithRequired)));
        var payload = Map.of("chiefComplaint", "fever", "painScore", 5);
        var sub = service.submit(1L, 10L, 20L, payload, 1L);
        assertThat(sub.getId()).isEqualTo(1L);
        assertThat(sub.getFormId()).isEqualTo(1L);
        assertThat(sub.getPatientId()).isEqualTo(10L);
    }

    @Test
    void submit_missingRequired_throws400() {
        when(formRepo.findById(1L)).thenReturn(Optional.of(form(1L, schemaWithRequired)));
        var payload = Map.of("chiefComplaint", "fever"); // missing painScore
        assertThatThrownBy(() -> service.submit(1L, null, null, payload, null))
                .isInstanceOf(InvalidFormRequestException.class)
                .hasMessageContaining("painScore");
    }

    @Test
    void submit_emptyPayload_throws400() {
        when(formRepo.findById(1L)).thenReturn(Optional.of(form(1L, schemaWithRequired)));
        assertThatThrownBy(() -> service.submit(1L, null, null, Map.of(), null))
                .isInstanceOf(InvalidFormRequestException.class);
    }

    @Test
    void submit_nullPayload_throws400() {
        when(formRepo.findById(1L)).thenReturn(Optional.of(form(1L, schemaWithRequired)));
        assertThatThrownBy(() -> service.submit(1L, null, null, null, null))
                .isInstanceOf(InvalidFormRequestException.class);
    }

    @Test
    void listForms_returnsAll() {
        when(formRepo.findAllActive()).thenReturn(List.of(form(1L, "{}"), form(2L, "{}")));
        assertThat(service.listForms()).hasSize(2);
    }

    @Test
    void listFormsBySpecialty_filtersAndUppercases() {
        when(formRepo.findAllActiveBySpecialty("NURSING"))
                .thenReturn(List.of(form(1L, "NURSING_INITIAL_ASSESSMENT", "NURSING", "{}")));
        var result = service.listFormsBySpecialty("nursing");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSpecialty()).isEqualTo("NURSING");
    }

    @Test
    void listFormsBySpecialty_blankReturnsAll() {
        when(formRepo.findAllActive()).thenReturn(List.of(form(1L, "{}")));
        var result = service.listFormsBySpecialty("  ");
        assertThat(result).hasSize(1);
    }

    @Test
    void listSpecialties() {
        when(formRepo.findDistinctSpecialties()).thenReturn(List.of("NURSING", "OBGYN", "PEDIATRICS"));
        assertThat(service.listSpecialties()).containsExactly("NURSING", "OBGYN", "PEDIATRICS");
    }

    @Test
    void listSubmissionsByPatient_ordersBySubmittedAtDesc() {
        when(subRepo.findByPatientIdOrderBySubmittedAtDesc(10L))
                .thenReturn(List.of(submission(3L, 1L, 10L), submission(1L, 1L, 10L)));
        var result = service.listSubmissionsByPatient(10L);
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(3L);
    }

    @Test
    void listSubmissionsByEncounter() {
        when(subRepo.findByEncounterId(20L)).thenReturn(List.of(submission(1L, 1L, 10L)));
        var result = service.listSubmissionsByEncounter(20L);
        assertThat(result).hasSize(1);
    }

    @Test
    void getSubmission_found() {
        when(subRepo.findByIdAndFormId(7L, 1L)).thenReturn(Optional.of(submission(7L, 1L, 10L)));
        var s = service.getSubmission(1L, 7L);
        assertThat(s.getId()).isEqualTo(7L);
    }

    @Test
    void getSubmission_notFound_throws404() {
        when(subRepo.findByIdAndFormId(999L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getSubmission(1L, 999L))
                .isInstanceOf(FormNotFoundException.class);
    }

    @Test
    void softDeleteSubmission_flipsStatus() {
        FormSubmission s = submission(7L, 1L, 10L);
        when(subRepo.findByIdAndFormId(7L, 1L)).thenReturn(Optional.of(s));
        var deleted = service.softDeleteSubmission(1L, 7L);
        assertThat(deleted.getStatus()).isFalse();
    }

    @Test
    void softDeleteSubmission_notFound_throws404() {
        when(subRepo.findByIdAndFormId(999L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.softDeleteSubmission(1L, 999L))
                .isInstanceOf(FormNotFoundException.class);
    }
}
