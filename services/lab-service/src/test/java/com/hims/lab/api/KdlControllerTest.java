package com.hims.lab.api;

import com.hims.lab.application.KdlService;
import com.hims.lab.configuration.SecurityConfig;
import com.hims.lab.domain.model.KdlResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(KdlController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class KdlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KdlService kdlService;

    @Test
    void endpoints_requireAuth_defaultDeny() throws Exception {
        mockMvc.perform(post("/api/v1/kdl/results").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/kdl/results/unmapped")).andExpect(status().isUnauthorized());
    }

    @Test
    void parseAndStore_valid_returns201() throws Exception {
        KdlResult r = new KdlResult();
        r.setKdlResultId(1L);
        r.setProtocol("HL7");
        r.setRawPayload("MSH|^~\\&|SENDER|FAC");
        r.setLabNumber("LAB-001");
        when(kdlService.parseAndStore(nullable(Long.class), anyString(), anyString(), nullable(String.class), nullable(String.class), nullable(String.class), nullable(String.class), nullable(String.class)))
                .thenReturn(r);

        mockMvc.perform(post("/api/v1/kdl/results").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"machineId\":1,\"protocol\":\"HL7\",\"rawPayload\":\"MSH|^~\\\\&|SENDER|FAC\",\"labNumber\":\"LAB-001\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.kdlResultId").value(1))
                .andExpect(jsonPath("$.protocol").value("HL7"));
    }

    @Test
    void parseAndStore_missingProtocol_returns400() throws Exception {
        when(kdlService.parseAndStore(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new com.hims.lab.application.exceptions.InvalidLabRequestException("LAB_REQ_INVALID", "protocol is required"));

        mockMvc.perform(post("/api/v1/kdl/results").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rawPayload\":\"foo\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void mapToDetail_returnsMappedResult() throws Exception {
        KdlResult r = new KdlResult();
        r.setKdlResultId(5L);
        r.setIsMapped(true);
        r.setMappedDetailId(21L);
        when(kdlService.mapToDetail(5L, 21L)).thenReturn(r);

        mockMvc.perform(post("/api/v1/kdl/results/5/map").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"detailId\":21}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isMapped").value(true))
                .andExpect(jsonPath("$.mappedDetailId").value(21));
    }

    @Test
    void listUnmapped_returnsResults() throws Exception {
        when(kdlService.listUnmapped()).thenReturn(List.of(new KdlResult()));

        mockMvc.perform(get("/api/v1/kdl/results/unmapped").with(csrf()).with(user("devops").roles("DEV")))
                .andExpect(status().isOk());
    }

    @Test
    void get_returnsResult() throws Exception {
        KdlResult r = new KdlResult();
        r.setKdlResultId(7L);
        when(kdlService.get(7L)).thenReturn(r);

        mockMvc.perform(get("/api/v1/kdl/results/7").with(csrf()).with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kdlResultId").value(7));
    }
}
