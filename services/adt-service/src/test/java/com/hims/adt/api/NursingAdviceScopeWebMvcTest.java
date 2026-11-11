package com.hims.adt.api;

import com.hims.adt.configuration.JwtConfig;
import com.hims.adt.configuration.SecurityConfig;
import com.hims.nursing.api.NursingController;
import com.hims.nursing.application.NursingChartingService;
import com.hims.nursing.application.exceptions.InvalidNursingRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves that in adt-service bundle, absorbed module nursing's domain exception
 * is handled by com.hims.nursing.api.GlobalExceptionHandler (400) and NOT swallowed
 * into 500 by com.hims.adt.api.GlobalExceptionHandler.
 */
@WebMvcTest(NursingController.class)
@Import({
        NursingController.class,
        SecurityConfig.class,
        JwtConfig.class,
        com.hims.adt.api.GlobalExceptionHandler.class,
        com.hims.nursing.api.GlobalExceptionHandler.class
})
@AutoConfigureMockMvc
class NursingAdviceScopeWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NursingChartingService service;

    @Test
    void postVitals_withMissingValue_returns400_not500() throws Exception {
        when(service.recordVital(any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new InvalidNursingRequestException("BEXA003", "value is required and must be numeric"));

        mockMvc.perform(post("/api/v1/patients/501/vitals")
                        .with(user("nurse"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vitalsType\":\"Heart Rate\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errorCode").value("BEXA003"))
                .andExpect(jsonPath("$.message").value("BEXA003: value is required and must be numeric"));
    }
}
