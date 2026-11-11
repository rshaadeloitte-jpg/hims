package com.hims.patient.api;

import com.hims.patient.application.exceptions.InvalidPatientRequestException;
import com.hims.patient.configuration.JwtConfig;
import com.hims.patient.configuration.SecurityConfig;
import com.hims.pmrd.api.PMRDController;
import com.hims.pmrd.application.PMRDService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves that in patient-service bundle, absorbed module pmrd controller is covered
 * by com.hims.patient.api.GlobalExceptionHandler (basePackages = {"com.hims.patient", "com.hims.pmrd"})
 * returning 400 for domain validation errors.
 */
@WebMvcTest(PMRDController.class)
@Import({
        PMRDController.class,
        SecurityConfig.class,
        JwtConfig.class,
        com.hims.patient.api.GlobalExceptionHandler.class
})
@AutoConfigureMockMvc
class PmrdAdviceScopeWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PMRDService pmrdService;

    @Test
    void getJsyForms_withDomainException_returns400FromScopedHandler() throws Exception {
        when(pmrdService.getJsyForms(any(), any()))
                .thenThrow(new InvalidPatientRequestException("BEXA003", "Invalid PMRD form request"));

        mockMvc.perform(get("/api/v1/pmrd/jsy-forms")
                        .with(user("tester")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errorCode").value("BEXA003"))
                .andExpect(jsonPath("$.message").value("Invalid PMRD form request"));
    }
}
