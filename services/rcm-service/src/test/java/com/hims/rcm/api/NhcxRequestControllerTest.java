package com.hims.rcm.api;

import com.hims.rcm.application.NhcxService;
import com.hims.rcm.application.exceptions.InvalidRcmRequestException;
import com.hims.rcm.application.exceptions.RcmNotFoundException;
import com.hims.rcm.configuration.SecurityConfig;
import com.hims.rcm.domain.model.NhcxRequest;
import com.hims.rcm.domain.model.NhcxRequestDetail;
import com.hims.rcm.domain.model.NhcxStatus;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contract tests for NHCX request intake / callback / list APIs (default-deny enforced). */
@WebMvcTest(NhcxRequestController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class NhcxRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NhcxService nhcxService;

    private static NhcxRequest request(long id, short kind, short status, String correlationId) {
        NhcxRequest r = new NhcxRequest();
        r.setRequestId(id);
        r.setKind(kind);
        r.setPatientId(505L);
        r.setStatusCode(status);
        r.setCorrelationId(correlationId);
        r.setRequestPayload("{\"kind\":" + kind + "}");
        NhcxRequestDetail d = new NhcxRequestDetail();
        d.setRequestId(id);
        d.setLineNo(1L);
        d.setDetail("{\"line\":1}");
        r.setDetails(List.of(d));
        return r;
    }

    @Test
    void endpoints_requireAuth_defaultDeny() throws Exception {
        mockMvc.perform(get("/api/v1/nhcx")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/pre-auths").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/claims").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/nhcx/some-correlation/callback").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":2}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createPreAuth_valid_returnsRequestWithCorrelationAndDetails() throws Exception {
        when(nhcxService.createPreAuth(eq(505L), eq(8L), any()))
                .thenReturn(request(20L, NhcxRequest.KIND_PRE_AUTH, (short) 0, "corr-pre"));

        mockMvc.perform(post("/api/v1/pre-auths").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":505,\"admissionId\":8,\"payload\":{\"lines\":[{\"item\":1}]}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(20))
                .andExpect(jsonPath("$.kind").value(2))
                .andExpect(jsonPath("$.statusCode").value(0))
                .andExpect(jsonPath("$.correlationId").value("corr-pre"))
                .andExpect(jsonPath("$.details[0].lineNo").value(1));
    }

    @Test
    void createPreAuth_missingPayload_mapsTo400Bexa003() throws Exception {
        when(nhcxService.createPreAuth(any(), any(), isNull()))
                .thenThrow(new InvalidRcmRequestException("BEXA003", "payload is required"));

        mockMvc.perform(post("/api/v1/pre-auths").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":505}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BEXA003"));
    }

    @Test
    void createClaim_valid_returnsClaimSubmitRecord() throws Exception {
        when(nhcxService.createClaim(eq(505L), eq(9001L), any()))
                .thenAnswer(inv -> {
                    NhcxRequest claim = request(30L, NhcxRequest.KIND_CLAIM_SUBMIT, (short) 0, "corr-claim");
                    claim.setBillId(9001L);   // mirrors production: persisted record carries billId
                    return claim;
                });

        mockMvc.perform(post("/api/v1/claims").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":505,\"billId\":9001,\"payload\":{\"amount\":100}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(30))
                .andExpect(jsonPath("$.kind").value(3))
                .andExpect(jsonPath("$.billId").value(9001))
                .andExpect(jsonPath("$.correlationId").value("corr-claim"));
    }

    @Test
    void createClaim_missingBillId_mapsTo400Bexa003() throws Exception {
        when(nhcxService.createClaim(eq(505L), isNull(), any()))
                .thenThrow(new InvalidRcmRequestException("BEXA003", "billId is required for claim submission"));

        mockMvc.perform(post("/api/v1/claims").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":505,\"payload\":{}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BEXA003"));
    }

    @Test
    void callback_unknownCorrelation_mapsTo404Envelope() throws Exception {
        when(nhcxService.callback(eq("ghost"), any(), any()))
                .thenThrow(new RcmNotFoundException("NOT_FOUND", "Unknown correlation id: ghost"));

        mockMvc.perform(put("/api/v1/nhcx/ghost/callback").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"payload\":{},\"status\":2}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    @Test
    void callback_forwardTransition_returnsAdvancedStatus() throws Exception {
        when(nhcxService.callback(eq("corr-ok"), any(), eq((short) 2)))
                .thenReturn(request(20L, NhcxRequest.KIND_PRE_AUTH, (short) 2, "corr-ok"));

        mockMvc.perform(put("/api/v1/nhcx/corr-ok/callback").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"payload\":{\"ack\":true},\"status\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(2));
    }

    @Test
    void callback_backwardTransition_mapsTo400IllegalTransition() throws Exception {
        when(nhcxService.callback(eq("corr-back"), any(), eq((short) 0)))
                .thenThrow(new InvalidRcmRequestException("RCM_ILLEGAL_TRANSITION",
                        "Cannot move request 20 from SENT back to INITIATED"));

        mockMvc.perform(put("/api/v1/nhcx/corr-back/callback").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"payload\":{},\"status\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("RCM_ILLEGAL_TRANSITION"));
    }

    @Test
    void list_acceptsNamedOrNumericKindAndStatusFilters() throws Exception {
        when(nhcxService.list((short) NhcxRequest.KIND_CLAIM_SUBMIT, (short) NhcxStatus.SENT))
                .thenReturn(List.of(request(31L, NhcxRequest.KIND_CLAIM_SUBMIT, NhcxStatus.SENT, "corr-l1")));

        mockMvc.perform(get("/api/v1/nhcx").with(csrf()).with(user("devops").roles("DEV"))
                        .param("kind", "CLAIM_SUBMIT").param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].requestId").value(31));

        mockMvc.perform(get("/api/v1/nhcx").with(csrf()).with(user("devops").roles("DEV"))
                        .param("kind", "3").param("status", "SENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].correlationId").value("corr-l1"));
    }
}
