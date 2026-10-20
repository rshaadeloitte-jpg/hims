package com.hims.billing.api;

import com.hims.billing.application.BillingSagaIngressService;
import com.hims.billing.configuration.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contract tests for InternalEventController (WP-S1 saga ingress).
 */
@WebMvcTest(InternalEventController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class InternalEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BillingSagaIngressService sagaIngressService;

    @Test
    void consume_applied_returns201() throws Exception {
        BillingSagaIngressService.IngestResult result = BillingSagaIngressService.IngestResult.applied(
                "evt-001", 100L, 50L, 2);
        when(sagaIngressService.ingest(anyString(), anyString(), any())).thenReturn(result);

        mockMvc.perform(post("/api/v1/internal/events")
                        .with(user("devops")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"eventId":"evt-001","eventType":"OrderCreated","payload":{
                                  "orderId":50,"patientId":505,"encounterId":7777,
                                  "lines":[{"itemId":900,"serviceName":"X-Ray","rate":350,"qty":1}]
                                }}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPLIED"))
                .andExpect(jsonPath("$.billId").value(100));
    }

    @Test
    void consume_duplicate_returns200() throws Exception {
        BillingSagaIngressService.IngestResult result = BillingSagaIngressService.IngestResult.duplicate(
                "evt-001", 100L, 50L);
        when(sagaIngressService.ingest(anyString(), anyString(), any())).thenReturn(result);

        mockMvc.perform(post("/api/v1/internal/events")
                        .with(user("devops")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"eventId":"evt-001","eventType":"OrderCreated","payload":{
                                  "orderId":50,"patientId":505,
                                  "lines":[{"itemId":900,"serviceName":"X-Ray","rate":350,"qty":1}]
                                }}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DUPLICATE"));
    }

    @Test
    void consume_rejected_returns422() throws Exception {
        BillingSagaIngressService.IngestResult result = BillingSagaIngressService.IngestResult.rejected(
                "evt-002", 50L, "BILL_REQ_INVALID", "patientId is required");
        when(sagaIngressService.ingest(anyString(), anyString(), any())).thenReturn(result);

        mockMvc.perform(post("/api/v1/internal/events")
                        .with(user("devops")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"eventId":"evt-002","eventType":"OrderCreated","payload":{
                                  "orderId":50,
                                  "lines":[{"itemId":900,"serviceName":"X-Ray","rate":350,"qty":1}]
                                }}"""))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void endpoints_requireAuth_defaultDeny() throws Exception {
        mockMvc.perform(post("/api/v1/internal/events").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
