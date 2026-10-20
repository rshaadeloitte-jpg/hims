package com.hims.nursing.api;

import com.hims.nursing.application.NursingChartingService;
import com.hims.nursing.application.exceptions.InvalidNursingRequestException;
import com.hims.nursing.application.exceptions.NursingResourceNotFoundException;
import com.hims.nursing.configuration.SecurityConfig;
import com.hims.nursing.domain.model.NurseTask;
import com.hims.nursing.domain.model.PatientVitals;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contract tests for the nursing phase-1 API (default-deny enforced). Service is mocked; L1 covers the rules. */
@WebMvcTest(NursingController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class NursingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NursingChartingService service;

    private PatientVitals sampleVital() {
        PatientVitals v = new PatientVitals();
        v.setId(1L);
        v.setPatientId(501L);
        v.setAdmissionId(9L);
        v.setVitalsType("Heart Rate");
        v.setValue(new BigDecimal("72.00"));
        v.setUnit("bpm");
        return v;
    }

    @Test
    void recordVital_201() throws Exception {
        when(service.recordVital(eq(501L), eq(9L), eq("Heart Rate"),
                any(BigDecimal.class), eq("bpm"), any(), isNull()))
                .thenReturn(sampleVital());
        mockMvc.perform(post("/api/v1/patients/501/vitals").with(user("nurse"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"admissionId\":9,\"vitalsType\":\"Heart Rate\",\"value\":72,\"unit\":\"bpm\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.vitalsType").value("Heart Rate"));
    }

    @Test
    void recordVital_missingValue_400Bexa003() throws Exception {
        when(service.recordVital(any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new InvalidNursingRequestException("BEXA003", "value is required and must be numeric"));
        mockMvc.perform(post("/api/v1/patients/501/vitals").with(user("nurse"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vitalsType\":\"Heart Rate\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BEXA003"));
    }

    @Test
    void listVitals_requiresAuth_defaultDeny() throws Exception {
        mockMvc.perform(get("/api/v1/patients/501/vitals"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addNote_and_listNotes() throws Exception {
        mockMvc.perform(post("/api/v1/admissions/9/notes").with(user("nurse"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"Patient stable after rounds.\",\"nurseId\":7}"))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/v1/admissions/9/notes").with(user("nurse")))
                .andExpect(status().isOk());
    }

    @Test
    void completeTask_ok_illegalTransition400_notFound404() throws Exception {
        NurseTask pending = new NurseTask();
        pending.setId(7L);
        pending.setTaskStatus(NurseTask.STATUS_PENDING);
        when(service.completeTask(7L, null)).thenReturn(pending);

        NurseTask done = new NurseTask();
        done.setId(8L);
        done.setTaskStatus(NurseTask.STATUS_COMPLETED);
        when(service.completeTask(8L, null)).thenReturn(done);

        when(service.completeTask(9L, null))
                .thenThrow(new InvalidNursingRequestException("NURSING_ILLEGAL_TRANSITION",
                        "complete allowed only from PENDING(1), current=3"));
        when(service.completeTask(404L, null))
                .thenThrow(new NursingResourceNotFoundException("NOT_FOUND", "Nursing task not found: 404"));

        mockMvc.perform(post("/api/v1/tasks/7/complete").with(user("nurse")).content("{}")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskStatus").value(1));
        mockMvc.perform(post("/api/v1/tasks/8/complete").with(user("nurse")).content("{}")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskStatus").value(2));
        mockMvc.perform(post("/api/v1/tasks/9/complete").with(user("nurse")).content("{}")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("NURSING_ILLEGAL_TRANSITION"));
        mockMvc.perform(post("/api/v1/tasks/404/complete").with(user("nurse")).content("{}")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void listTasks_invalidStatusFilter_400() throws Exception {
        when(service.listTasks((short) 9, null))
                .thenThrow(new InvalidNursingRequestException("BEXA003",
                        "status must be 1=PENDING, 2=COMPLETED or 3=CANCELLED"));
        mockMvc.perform(get("/api/v1/tasks?status=9").with(user("nurse")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BEXA003"));
    }
}
