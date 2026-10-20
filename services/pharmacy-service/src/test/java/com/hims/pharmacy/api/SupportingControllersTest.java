package com.hims.pharmacy.api;

import com.hims.pharmacy.application.DrugInterventionService;
import com.hims.pharmacy.application.PrescriptionDispenseService;
import com.hims.pharmacy.application.WardPicklistService;
import com.hims.pharmacy.application.exceptions.InvalidSaleRequestException;
import com.hims.pharmacy.application.exceptions.SaleNotFoundException;
import com.hims.pharmacy.configuration.SecurityConfig;
import com.hims.pharmacy.domain.model.DrugIntervention;
import com.hims.pharmacy.domain.model.PrescriptionDispense;
import com.hims.pharmacy.domain.model.WardPicklist;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contract tests for the supporting WP-R2 APIs — drug interventions, ward picklists,
 * prescription projection EMR stamping (default-deny enforced). Services mocked.
 */
@WebMvcTest(controllers = {InterventionController.class, PicklistController.class,
        PrescriptionProjectionController.class})
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class SupportingControllersTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DrugInterventionService drugInterventionService;

    @MockitoBean
    private WardPicklistService wardPicklistService;

    @MockitoBean
    private PrescriptionDispenseService prescriptionDispenseService;

    @Test
    void endpoints_requireAuth_defaultDeny() throws Exception {
        mockMvc.perform(post("/api/v1/interventions").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/interventions").param("patientId", "1"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/picklists").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/prescriptions/555/emr-no").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------ interventions

    @Test
    void intervention_create_valid_returns201() throws Exception {
        DrugIntervention saved = new DrugIntervention();
        saved.setInterventionId(7L);
        saved.setPatientId(505L);
        saved.setSaleId(10L);
        saved.setInterventionType("DOSAGE_ADJUSTED");
        saved.setNotes("renal impairment");
        saved.setPharmacistId(9L);
        when(drugInterventionService.create(505L, 10L, "DOSAGE_ADJUSTED",
                "renal impairment", 9L)).thenReturn(saved);

        mockMvc.perform(post("/api/v1/interventions").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":505,"saleId":10,"interventionType":"DOSAGE_ADJUSTED",
                                 "notes":"renal impairment","pharmacistId":9}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.interventionId").value(7))
                .andExpect(jsonPath("$.interventionType").value("DOSAGE_ADJUSTED"));
    }

    @Test
    void intervention_missingType_mapsTo400Bexa003() throws Exception {
        when(drugInterventionService.create(any(), any(), any(), any(), any()))
                .thenThrow(new InvalidSaleRequestException("BEXA003", "interventionType is required"));

        mockMvc.perform(post("/api/v1/interventions").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":505,\"pharmacistId\":9}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BEXA003"));
    }

    @Test
    void intervention_list_byPatient() throws Exception {
        DrugIntervention saved = new DrugIntervention();
        saved.setInterventionId(7L);
        saved.setPatientId(505L);
        saved.setInterventionType("DUP_THERAPY");
        saved.setPharmacistId(9L);
        when(drugInterventionService.listByPatient(505L)).thenReturn(List.of(saved));

        mockMvc.perform(get("/api/v1/interventions").with(csrf()).with(user("devops").roles("DEV"))
                        .param("patientId", "505"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].interventionId").value(7));
    }

    // --------------------------------------------------------------- picklists

    @Test
    void picklist_create_valid_returns201_withServerStampedPickedAt() throws Exception {
        WardPicklist saved = new WardPicklist();
        saved.setPicklistId(3L);
        saved.setWardId(12L);
        saved.setSaleId(10L);
        saved.setPickedAt(Timestamp.valueOf("2026-08-23 10:15:00"));
        when(wardPicklistService.create(12L, 10L)).thenReturn(saved);

        mockMvc.perform(post("/api/v1/picklists").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"wardId\":12,\"saleId\":10}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.picklistId").value(3))
                .andExpect(jsonPath("$.wardId").value(12))
                .andExpect(jsonPath("$.pickedAt").exists());
    }

    @Test
    void picklist_missingSaleId_mapsTo400_and_unknownSale_mapsTo404() throws Exception {
        when(wardPicklistService.create(eq(12L), eq((Long) null)))
                .thenThrow(new InvalidSaleRequestException("BEXA003", "saleId is required"));
        when(wardPicklistService.create(eq(12L), eq(999L)))
                .thenThrow(new SaleNotFoundException("NOT_FOUND", "Item sale not found: 999"));

        mockMvc.perform(post("/api/v1/picklists").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"wardId\":12}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BEXA003"));

        mockMvc.perform(post("/api/v1/picklists").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"wardId\":12,\"saleId\":999}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    // ------------------------------------------- prescription projection EMR no

    @Test
    void emrNo_stamp_returnsProjectionMaster() throws Exception {
        PrescriptionDispense master = new PrescriptionDispense();
        master.setDispenseId(21L);
        master.setPrescriptionId(555L);
        master.setEmrNo("EMR-88");
        master.setClosed(false);
        when(prescriptionDispenseService.setEmrNo(555L, "EMR-88")).thenReturn(master);

        mockMvc.perform(put("/api/v1/prescriptions/555/emr-no")
                        .with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"emrNo\":\"EMR-88\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prescriptionId").value(555))
                .andExpect(jsonPath("$.emrNo").value("EMR-88"));
    }

    @Test
    void emrNo_unknownPrescription_mapsTo404() throws Exception {
        when(prescriptionDispenseService.setEmrNo(eq(404L), any()))
                .thenThrow(new SaleNotFoundException("NOT_FOUND",
                        "No pharmacy dispensing record exists for prescription 404"));

        mockMvc.perform(put("/api/v1/prescriptions/404/emr-no")
                        .with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"emrNo\":\"EMR-X\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }
}
