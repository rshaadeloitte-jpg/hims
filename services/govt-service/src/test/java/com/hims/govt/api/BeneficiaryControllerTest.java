package com.hims.govt.api;

import com.hims.govt.application.BeneficiaryService;
import com.hims.govt.application.exceptions.GovtConflictException;
import com.hims.govt.application.exceptions.GovtNotFoundException;
import com.hims.govt.application.exceptions.InvalidGovtRequestException;
import com.hims.govt.configuration.SecurityConfig;
import com.hims.govt.domain.model.BeneficiaryEnrolment;
import com.hims.govt.domain.model.ReferenceTid;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contract tests for the Beneficiary API (default-deny enforced). Services mocked; L0 covers the rules. */
@WebMvcTest(BeneficiaryController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class BeneficiaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BeneficiaryService beneficiaryService;

    private static BeneficiaryEnrolment enrolment(long id) {
        BeneficiaryEnrolment e = new BeneficiaryEnrolment();
        e.setEnrolmentId(id);
        e.setPatientId(11L);
        e.setScheme("CHIRANJEEVI");
        e.setEnrolmentNo("CH-001");
        e.setEnrolledOn(LocalDate.of(2025, 1, 15));
        e.setIsActive(true);
        return e;
    }

    @Test
    void endpoints_requireAuth_defaultDeny() throws Exception {
        mockMvc.perform(post("/api/v1/beneficiaries/enrol").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/beneficiaries/CH-001/tid").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"tid\":\"T\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void enrol_valid_returns201WithChiranjeeviScheme() throws Exception {
        when(beneficiaryService.enrol(eq(11L), eq("CHIRANJEEVI"), eq("CH-001"),
                eq(LocalDate.of(2025, 1, 15)))).thenReturn(enrolment(5L));

        mockMvc.perform(post("/api/v1/beneficiaries/enrol").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":11,"scheme":"CHIRANJEEVI","enrolmentNo":"CH-001",
                                 "enrolledOn":"2025-01-15"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.enrolmentId").value(5))
                .andExpect(jsonPath("$.scheme").value("CHIRANJEEVI"))
                .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    void enrol_badScheme_mapsTo400Bexa003Envelope() throws Exception {
        when(beneficiaryService.enrol(any(), any(), any(), any()))
                .thenThrow(new InvalidGovtRequestException("BEXA003", "scheme must be CHIRANJEEVI — got: OTHER"));

        mockMvc.perform(post("/api/v1/beneficiaries/enrol").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":11,\"scheme\":\"OTHER\",\"enrolmentNo\":\"X\",\"enrolledOn\":\"2025-01-15\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errorCode").value("BEXA003"));
    }

    @Test
    void enrol_duplicate_mapsTo409Envelope() throws Exception {
        when(beneficiaryService.enrol(any(), any(), any(), any()))
                .thenThrow(new GovtConflictException("GOVT_DUPLICATE_ENROLMENT_NO",
                        "enrolmentNo already exists: CH-001"));

        mockMvc.perform(post("/api/v1/beneficiaries/enrol").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":11,\"scheme\":\"CHIRANJEEVI\",\"enrolmentNo\":\"CH-001\",\"enrolledOn\":\"2025-01-15\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("GOVT_DUPLICATE_ENROLMENT_NO"))
                .andExpect(jsonPath("$.status").value("ERROR"));
    }

    @Test
    void linkTid_valid_returns201() throws Exception {
        ReferenceTid link = new ReferenceTid();
        link.setTidId(9L);
        link.setEnrolmentId(5L);
        link.setTid("TID-1");
        when(beneficiaryService.linkTid("CH-001", "TID-1")).thenReturn(link);

        mockMvc.perform(put("/api/v1/beneficiaries/CH-001/tid").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"tid\":\"TID-1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tidId").value(9))
                .andExpect(jsonPath("$.tid").value("TID-1"));
    }

    @Test
    void linkTid_unknownEnrolmentNo_mapsTo404_andDupTidTo409() throws Exception {
        when(beneficiaryService.linkTid("GHOST", "TID-2"))
                .thenThrow(new GovtNotFoundException("NOT_FOUND", "Enrolment not found: GHOST"));

        mockMvc.perform(put("/api/v1/beneficiaries/GHOST/tid").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"tid\":\"TID-2\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }
}
