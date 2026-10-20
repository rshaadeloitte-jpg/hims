package com.hims.patient.api;

import com.hims.patient.application.PatientRegistrationService;
import com.hims.patient.application.exceptions.InvalidPatientRequestException;
import com.hims.patient.configuration.SecurityConfig;
import com.hims.patient.domain.model.PatientRegistration;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** L1 — REST contract: default-deny 401, status codes, JSON shape, legacy error codes. */
@WebMvcTest(PatientController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class PatientControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private PatientRegistrationService service;

    private static PatientRegistration patient(long id) {
        PatientRegistration p = new PatientRegistration();
        p.setId(id);
        p.setMrno("22082600000000" + id);
        p.setFirstName("Ramesh");
        p.setGender("M");
        return p;
    }

    @Test
    void businessEndpoints_areDefaultDenied() throws Exception {
        mockMvc.perform(post("/api/v1/patients/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"X\",\"gender\":\"M\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/patients/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_returns201WithIdAndMrno() throws Exception {
        Mockito.when(service.register(any(), org.mockito.ArgumentMatchers.eq(7L))).thenReturn(patient(3L));
        mockMvc.perform(post("/api/v1/patients/register?unitId=7")
                        .with(SecurityMockMvcRequestPostProcessors.user("devops"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Ramesh\",\"gender\":\"M\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.mrno").value("220826000000003"));
    }

    @Test
    void duplicateErrors_mapToLegacyCodes_not500() throws Exception {
        Mockito.when(service.register(any(), any()))
                .thenThrow(new InvalidPatientRequestException("BERA011", "Duplicate patient identity"),
                        new InvalidPatientRequestException("BEXA040", "ABHA already registered for MRN M5"));

        mockMvc.perform(post("/api/v1/patients/register")
                        .with(SecurityMockMvcRequestPostProcessors.user("devops"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BERA011"));

        mockMvc.perform(post("/api/v1/patients/register")
                        .with(SecurityMockMvcRequestPostProcessors.user("devops"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BEXA040"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("M5")));
    }

    @Test
    void searchAndGet_contract() throws Exception {
        Mockito.when(service.search(null, "111122223333", null)).thenReturn(java.util.List.of(patient(9L)));
        mockMvc.perform(get("/api/v1/patients/search?aadhaar=111122223333")
                        .with(SecurityMockMvcRequestPostProcessors.user("devops")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(9));

        Mockito.when(service.get(9L)).thenReturn(patient(9L));
        mockMvc.perform(get("/api/v1/patients/9").with(SecurityMockMvcRequestPostProcessors.user("devops")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9));
    }

    @Test
    void update_delegates() throws Exception {
        Mockito.when(service.update(org.mockito.ArgumentMatchers.eq(9L), any())).thenReturn(patient(9L));
        mockMvc.perform(put("/api/v1/patients/9")
                        .with(SecurityMockMvcRequestPostProcessors.user("devops"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"city\":\"Jaipur\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9));
    }

    @Test
    void documents_linkAndList_contract() throws Exception {
        com.hims.patient.domain.model.PatientDocument d = new com.hims.patient.domain.model.PatientDocument();
        d.setId(500L);
        d.setPatientId(9L);
        d.setDocType("AADHAAR");
        d.setDocRef("obj/pat/9/aadhaar.pdf");
        Mockito.when(service.linkDocument(org.mockito.ArgumentMatchers.eq(9L), any())).thenReturn(d);
        Mockito.when(service.documents(9L)).thenReturn(java.util.List.of(d));

        mockMvc.perform(post("/api/v1/patients/9/documents")
                        .with(SecurityMockMvcRequestPostProcessors.user("devops"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"docType\":\"AADHAAR\",\"docRef\":\"obj/pat/9/aadhaar.pdf\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.docType").value("AADHAAR"));

        mockMvc.perform(get("/api/v1/patients/9/documents")
                        .with(SecurityMockMvcRequestPostProcessors.user("devops")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(500));
    }

    @Test
    void sponsors_linkAndList_contract() throws Exception {
        com.hims.patient.domain.model.PatientSponsor s = new com.hims.patient.domain.model.PatientSponsor();
        s.setId(700L);
        s.setPatientId(9L);
        s.setSponsorType("RGHS");
        Mockito.when(service.linkSponsor(org.mockito.ArgumentMatchers.eq(9L), any())).thenReturn(s);
        Mockito.when(service.sponsors(9L)).thenReturn(java.util.List.of(s));

        mockMvc.perform(post("/api/v1/patients/9/sponsors")
                        .with(SecurityMockMvcRequestPostProcessors.user("devops"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sponsorType\":\"RGHS\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sponsorType").value("RGHS"));

        mockMvc.perform(get("/api/v1/patients/9/sponsors")
                        .with(SecurityMockMvcRequestPostProcessors.user("devops")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(700));
    }

    @Test
    void merge_contract_andBadRequest() throws Exception {
        Mockito.when(service.merge(12L, 20L)).thenReturn(
                com.hims.patient.api.PatientMergeResult.of(12L, "M12", 20L, "M20", 2, 1));
        mockMvc.perform(post("/api/v1/patients/merge")
                        .with(SecurityMockMvcRequestPostProcessors.user("devops"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceId\":12,\"targetId\":20}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetId").value(20))
                .andExpect(jsonPath("$.documentsRepointed").value(2))
                .andExpect(jsonPath("$.sponsorsRepointed").value(1));

        Mockito.when(service.merge(org.mockito.ArgumentMatchers.eq(5L), org.mockito.ArgumentMatchers.eq(5L)))
                .thenThrow(new InvalidPatientRequestException("BEXA003", "sourceId and targetId must differ"));
        mockMvc.perform(post("/api/v1/patients/merge")
                        .with(SecurityMockMvcRequestPostProcessors.user("devops"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceId\":5,\"targetId\":5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BEXA003"));
    }

    @Test
    void unknownPath_is404NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/patients/nope/nope")
                        .with(SecurityMockMvcRequestPostProcessors.user("devops")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    @Test
    void m01_aadhaarOtp_andCovid_andSocial_contract() throws Exception {
        com.hims.patient.domain.model.PatientAadhaarVerification v = new com.hims.patient.domain.model.PatientAadhaarVerification();
        v.setId(1000L);
        v.setPatientId(9L);
        v.setAadhaarNo("234567890123");
        v.setVerificationType("OTP");
        v.setOtp("123456");
        v.setVerified(false);
        Mockito.when(service.requestAadhaarOtp(org.mockito.ArgumentMatchers.eq(9L), any())).thenReturn(v);
        mockMvc.perform(post("/api/v1/patients/9/aadhaar/otp")
                        .with(SecurityMockMvcRequestPostProcessors.user("devops"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aadhaarNo\":\"234567890123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.aadhaarNo").value("234567890123"));

        com.hims.patient.domain.model.PatientRegistration p = patient(9L);
        p.setCovidStatus("POSITIVE");
        Mockito.when(service.updateCovidStatus(org.mockito.ArgumentMatchers.eq(9L), any())).thenReturn(p);
        mockMvc.perform(post("/api/v1/patients/9/covid-status")
                        .with(SecurityMockMvcRequestPostProcessors.user("devops"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"covidStatus\":\"POSITIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.covidStatus").value("POSITIVE"));

        Mockito.when(service.listCovidPatients(org.mockito.ArgumentMatchers.any())).thenReturn(java.util.List.of(p));
        mockMvc.perform(get("/api/v1/patients?covid=true")
                        .with(SecurityMockMvcRequestPostProcessors.user("devops")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].covidStatus").value("POSITIVE"));

        com.hims.patient.domain.model.PatientSocialAssessment a = new com.hims.patient.domain.model.PatientSocialAssessment();
        a.setId(2000L);
        a.setPatientId(9L);
        a.setAssessmentData(java.util.Map.of("housing", "kutcha"));
        Mockito.when(service.createSocialAssessment(org.mockito.ArgumentMatchers.eq(9L), any())).thenReturn(a);
        mockMvc.perform(post("/api/v1/patients/9/social-worker-assessment")
                        .with(SecurityMockMvcRequestPostProcessors.user("devops"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assessmentData\":{\"housing\":\"kutcha\"}}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patientId").value(9));
    }
}
