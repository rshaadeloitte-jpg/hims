package com.hims.grievance.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.grievance.application.GrievanceTicketService;
import com.hims.grievance.configuration.SecurityConfig;
import com.hims.grievance.domain.model.GrievanceComment;
import com.hims.grievance.domain.model.GrievanceTicket;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contract tests for the grievance ticket API (default-deny enforced). Service is mocked; L1 covers the state machine. */
@WebMvcTest(GrievanceTicketController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class GrievanceTicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GrievanceTicketService service;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void raise_requiresAuth_defaultDeny() throws Exception {
        mockMvc.perform(post("/api/v1/grievances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"issue\":\"Y\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void raise_validRequest_returns201WithTicket() throws Exception {
        GrievanceTicket t = new GrievanceTicket();
        t.setId(1L);
        t.setTicketNo("IHMS/G/2027/00001");
        t.setIssueStatus((short) 1);
        t.setName("Ramesh");
        t.setIssue("Long OPD wait");
        when(service.raiseTicket(any(), isNull(), eq(false))).thenReturn(t);

        mockMvc.perform(post("/api/v1/grievances")
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ramesh\",\"issue\":\"Long OPD wait\",\"mobileNo\":9876543210}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticketNo").value("IHMS/G/2027/00001"))
                .andExpect(jsonPath("$.issueStatus").value(1));
    }

    @Test
    void counts_returnsDashboardMap() throws Exception {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("TOTAL", 5L);
        counts.put("NEW", 2L);
        counts.put("ASSIGNED", 2L);
        counts.put("RESOLVED", 1L);
        counts.put("RAISED-BY-CITIZEN", 1L);
        counts.put("MY-LIST", 3L);
        counts.put("TODAYS-TICKET", 2L);
        when(service.getTicketCounts(42L)).thenReturn(counts);

        mockMvc.perform(get("/api/v1/grievances/counts")
                        .with(user("devops").roles("DEV"))
                        .param("employeeId", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.TOTAL").value(5))
                .andExpect(jsonPath("$.NEW").value(2))
                .andExpect(jsonPath("$.MY-LIST").value(3))
                .andExpect(jsonPath("$.TODAYS-TICKET").value(2));
    }

    @Test
    void changeStatus_resolved_returnsUpdatedTicket() throws Exception {
        GrievanceTicket t = new GrievanceTicket();
        t.setId(5L);
        t.setIssueStatus((short) 3);
        t.setTicketCloseStatus((short) 2);
        when(service.changeStatus(eq(5L), eq((short) 3), eq(false))).thenReturn(t);

        mockMvc.perform(post("/api/v1/grievances/5/status")
                        .with(user("devops").roles("DEV"))
                        .param("issueStatus", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issueStatus").value(3))
                .andExpect(jsonPath("$.ticketCloseStatus").value(2));
    }

    @Test
    void addComment_returns201() throws Exception {
        GrievanceComment c = new GrievanceComment();
        c.setId(1L);
        c.setGrievanceId(5L);
        c.setComment("Looking into it");
        when(service.addComment(eq(5L), eq("Looking into it"))).thenReturn(c);

        mockMvc.perform(post("/api/v1/grievances/5/comments")
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"Looking into it\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.grievanceId").value(5))
                .andExpect(jsonPath("$.comment").value("Looking into it"));
    }

    @Test
    void list_returnsTickets() throws Exception {
        GrievanceTicket t = new GrievanceTicket();
        t.setId(1L);
        t.setTicketNo("IHMS/G/2027/00001");
        when(service.listTickets(isNull(), isNull())).thenReturn(List.of(t));

        mockMvc.perform(get("/api/v1/grievances")
                        .with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ticketNo").value("IHMS/G/2027/00001"));
    }

    @Test
    void unmappedPath_returns404_not500() throws Exception {
        mockMvc.perform(get("/api/v1/does-not-exist")
                        .with(user("devops").roles("DEV")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }
}
