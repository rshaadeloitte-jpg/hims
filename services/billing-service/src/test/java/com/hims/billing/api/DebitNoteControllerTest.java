package com.hims.billing.api;

import com.hims.billing.application.DebitNoteService;
import com.hims.billing.configuration.SecurityConfig;
import com.hims.billing.domain.model.DebitNote;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contract tests for DebitNoteController.
 */
@WebMvcTest(DebitNoteController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class DebitNoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DebitNoteService debitNoteService;

    @Test
    void issue_validRequest_returns201() throws Exception {
        DebitNote dn = new DebitNote();
        dn.setDebitNoteId(1L);
        dn.setBillId(3L);
        dn.setAmount(new BigDecimal("500.00"));
        when(debitNoteService.issue(eq(3L), any(), any())).thenReturn(dn);

        mockMvc.perform(post("/api/v1/bills/3/debit-notes")
                        .with(user("devops")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":500.00,"reason":"additional charge"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(500.00));
    }

    @Test
    void approve_returns200() throws Exception {
        DebitNote dn = new DebitNote();
        dn.setDebitNoteId(1L);
        dn.setIsApplied(true);
        when(debitNoteService.approve(1L, null)).thenReturn(dn);

        mockMvc.perform(post("/api/v1/debit-notes/1/approve")
                        .with(user("devops")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isApplied").value(true));
    }

    @Test
    void get_returns200() throws Exception {
        DebitNote dn = new DebitNote();
        dn.setDebitNoteId(1L);
        dn.setAmount(new BigDecimal("500.00"));
        when(debitNoteService.get(1L)).thenReturn(dn);

        mockMvc.perform(get("/api/v1/debit-notes/1")
                        .with(user("devops")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(500.00));
    }

    @Test
    void listForBill_returns200WithList() throws Exception {
        DebitNote dn = new DebitNote();
        dn.setDebitNoteId(1L);
        dn.setBillId(3L);
        when(debitNoteService.listForBill(3L)).thenReturn(List.of(dn));

        mockMvc.perform(get("/api/v1/bills/3/debit-notes")
                        .with(user("devops")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].billId").value(3));
    }

    @Test
    void listForPatient_returns200WithList() throws Exception {
        DebitNote dn = new DebitNote();
        dn.setDebitNoteId(1L);
        dn.setPatientId(505L);
        when(debitNoteService.listForPatient(505L)).thenReturn(List.of(dn));

        mockMvc.perform(get("/api/v1/patients/505/debit-notes")
                        .with(user("devops")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].patientId").value(505));
    }

    @Test
    void endpoints_requireAuth_defaultDeny() throws Exception {
        mockMvc.perform(post("/api/v1/bills/3/debit-notes").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/debit-notes/1"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/debit-notes/1/approve").with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}
