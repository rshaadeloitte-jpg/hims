package com.hims.reference.api;

import com.hims.reference.application.CommonsService;
import com.hims.reference.configuration.JwtConfig;
import com.hims.reference.configuration.SecurityConfig;
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

@WebMvcTest(CommonsController.class)
@Import({SecurityConfig.class, JwtConfig.class})
@AutoConfigureMockMvc
class CommonsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommonsService commonsService;

    @Test
    void listConfig_ok() throws Exception {
        when(commonsService.listConfig(null)).thenReturn(List.of());
        mockMvc.perform(get("/api/v1/commons/config").with(user("tester")))
                .andExpect(status().isOk());
    }

    @Test
    void byEntity_ok() throws Exception {
        when(commonsService.findByEntity("Patient", 7L)).thenReturn(List.of());
        mockMvc.perform(get("/api/v1/commons/audit/by-entity")
                        .param("entityType", "Patient")
                        .param("entityId", "7")
                        .with(user("tester")))
                .andExpect(status().isOk());
    }

    @Test
    void byEntity_missingEntityType_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/commons/audit/by-entity")
                        .param("entityId", "7")
                        .with(user("tester")))
                .andExpect(status().isBadRequest());
    }
}
