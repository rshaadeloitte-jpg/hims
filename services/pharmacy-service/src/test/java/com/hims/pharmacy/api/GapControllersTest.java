package com.hims.pharmacy.api;

import com.hims.pharmacy.application.DrugAllergyService;
import com.hims.pharmacy.application.DrugInteractionService;
import com.hims.pharmacy.application.SaleReturnService;
import com.hims.pharmacy.application.exceptions.InvalidSaleRequestException;
import com.hims.pharmacy.configuration.SecurityConfig;
import com.hims.pharmacy.domain.model.DrugAllergy;
import com.hims.pharmacy.domain.model.DrugInteraction;
import com.hims.pharmacy.domain.model.SaleReturn;
import com.hims.pharmacy.domain.model.SaleReturnLine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contract tests for the M07 gap APIs: sale returns, drug allergies, drug interactions.
 */
@WebMvcTest(controllers = {SaleReturnController.class, DrugAllergyController.class,
        DrugInteractionController.class})
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class GapControllersTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean SaleReturnService saleReturnService;
    @MockitoBean DrugAllergyService drugAllergyService;
    @MockitoBean DrugInteractionService drugInteractionService;

    // ------------------------------------------------------- sale return

    private static SaleReturn makeReturn(long id, short status) {
        SaleReturn r = new SaleReturn();
        r.setReturnId(id);
        r.setOriginalSaleId(100L);
        r.setPatientId(50L);
        r.setReturnReason("DAMAGED");
        r.setStatusCode(status);
        r.setTotalRefundAmount(new BigDecimal("25.00"));
        SaleReturnLine line = new SaleReturnLine();
        line.setReturnId(id);
        line.setLineNo(1L);
        line.setItemId(5L);
        line.setReturnQty(2);
        line.setUnitPrice(new BigDecimal("12.50"));
        line.setRefundAmount(new BigDecimal("25.00"));
        r.setLines(List.of(line));
        return r;
    }

    @Test
    void saleReturn_endpoints_requireAuth() throws Exception {
        mockMvc.perform(post("/api/v1/sale-returns").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/sale-returns/1"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/sale-returns"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void saleReturn_initiate_returns201() throws Exception {
        when(saleReturnService.initiate(eq(100L), eq(50L), eq("DAMAGED"), any(), any()))
                .thenReturn(makeReturn(1L, SaleReturn.STATUS_INITIATED));

        mockMvc.perform(post("/api/v1/sale-returns").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"originalSaleId":100,"patientId":50,"returnReason":"DAMAGED",
                                 "lines":[{"itemId":5,"returnQty":2,"unitPrice":12.50}]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.returnId").value(1))
                .andExpect(jsonPath("$.statusCode").value((int) SaleReturn.STATUS_INITIATED));
    }

    @Test
    void saleReturn_initiate_badReason_mapsTo400() throws Exception {
        when(saleReturnService.initiate(any(), any(), any(), any(), any()))
                .thenThrow(new InvalidSaleRequestException("BEXA003",
                        "returnReason must be one of [DAMAGED, WRONG_ITEM, PATIENT_REFUSED, EXPIRED, OTHER]"));

        mockMvc.perform(post("/api/v1/sale-returns").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"originalSaleId":100,"patientId":50,"returnReason":"BOGUS",
                                 "lines":[{"itemId":5,"returnQty":2}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BEXA003"));
    }

    @Test
    void saleReturn_complete_returns200() throws Exception {
        when(saleReturnService.complete(eq(1L), any(), any()))
                .thenReturn(makeReturn(1L, SaleReturn.STATUS_COMPLETED));
        mockMvc.perform(post("/api/v1/sale-returns/1/complete").with(csrf()).with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value((int) SaleReturn.STATUS_COMPLETED));
    }

    @Test
    void saleReturn_reject_returns200() throws Exception {
        when(saleReturnService.reject(eq(1L), any()))
                .thenReturn(makeReturn(1L, SaleReturn.STATUS_REJECTED));
        mockMvc.perform(post("/api/v1/sale-returns/1/reject").with(csrf()).with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value((int) SaleReturn.STATUS_REJECTED));
    }

    @Test
    void saleReturn_list_filters() throws Exception {
        when(saleReturnService.list(eq((short) 1), eq(50L))).thenReturn(List.of());
        mockMvc.perform(get("/api/v1/sale-returns").with(user("devops").roles("DEV"))
                        .param("status", "1").param("patientId", "50"))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------- drug allergy

    @Test
    void drugAllergy_endpoints_requireAuth() throws Exception {
        mockMvc.perform(post("/api/v1/drug-allergies").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/drug-allergies/patient/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void drugAllergy_add_returns201() throws Exception {
        DrugAllergy a = new DrugAllergy();
        a.setAllergyId(1L);
        a.setPatientId(50L);
        a.setAllergen("Penicillin");
        a.setAllergyType("DRUG");
        a.setSeverity("SEVERE");
        a.setReactionDescription("Anaphylaxis");
        a.setRecordedAt(Timestamp.from(Instant.now()));
        when(drugAllergyService.add(eq(50L), eq("Penicillin"), eq("DRUG"), eq("SEVERE"),
                eq("Anaphylaxis"), any())).thenReturn(a);

        mockMvc.perform(post("/api/v1/drug-allergies").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":50,"allergen":"Penicillin","allergyType":"DRUG",
                                 "severity":"SEVERE","reactionDescription":"Anaphylaxis"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.allergen").value("Penicillin"))
                .andExpect(jsonPath("$.severity").value("SEVERE"));
    }

    @Test
    void drugAllergy_add_badSeverity_mapsTo400() throws Exception {
        when(drugAllergyService.add(any(), any(), any(), any(), any(), any()))
                .thenThrow(new InvalidSaleRequestException("BEXA003",
                        "severity must be one of [MILD, MODERATE, SEVERE, ANAPHYLAXIS]"));

        mockMvc.perform(post("/api/v1/drug-allergies").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":50,"allergen":"X","severity":"FOO"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BEXA003"));
    }

    @Test
    void drugAllergy_listByPatient() throws Exception {
        when(drugAllergyService.listByPatient(50L)).thenReturn(List.of());
        mockMvc.perform(get("/api/v1/drug-allergies/patient/50").with(user("devops").roles("DEV")))
                .andExpect(status().isOk());
    }

    @Test
    void drugAllergy_match_returnsList() throws Exception {
        when(drugAllergyService.findMatching(eq(50L), eq("Penicillin"))).thenReturn(List.of());
        mockMvc.perform(get("/api/v1/drug-allergies/patient/50/match")
                        .with(user("devops").roles("DEV"))
                        .param("allergen", "Penicillin"))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------- drug interaction

    @Test
    void drugInteraction_endpoints_requireAuth() throws Exception {
        mockMvc.perform(post("/api/v1/drug-interactions").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/drug-interactions/patient/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void drugInteraction_record_returns201() throws Exception {
        DrugInteraction d = new DrugInteraction();
        d.setInteractionId(1L);
        d.setPatientId(50L);
        d.setDrug1Id(1L);
        d.setDrug2Id(2L);
        d.setInteractionType("DRUG_DRUG");
        d.setSeverity("MAJOR");
        d.setAcknowledged(false);
        d.setRecordedAt(Timestamp.from(Instant.now()));
        when(drugInteractionService.record(any(), any(), any(), any(), any(), any())).thenReturn(d);

        mockMvc.perform(post("/api/v1/drug-interactions").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":50,"drug1Id":1,"drug2Id":2,
                                 "interactionType":"DRUG_DRUG","severity":"MAJOR",
                                 "description":"increased bleeding risk"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.severity").value("MAJOR"))
                .andExpect(jsonPath("$.acknowledged").value(false));
    }

    @Test
    void drugInteraction_acknowledge_returns200() throws Exception {
        DrugInteraction d = new DrugInteraction();
        d.setInteractionId(1L);
        d.setAcknowledged(true);
        when(drugInteractionService.acknowledge(eq(1L), any())).thenReturn(d);
        mockMvc.perform(post("/api/v1/drug-interactions/1/acknowledge")
                        .with(csrf()).with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acknowledged").value(true));
    }

    @Test
    void drugInteraction_unacknowledged_list() throws Exception {
        when(drugInteractionService.unacknowledged(50L)).thenReturn(List.of());
        mockMvc.perform(get("/api/v1/drug-interactions/patient/50/unacknowledged")
                        .with(user("devops").roles("DEV")))
                .andExpect(status().isOk());
    }
}
