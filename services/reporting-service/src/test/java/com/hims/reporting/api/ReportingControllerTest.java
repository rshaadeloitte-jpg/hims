package com.hims.reporting.api;

import com.hims.reporting.application.EventIngressService;
import com.hims.reporting.application.ReportEngineService;
import com.hims.reporting.application.exceptions.InvalidReportingRequestException;
import com.hims.reporting.application.exceptions.ReportingConflictException;
import com.hims.reporting.application.exceptions.ReportingNotFoundException;
import com.hims.reporting.configuration.SecurityConfig;
import com.hims.reporting.domain.model.ReportExecution;
import com.hims.reporting.domain.model.ReportMaster;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contract tests for the Reporting API (default-deny enforced). Services mocked;
 * L0 covers the rules; L2 covers persistence on real PG16.
 */
@WebMvcTest({IngestionController.class, ReportController.class})
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class ReportingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventIngressService eventIngressService;

    @MockitoBean
    private ReportEngineService reportEngineService;

    private static ReportMaster master(long id, String code, short kind) {
        ReportMaster m = new ReportMaster();
        m.setReportId(id);
        m.setReportCode(code);
        m.setTitle("T " + code);
        m.setKind(kind);
        m.setDefinition("{\"table\":\"rm_patient_registry\",\"columns\":[\"patientId\"]}");
        m.setIsActive(true);
        return m;
    }

    private static ReportExecution execution(long id, int rowCount, String result) {
        ReportExecution e = new ReportExecution();
        e.setExecutionId(id);
        e.setReportId(10L);
        e.setStatus(ReportExecution.STATUS_DONE);
        e.setRowCount(rowCount);
        e.setResult(result);
        e.setExecutedAt(java.sql.Timestamp.valueOf("2026-03-01 10:00:00"));
        return e;
    }

    @Test
    void endpoints_requireAuth_defaultDeny() throws Exception {
        mockMvc.perform(get("/api/v1/reports")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/reports").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/reports/PATIENT-LIST/execute").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/reports/executions")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/reports/executions/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/dashboard/counts")).andExpect(status().isUnauthorized());
        // the INTERNAL ingress is default-deny too
        mockMvc.perform(post("/api/v1/internal/events").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ingest_applied_returnsStatusAndKey() throws Exception {
        when(eventIngressService.ingest(eq("PatientRegistered"), anyMap()))
                .thenReturn(new EventIngressService.IngestResult("APPLIED",
                        "PatientRegistered", "patient:501"));

        mockMvc.perform(post("/api/v1/internal/events").with(csrf())
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"eventType":"PatientRegistered",
                                 "payload":{"patientId":501,"mrn":"MRN-501","name":"Ramesh"}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPLIED"))
                .andExpect(jsonPath("$.eventType").value("PatientRegistered"))
                .andExpect(jsonPath("$.eventKey").value("patient:501"));
    }

    @Test
    void ingest_unknownEventType_mapsTo400Envelope() throws Exception {
        when(eventIngressService.ingest(eq("LabResultFinalized"), anyMap()))
                .thenThrow(new InvalidReportingRequestException("REPORTING_UNKNOWN_EVENT_TYPE",
                        "Unknown eventType: LabResultFinalized"));

        mockMvc.perform(post("/api/v1/internal/events").with(csrf())
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventType\":\"LabResultFinalized\",\"payload\":{}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errorCode").value("REPORTING_UNKNOWN_EVENT_TYPE"));
    }

    @Test
    void createReport_returns201_andDuplicateMapsTo409() throws Exception {
        when(reportEngineService.create(eq("PATIENT-LIST"), eq("Patient list"), eq((short) 1), any()))
                .thenReturn(master(10L, "PATIENT-LIST", (short) 1));

        mockMvc.perform(post("/api/v1/reports").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reportCode":"PATIENT-LIST","title":"Patient list","kind":1,
                                 "definition":{"table":"rm_patient_registry",
                                               "columns":["patientId","mrn","name"]}}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reportId").value(10))
                .andExpect(jsonPath("$.reportCode").value("PATIENT-LIST"))
                .andExpect(jsonPath("$.isActive").value(true));

        when(reportEngineService.create(eq("PATIENT-LIST"), any(), any(), any()))
                .thenThrow(new ReportingConflictException("REPORTING_DUPLICATE_REPORT_CODE",
                        "reportCode already exists: PATIENT-LIST"));
        mockMvc.perform(post("/api/v1/reports").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reportCode":"PATIENT-LIST","title":"Again","kind":1,
                                 "definition":{"table":"rm_patient_registry",
                                               "columns":["patientId"]}}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("REPORTING_DUPLICATE_REPORT_CODE"));
    }

    @Test
    void listReports_passesActiveFilter() throws Exception {
        when(reportEngineService.list(true)).thenReturn(List.of(master(11L, "R-A", (short) 2)));

        mockMvc.perform(get("/api/v1/reports").with(csrf()).with(user("devops").roles("DEV"))
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reportCode").value("R-A"))
                .andExpect(jsonPath("$[0].kind").value(2));
    }

    @Test
    void execute_returnsDoneExecution_andUnknownCodeMapsTo404() throws Exception {
        when(reportEngineService.execute(eq("PATIENT-LIST"), any()))
                .thenReturn(execution(7L, 2, "[{\"patient_id\":501},{\"patient_id\":502}]"));

        mockMvc.perform(post("/api/v1/reports/PATIENT-LIST/execute").with(csrf())
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"params\":{\"dateFrom\":\"2026-01-01\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionId").value(7))
                .andExpect(jsonPath("$.status").value(1))
                .andExpect(jsonPath("$.rowCount").value(2))
                .andExpect(jsonPath("$.result").isNotEmpty());

        when(reportEngineService.execute(eq("NOPE"), any()))
                .thenThrow(new ReportingNotFoundException("NOT_FOUND", "Report not found: NOPE"));
        mockMvc.perform(post("/api/v1/reports/NOPE/execute").with(csrf())
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    @Test
    void executionQueries_listAndById_with404() throws Exception {
        when(reportEngineService.listExecutions("PATIENT-LIST", "DONE"))
                .thenReturn(List.of(execution(7L, 2, "[]")));
        mockMvc.perform(get("/api/v1/reports/executions").with(csrf())
                        .with(user("devops").roles("DEV"))
                        .param("reportCode", "PATIENT-LIST").param("status", "DONE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].executionId").value(7))
                .andExpect(jsonPath("$[0].status").value(1));

        when(reportEngineService.getExecution(999L))
                .thenThrow(new ReportingNotFoundException("NOT_FOUND",
                        "Report execution not found: 999"));
        mockMvc.perform(get("/api/v1/reports/executions/999").with(csrf())
                        .with(user("devops").roles("DEV")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    @Test
    void dashboardCounts_returnsReadModelTotals() throws Exception {
        when(reportEngineService.dashboardCounts(isNull())).thenReturn(Map.of(
                "patients", 2L, "encounters", 5L, "revenueTotal", new BigDecimal("12345.67")));

        mockMvc.perform(get("/api/v1/dashboard/counts").with(csrf()).with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patients").value(2))
                .andExpect(jsonPath("$.encounters").value(5))
                .andExpect(jsonPath("$.revenueTotal").value(12345.67));
    }
}
