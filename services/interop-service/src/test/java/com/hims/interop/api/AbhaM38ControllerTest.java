package com.hims.interop.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.interop.application.AbhaService;
import com.hims.interop.domain.model.AbhaConsent;
import com.hims.interop.domain.model.AbhaLink;
import com.hims.interop.domain.model.PhrPush;
import com.hims.interop.configuration.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AbhaM38Controller.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class AbhaM38ControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private AbhaService service;
    private final ObjectMapper om = new ObjectMapper();

    @Test void createLink_returnsCreated() throws Exception {
        AbhaLink link = new AbhaLink(); link.setId(1L); link.setLinkStatus(AbhaLink.STATUS_PENDING_VERIFY);
        when(service.createLink(any(), any(), any(), any())).thenReturn(link);
        mockMvc.perform(post("/api/v1/interop/abha-m38/links")
                .with(user("u").roles("DOCTOR"))
                .contentType("application/json")
                .content("{\"abhaAddress\":\"x@y\",\"txnId\":\"t1\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1));
    }

    @Test void verifyLink_returnsVerified() throws Exception {
        AbhaLink link = new AbhaLink(); link.setId(1L); link.setLinkStatus(AbhaLink.STATUS_VERIFIED);
        when(service.verifyLink(anyLong(), any())).thenReturn(link);
        mockMvc.perform(post("/api/v1/interop/abha-m38/links/1/verify")
                .with(user("u").roles("DOCTOR")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.linkStatus").value(2));
    }

    @Test void delink_returnsDelinked() throws Exception {
        AbhaLink link = new AbhaLink(); link.setId(1L); link.setLinkStatus(AbhaLink.STATUS_DELINKED);
        when(service.delink(anyLong())).thenReturn(link);
        mockMvc.perform(post("/api/v1/interop/abha-m38/links/1/delink")
                .with(user("u").roles("DOCTOR")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.linkStatus").value(3));
    }

    @Test void grantConsent_returnsCreated() throws Exception {
        AbhaConsent c = new AbhaConsent(); c.setId(1L);
        when(service.grantConsent(anyLong(), any(), any(), any(), any(), any(), any())).thenReturn(c);
        mockMvc.perform(post("/api/v1/interop/abha-m38/consents")
                .with(user("u").roles("DOCTOR"))
                .contentType("application/json")
                .content("{\"abhaLinkId\":1,\"consentRequestId\":\"r1\",\"purpose\":\"CAREMGT\",\"hipId\":\"H1\",\"hiuId\":\"HU1\",\"validFrom\":\"2026-01-01T00:00:00\",\"validTo\":\"2026-12-31T23:59:59\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1));
    }

    @Test void queuePhrPush_returnsCreated() throws Exception {
        PhrPush p = new PhrPush(); p.setId(1L); p.setDocStatus(PhrPush.STATUS_PENDING);
        when(service.queuePush(anyLong(), any(), any())).thenReturn(p);
        mockMvc.perform(post("/api/v1/interop/abha-m38/phr-push")
                .with(user("u").roles("DOCTOR"))
                .contentType("application/json")
                .content("{\"abhaLinkId\":1,\"payloadType\":\"OPConsultation\",\"payload\":{}}"))
            .andExpect(status().isCreated());
    }

    @Test void listByPatient_returnsList() throws Exception {
        AbhaLink l = new AbhaLink(); l.setPatientId(7L);
        when(service.findByPatient(anyLong())).thenReturn(List.of(l));
        mockMvc.perform(get("/api/v1/interop/abha-m38/links/by-patient/7")
                .with(user("u").roles("DOCTOR")))
            .andExpect(status().isOk());
    }
}
