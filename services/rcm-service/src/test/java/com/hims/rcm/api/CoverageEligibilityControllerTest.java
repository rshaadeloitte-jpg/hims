package com.hims.rcm.api;

import com.hims.rcm.application.NhcxService;
import com.hims.rcm.application.exceptions.InvalidRcmRequestException;
import com.hims.rcm.application.exceptions.RcmNotFoundException;
import com.hims.rcm.configuration.SecurityConfig;
import com.hims.rcm.domain.model.EligibilityCheck;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contract tests for the coverage-eligibility API (default-deny enforced). Services mocked; L0 covers the rules. */
@WebMvcTest(CoverageEligibilityController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class CoverageEligibilityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NhcxService nhcxService;

    private static EligibilityCheck check(long id, short status) {
        EligibilityCheck c = new EligibilityCheck();
        c.setEligibilityId(id);
        c.setPatientId(505L);
        c.setPayerScheme("RGHS");
        c.setStatusCode(status);
        c.setRequestPayload("{\"patientId\":505,\"payerScheme\":\"RGHS\"}");
        c.setRequestId(900L);
        c.setCorrelationId("corr-" + id);
        return c;
    }

    @Test
    void endpoints_requireAuth_defaultDeny() throws Exception {
        mockMvc.perform(post("/api/v1/coverage-eligibility").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/coverage-eligibility/1/response").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"payload\":{}}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_valid_returns201InitiatedWithCorrelation() throws Exception {
        when(nhcxService.createCoverageEligibility(505L, "RGHS")).thenReturn(check(10L, (short) 0));

        mockMvc.perform(post("/api/v1/coverage-eligibility").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":505,\"payerScheme\":\"RGHS\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eligibilityId").value(10))
                .andExpect(jsonPath("$.statusCode").value(0))
                .andExpect(jsonPath("$.correlationId").value("corr-10"))
                .andExpect(jsonPath("$.requestId").value(900));
    }

    @Test
    void create_blankPayerScheme_mapsTo400Bexa003Envelope() throws Exception {
        when(nhcxService.createCoverageEligibility(eq(505L), any()))
                .thenThrow(new InvalidRcmRequestException("BEXA003", "payerScheme is required"));

        mockMvc.perform(post("/api/v1/coverage-eligibility").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":505,\"payerScheme\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errorCode").value("BEXA003"));
    }

    @Test
    void respond_fromInitiated_pinsAcknowledged2() throws Exception {
        when(nhcxService.applyEligibilityResponse(eq(10L), any()))
                .thenReturn(check(10L, (short) 2));

        mockMvc.perform(put("/api/v1/coverage-eligibility/10/response").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"payload\":{\"eligible\":true}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(2));
    }

    @Test
    void respond_secondResponse_mapsTo400IllegalTransition() throws Exception {
        when(nhcxService.applyEligibilityResponse(eq(10L), any()))
                .thenThrow(new InvalidRcmRequestException("RCM_ILLEGAL_TRANSITION",
                        "Eligibility check 10 already has a response"));

        mockMvc.perform(put("/api/v1/coverage-eligibility/10/response").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"payload\":{}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("RCM_ILLEGAL_TRANSITION"));
    }

    @Test
    void respond_unknownCheck_mapsTo404Envelope() throws Exception {
        when(nhcxService.applyEligibilityResponse(eq(404L), any()))
                .thenThrow(new RcmNotFoundException("NOT_FOUND", "Eligibility check not found: 404"));

        mockMvc.perform(put("/api/v1/coverage-eligibility/404/response").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"payload\":{}}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }
}
