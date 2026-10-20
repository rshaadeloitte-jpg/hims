package com.hims.rmrs.api;

import com.hims.rmrs.application.MasterService;
import com.hims.rmrs.application.exceptions.DuplicateRmrsResourceException;
import com.hims.rmrs.application.exceptions.InvalidRmrsRequestException;
import com.hims.rmrs.configuration.SecurityConfig;
import com.hims.rmrs.domain.model.AuthorityMaster;
import com.hims.rmrs.domain.model.HeadMaster;
import com.hims.rmrs.domain.model.LedgerMaster;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contract tests for the treasury masters API (default-deny enforced). Services mocked; L0 covers the rules. */
@WebMvcTest(MasterController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class MasterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MasterService masterService;

    @Test
    void endpoints_requireAuth_defaultDeny() throws Exception {
        mockMvc.perform(get("/api/v1/masters/heads")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/masters/heads").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/masters/authorities")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/masters/authorities").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/masters/ledgers")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/masters/ledgers").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createHead_valid_returns201() throws Exception {
        HeadMaster head = new HeadMaster();
        head.setHeadId(1L);
        head.setHeadCode("PHARMACY");
        head.setHeadName("Pharmacy cash head");
        head.setIsActive(true);
        when(masterService.createHead("PHARMACY", "Pharmacy cash head")).thenReturn(head);

        mockMvc.perform(post("/api/v1/masters/heads").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"headCode":"PHARMACY","headName":"Pharmacy cash head"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.headId").value(1))
                .andExpect(jsonPath("$.headCode").value("PHARMACY"))
                .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    void createHead_duplicateCode_mapsTo409Envelope() throws Exception {
        when(masterService.createHead(eq("PHARMACY"), any()))
                .thenThrow(new DuplicateRmrsResourceException("RMRS_DUPLICATE_CODE",
                        "headCode already exists: PHARMACY"));

        mockMvc.perform(post("/api/v1/masters/heads").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"headCode":"PHARMACY","headName":"x"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errorCode").value("RMRS_DUPLICATE_CODE"));
    }

    @Test
    void createAuthority_valid_withOptionalHead() throws Exception {
        AuthorityMaster authority = new AuthorityMaster();
        authority.setAuthorityId(5L);
        authority.setAuthorityCode("CASH-A");
        authority.setAuthorityName("Cash counter A");
        authority.setHeadId(1L);
        when(masterService.createAuthority("CASH-A", "Cash counter A", 1L)).thenReturn(authority);

        mockMvc.perform(post("/api/v1/masters/authorities").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"authorityCode":"CASH-A","authorityName":"Cash counter A","headId":1}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorityCode").value("CASH-A"))
                .andExpect(jsonPath("$.headId").value(1));
    }

    @Test
    void createLedger_unknownHead_mapsTo400Bexa003Envelope() throws Exception {
        when(masterService.createLedger(eq("L1"), any(), eq(99L)))
                .thenThrow(new InvalidRmrsRequestException("Unknown headId for ledger: 99"));

        mockMvc.perform(post("/api/v1/masters/ledgers").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ledgerCode":"L1","ledgerName":"Main ledger","headId":99}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BEXA003"));
    }

    @Test
    void lists_return200() throws Exception {
        LedgerMaster ledger = new LedgerMaster();
        ledger.setLedgerId(11L);
        ledger.setLedgerCode("PH-LEDGER");
        ledger.setHeadId(1L);
        when(masterService.listHeads()).thenReturn(List.of());
        when(masterService.listAuthorities()).thenReturn(List.of());
        when(masterService.listLedgers()).thenReturn(List.of(ledger));

        mockMvc.perform(get("/api/v1/masters/heads").with(csrf()).with(user("devops").roles("DEV")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/masters/authorities").with(csrf()).with(user("devops").roles("DEV")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/masters/ledgers").with(csrf()).with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ledgerCode").value("PH-LEDGER"));
    }
}
