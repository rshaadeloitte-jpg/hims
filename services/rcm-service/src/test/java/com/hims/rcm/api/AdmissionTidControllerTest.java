package com.hims.rcm.api;

import com.hims.rcm.application.TidService;
import com.hims.rcm.application.exceptions.InvalidRcmRequestException;
import com.hims.rcm.application.exceptions.RcmNotFoundException;
import com.hims.rcm.application.exceptions.TidConflictException;
import com.hims.rcm.configuration.SecurityConfig;
import com.hims.rcm.domain.model.AdmissionTid;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contract tests for the admission TID registry API (default-deny enforced; 409 on duplicates). */
@WebMvcTest(AdmissionTidController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class AdmissionTidControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TidService tidService;

    private static AdmissionTid tid(long admissionId, String value) {
        AdmissionTid t = new AdmissionTid();
        t.setTidId(500L);
        t.setAdmissionId(admissionId);
        t.setTid(value);
        return t;
    }

    @Test
    void endpoints_requireAuth_defaultDeny() throws Exception {
        mockMvc.perform(get("/api/v1/admissions/77/tid")).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/admissions/77/tid").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"tid\":\"X\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void assign_returnsRegisteredTid() throws Exception {
        when(tidService.assign(77L, "TID-2026-0001")).thenReturn(tid(77L, "TID-2026-0001"));

        mockMvc.perform(put("/api/v1/admissions/77/tid").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tid\":\"TID-2026-0001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tidId").value(500))
                .andExpect(jsonPath("$.admissionId").value(77))
                .andExpect(jsonPath("$.tid").value("TID-2026-0001"));
    }

    @Test
    void assign_duplicateAdmission_mapsTo409TidExistsEnvelope() throws Exception {
        when(tidService.assign(77L, "TID-OTHER"))
                .thenThrow(new TidConflictException("TID_EXISTS",
                        "Admission 77 already has an NHCX transaction id — reassignment rejected"));

        mockMvc.perform(put("/api/v1/admissions/77/tid").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tid\":\"TID-OTHER\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errorCode").value("TID_EXISTS"));
    }

    @Test
    void assign_blankTid_mapsTo400Bexa003() throws Exception {
        when(tidService.assign(eq(77L), eq("")))
                .thenThrow(new InvalidRcmRequestException("BEXA003", "tid is required"));

        mockMvc.perform(put("/api/v1/admissions/77/tid").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tid\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BEXA003"));
    }

    @Test
    void get_unknownAdmission_mapsTo404Envelope() throws Exception {
        when(tidService.get(404L))
                .thenThrow(new RcmNotFoundException("NOT_FOUND",
                        "No NHCX transaction id registered for admission 404"));

        mockMvc.perform(get("/api/v1/admissions/404/tid").with(csrf()).with(user("devops").roles("DEV")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    @Test
    void get_knownAdmission_returnsTid() throws Exception {
        when(tidService.get(77L)).thenReturn(tid(77L, "TID-2026-0001"));

        mockMvc.perform(get("/api/v1/admissions/77/tid").with(csrf()).with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tid").value("TID-2026-0001"));
    }
}
