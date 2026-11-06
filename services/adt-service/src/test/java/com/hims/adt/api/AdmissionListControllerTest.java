package com.hims.adt.api;

import com.hims.adt.application.AdmissionService;
import com.hims.adt.configuration.JwtConfig;
import com.hims.adt.configuration.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdmissionController.class)
@Import({SecurityConfig.class, JwtConfig.class})
@AutoConfigureMockMvc
class AdmissionListControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdmissionService admissionService;

    @Test
    void activeTrue_ok() throws Exception {
        when(admissionService.search(null, true)).thenReturn(List.of());
        mockMvc.perform(get("/api/v1/admissions")
                        .param("active", "true")
                        .with(user("tester")))
                .andExpect(status().isOk());
    }

    @Test
    void missingParams_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/admissions")
                        .with(user("tester")))
                .andExpect(status().isBadRequest());
    }
}
