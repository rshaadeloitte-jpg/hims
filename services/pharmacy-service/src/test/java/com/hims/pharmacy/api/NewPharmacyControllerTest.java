package com.hims.pharmacy.api;

import com.hims.pharmacy.application.BcmaService;
import com.hims.pharmacy.application.IvCompoundService;
import com.hims.pharmacy.application.HomeMedicationService;
import com.hims.pharmacy.application.exceptions.BcmaVerificationException;
import com.hims.pharmacy.application.exceptions.CompoundNotFoundException;
import com.hims.pharmacy.application.exceptions.HomeMedicationNotFoundException;
import com.hims.pharmacy.configuration.SecurityConfig;
import com.hims.pharmacy.domain.model.BcmaVerification;
import com.hims.pharmacy.domain.model.IvCompound;
import com.hims.pharmacy.domain.model.HomeMedication;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contract tests for the new pharmacy BCMA + IV compound + home medication APIs (default-deny enforced). */
@WebMvcTest({BcmaController.class, IvCompoundController.class, HomeMedicationController.class})
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class NewPharmacyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BcmaService bcmaService;

    @MockitoBean
    private IvCompoundService ivCompoundService;

    @MockitoBean
    private HomeMedicationService homeMedicationService;

    // BCMA

    @Test
    void bcma_recordScan_requiresAuth() throws Exception {
        mockMvc.perform(post("/api/v1/bcma").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bcma_recordScan_returnsCreated() throws Exception {
        BcmaVerification v = new BcmaVerification();
        v.setBcmaId(1L);
        v.setPatientId(100L);
        v.setSaleId(50L);
        v.setRightPatient(true);
        v.setRightDrug(true);
        v.setRightDose(true);
        v.setRightRoute(true);
        v.setRightTime(true);
        v.setAllRightsPassed(true);
        when(bcmaService.recordScan(anyLong(), anyLong(), anyLong(), anyLong(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(), anyLong()))
                .thenReturn(v);

        mockMvc.perform(post("/api/v1/bcma").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":100,"saleId":50,"detailId":10,"encounterId":200,
                                 "rightPatient":true,"rightDrug":true,"rightDose":true,"rightRoute":true,"rightTime":true,"userId":1}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bcmaId").value(1))
                .andExpect(jsonPath("$.allRightsPassed").value(true));
    }

    @Test
    void bcma_listForPatient_returnsRecords() throws Exception {
        BcmaVerification v = new BcmaVerification();
        v.setBcmaId(1L);
        v.setPatientId(100L);
        when(bcmaService.listForPatient(100L)).thenReturn(List.of(v));

        mockMvc.perform(get("/api/v1/bcma/patient/100").with(csrf()).with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bcmaId").value(1));
    }

    @Test
    void bcma_listPendingOverrides_returnsRecords() throws Exception {
        BcmaVerification v = new BcmaVerification();
        v.setBcmaId(1L);
        when(bcmaService.listPendingOverrides()).thenReturn(List.of(v));

        mockMvc.perform(get("/api/v1/bcma/pending-overrides").with(csrf()).with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bcmaId").value(1));
    }

    // IV compound

    @Test
    void ivCompound_listByStatus_returnsRecords() throws Exception {
        IvCompound c = new IvCompound();
        c.setCompoundId(1L);
        c.setStatusCode(IvCompound.STATUS_PENDING);
        when(ivCompoundService.listByStatus((short) 1)).thenReturn(List.of(c));

        mockMvc.perform(get("/api/v1/iv-compounds").with(csrf()).with(user("devops").roles("DEV")).param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].compoundId").value(1));
    }

    // Home medication

    @Test
    void homeMed_listForPatient_returnsRecords() throws Exception {
        HomeMedication hm = new HomeMedication();
        hm.setHomeMedId(1L);
        hm.setPatientId(100L);
        when(homeMedicationService.listForPatient(100L)).thenReturn(List.of(hm));

        mockMvc.perform(get("/api/v1/home-medications/patient/100").with(csrf()).with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].homeMedId").value(1));
    }

    @Test
    void homeMed_listPendingDispense_returnsRecords() throws Exception {
        HomeMedication hm = new HomeMedication();
        hm.setHomeMedId(1L);
        when(homeMedicationService.listPendingDispense()).thenReturn(List.of(hm));

        mockMvc.perform(get("/api/v1/home-medications/pending-dispense").with(csrf()).with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].homeMedId").value(1));
    }
}
