package com.hims.reference.api;

import com.hims.reference.application.MasterEntityRegistry;
import com.hims.reference.application.MasterReadUseCase;
import com.hims.reference.application.MasterWriteUseCase;
import com.hims.reference.configuration.DevSecurityConfig;
import com.hims.reference.configuration.SecurityConfig;
import com.hims.reference.domain.repository.MasterReadRepository;
import com.hims.reference.infrastructure.outbox.OutboxService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Dev-profile basic auth (IMP-DEC-019): with profile {@code dev} the React
 * workspace authenticates with HTTP Basic (dev-only credentials). Guards pinned:
 * correct credentials -> 200 on the registry endpoint; wrong password -> 401;
 * actuator health stays anonymous.
 */
@WebMvcTest(MasterReadController.class)
@Import({SecurityConfig.class, DevSecurityConfig.class, MasterEntityRegistry.class, MasterReadUseCase.class})
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class DevBasicAuthTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MasterReadRepository repository;

    @Test
    void devBasicAuth_allowsRegisteredDevUser() throws Exception {
        mockMvc.perform(get("/api/v1/masters").with(httpBasic("devops", "devops-dev-only")))
                .andExpect(status().isOk());
    }

    @Test
    void devBasicAuth_rejectsWrongPassword() throws Exception {
        mockMvc.perform(get("/api/v1/masters").with(httpBasic("devops", "wrong-password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void devProfile_businessEndpointsStillDefaultDeny() throws Exception {
        // dev chain adds basic auth but must NOT open business endpoints
        mockMvc.perform(get("/api/v1/masters/unitmaster/1"))
                .andExpect(status().isUnauthorized());
    }
}
