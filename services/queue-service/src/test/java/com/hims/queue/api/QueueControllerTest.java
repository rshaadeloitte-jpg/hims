package com.hims.queue.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.queue.application.QueueService;
import com.hims.queue.configuration.SecurityConfig;
import com.hims.queue.domain.model.QueueToken;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** L1 — REST contract: default-deny 401, status codes, JSON shape, error envelope. */
@WebMvcTest({QueueTokenController.class, QueueCounterController.class})
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class QueueControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private QueueService service;
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void businessEndpoints_areDefaultDenied() throws Exception {
        mockMvc.perform(post("/api/v1/queue/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":1,\"departmentId\":9}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/queue/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void issue_returns201WithTokenShape() throws Exception {
        QueueToken t = new QueueToken();
        t.setId(100L);
        t.setTokenNo(4L);
        t.setPatientId(501L);
        t.setDepartmentId(9L);
        when(service.issue(any(), eq(7L))).thenReturn(t);

        mockMvc.perform(post("/api/v1/queue/tokens?unitId=7")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("devops"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":501,\"departmentId\":9}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.tokenNo").value(4))
                .andExpect(jsonPath("$.queueStatus").value(1));
    }

    @Test
    void callNext_delegates() throws Exception {
        QueueToken t = new QueueToken();
        t.setId(11L);
        t.setTokenNo(2L);
        t.setQueueStatus(QueueToken.CALLED);
        when(service.callNext(1L)).thenReturn(t);

        mockMvc.perform(post("/api/v1/queue/counters/1/call-next").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("devops")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queueStatus").value(2));
    }

    @Test
    void dashboard_shapeAndDefaultDeny() throws Exception {
        when(service.dashboard(null)).thenReturn(Map.of(
                "queueDate", "2026-08-22",
                "waiting", 3L, "called", 1L, "completed", 5L, "noShow", 0L,
                "nowServing", Map.of("1", 7),
                "waitingList", List.of()));
        mockMvc.perform(get("/api/v1/queue/dashboard").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("devops")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.waiting").value(3))
                .andExpect(jsonPath("$.nowServing['1']").value(7))
                .andExpect(jsonPath("$.waitingList").isArray());
    }

    @Test
    void badRequest_mapsToBexaEnvelope_not500() throws Exception {
        when(service.issue(any(), any()))
                .thenThrow(new com.hims.queue.application.exceptions.InvalidQueueRequestException("BEXA003", "Invalid input data !"));
        mockMvc.perform(post("/api/v1/queue/tokens")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("devops"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BEXA003"));
    }

    @Test
    void unknownPath_is404NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/queue/does-not-exist")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("devops")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    @Test
    void counters_createAndList_contract() throws Exception {
        com.hims.queue.domain.model.Counter c = new com.hims.queue.domain.model.Counter();
        c.setId(1L);
        c.setCode("C1");
        c.setName("Counter 1");
        c.setDepartmentId(9L);
        c.setDoctorId(77L);
        when(service.createCounter(any(), eq(7L))).thenReturn(c);

        mockMvc.perform(post("/api/v1/queue/counters?unitId=7")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("devops"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"OPD-1\",\"name\":\"OPD Counter\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("C1"));

        when(service.listCounters(true)).thenReturn(List.of(c));
        mockMvc.perform(get("/api/v1/queue/counters?activeOnly=true")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("devops")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    // ---- gap-hardening additions (GAP-06): previously untested token endpoints ----

    @Test
    void listTokens_filtersByCounter() throws Exception {
        QueueToken t = new QueueToken();
        t.setId(100L);
        t.setTokenNo(4L);
        t.setCounterId(1L);
        when(service.listTokens(1L)).thenReturn(List.of(t));

        mockMvc.perform(get("/api/v1/queue/tokens?counterId=1")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("devops")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tokenNo").value(4));
    }

    @Test
    void transfer_returnsTokenToWaiting() throws Exception {
        QueueToken t = new QueueToken();
        t.setId(100L);
        t.setQueueStatus(QueueToken.WAITING);
        when(service.transfer(eq(100L), any())).thenReturn(t);

        mockMvc.perform(post("/api/v1/queue/tokens/100/transfer")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("devops"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"counterId\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queueStatus").value(1));
    }

    @Test
    void noShow_marksCalledToken() throws Exception {
        QueueToken t = new QueueToken();
        t.setId(100L);
        t.setQueueStatus(QueueToken.NO_SHOW);
        when(service.markNoShow(100L)).thenReturn(t);

        mockMvc.perform(post("/api/v1/queue/tokens/100/no-show")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("devops")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queueStatus").value(4));
    }

    @Test
    void complete_marksTokenCompleted() throws Exception {
        QueueToken t = new QueueToken();
        t.setId(100L);
        t.setQueueStatus(QueueToken.COMPLETED);
        when(service.complete(100L)).thenReturn(t);

        mockMvc.perform(post("/api/v1/queue/tokens/100/complete")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("devops")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queueStatus").value(3));
    }

    // ---- M27 department-specific queues (rad/patho) + cabin board ----

    @Test
    void departmentTokens_listByDepartment() throws Exception {
        QueueToken t = new QueueToken();
        t.setId(200L); t.setTokenNo(5L); t.setDepartmentId(9001L);
        when(service.listTokensByDepartment(9001L)).thenReturn(List.of(t));
        mockMvc.perform(get("/api/v1/queue/tokens/department/9001")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("devops")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].departmentId").value(9001));
    }

    @Test
    void departmentDashboard_returnsFilteredCounts() throws Exception {
        when(service.dashboardByDepartment(9001L)).thenReturn(Map.of(
                "queueDate", "2026-08-22", "departmentId", 9001L,
                "waiting", 2L, "called", 1L, "nowServing", Map.of(), "waitingList", List.of(), "counters", List.of()));
        mockMvc.perform(get("/api/v1/queue/department/9001/dashboard")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("devops")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departmentId").value(9001))
                .andExpect(jsonPath("$.waiting").value(2));
    }

    @Test
    void departmentCounters_returnsList() throws Exception {
        var c = new com.hims.queue.domain.model.Counter();
        c.setId(9001L); c.setCode("RAD-01"); c.setName("Radiology Counter 1"); c.setDepartmentId(9001L);
        when(service.listCountersByDepartment(9001L)).thenReturn(List.of(c));
        mockMvc.perform(get("/api/v1/queue/department/9001/counters")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("devops")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("RAD-01"));
    }

    @Test
    void cabinBoard_returnsTokensForCounter() throws Exception {
        when(service.cabinBoard(1L)).thenReturn(Map.of(
                "counterId", 1L, "counterCode", "C1", "tokens", List.of()));
        mockMvc.perform(get("/api/v1/queue/cabin/1/board")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("devops")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counterId").value(1));
    }

    @Test
    void departmentEndpoints_areDefaultDenied() throws Exception {
        mockMvc.perform(get("/api/v1/queue/department/9001/dashboard"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/queue/tokens/department/9001"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/queue/cabin/1/board"))
                .andExpect(status().isUnauthorized());
    }
}
