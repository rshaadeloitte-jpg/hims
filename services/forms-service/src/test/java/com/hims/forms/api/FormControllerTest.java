package com.hims.forms.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.forms.application.FormService;
import com.hims.forms.configuration.SecurityConfig;
import com.hims.forms.domain.model.FormDefinition;
import com.hims.forms.domain.model.FormSubmission;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FormController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class FormControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean FormService service;
    @Autowired ObjectMapper objectMapper;

    private static FormDefinition def(long id, String code) {
        FormDefinition f = new FormDefinition();
        f.setId(id);
        f.setCode(code);
        f.setName(code.replace("_", " "));
        f.setSpecialty("NURSING");
        f.setDescription("test");
        f.setSchemaJson("{\"type\":\"object\",\"properties\":{\"chiefComplaint\":{\"type\":\"string\"}},\"required\":[\"chiefComplaint\"]}");
        f.setVersion(1);
        f.setStatus(true);
        f.setLegacyTiuxmlRef(code + ".json");
        return f;
    }

    private static FormSubmission sub(long id, long formId) {
        FormSubmission s = new FormSubmission();
        s.setId(id);
        s.setFormId(formId);
        s.setSubmissionJson("{\"chiefComplaint\":\"fever\"}");
        s.setSubmittedAt(Timestamp.from(Instant.now()));
        s.setStatus(true);
        return s;
    }

    @Test
    void list_requiresAuth() throws Exception {
        mockMvc.perform(get("/api/v1/forms"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void list_returnsForms() throws Exception {
        when(service.listFormsBySpecialty(null)).thenReturn(List.of(def(1L, "NURSING_INITIAL_ASSESSMENT"), def(2L, "EMERGENCY_TRIAGE")));
        mockMvc.perform(get("/api/v1/forms").with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("NURSING_INITIAL_ASSESSMENT"))
                .andExpect(jsonPath("$[1].code").value("EMERGENCY_TRIAGE"));
    }

    @Test
    void list_withSpecialty_callsFilter() throws Exception {
        when(service.listFormsBySpecialty("NURSING")).thenReturn(List.of(def(1L, "NURSING_INITIAL_ASSESSMENT")));
        mockMvc.perform(get("/api/v1/forms").param("specialty", "NURSING").with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("NURSING_INITIAL_ASSESSMENT"));
    }

    @Test
    void specialties_returnsList() throws Exception {
        when(service.listSpecialties()).thenReturn(List.of("NURSING", "OBGYN", "PEDIATRICS"));
        mockMvc.perform(get("/api/v1/forms/specialties").with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("NURSING"));
    }

    @Test
    void getSchema_returnsMapWithSchema() throws Exception {
        when(service.getSchema(1L)).thenReturn(def(1L, "NURSING_INITIAL_ASSESSMENT"));
        mockMvc.perform(get("/api/v1/forms/1/schema").with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("NURSING_INITIAL_ASSESSMENT"))
                .andExpect(jsonPath("$.schema.type").value("object"));
    }

    @Test
    void submit_returns201() throws Exception {
        when(service.submit(eq(1L), any(), any(), any(), any())).thenReturn(sub(10L, 1L));
        mockMvc.perform(post("/api/v1/forms/1/submit").with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"chiefComplaint\":\"fever\",\"painScore\":5}")
                        .param("patientId", "123"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.formId").value(1));
    }

    @Test
    void listSubmissions_returnsList() throws Exception {
        when(service.listSubmissions(1L)).thenReturn(List.of(sub(1L, 1L)));
        mockMvc.perform(get("/api/v1/forms/1/submissions").with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].formId").value(1));
    }

    @Test
    void listByPatient_returnsList() throws Exception {
        when(service.listSubmissionsByPatient(10L)).thenReturn(List.of(sub(1L, 1L)));
        mockMvc.perform(get("/api/v1/forms/submissions/patient/10").with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].formId").value(1));
    }

    @Test
    void listByEncounter_returnsList() throws Exception {
        when(service.listSubmissionsByEncounter(20L)).thenReturn(List.of(sub(1L, 1L)));
        mockMvc.perform(get("/api/v1/forms/submissions/encounter/20").with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].formId").value(1));
    }

    @Test
    void getSubmission_returnsOne() throws Exception {
        when(service.getSubmission(1L, 7L)).thenReturn(sub(7L, 1L));
        mockMvc.perform(get("/api/v1/forms/1/submissions/7").with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void deleteSubmission_returns200WithStatusFalse() throws Exception {
        FormSubmission deleted = sub(7L, 1L);
        deleted.setStatus(false);
        when(service.softDeleteSubmission(1L, 7L)).thenReturn(deleted);
        mockMvc.perform(delete("/api/v1/forms/1/submissions/7").with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(false));
    }
}
