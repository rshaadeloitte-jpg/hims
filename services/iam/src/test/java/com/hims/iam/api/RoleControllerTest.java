package com.hims.iam.api;

import com.hims.iam.api.dto.RoleDtos.CreateRoleRequest;
import com.hims.iam.api.dto.RoleDtos.RoleView;
import com.hims.iam.application.RoleService;
import com.hims.iam.configuration.SecurityConfig;
import com.hims.iam.security.InMemoryTokenRevocationService;
import com.hims.iam.security.JwtAuthenticationFilter;
import com.hims.iam.security.JwtProperties;
import com.hims.iam.security.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contract tests for /api/v1/roles (default-deny enforced). */
@EnableConfigurationProperties(JwtProperties.class)
@WebMvcTest(RoleController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenService.class,
        InMemoryTokenRevocationService.class})
@AutoConfigureMockMvc
class RoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoleService roleService;

    @Test
    void listRoles_requiresAuth_defaultDeny() throws Exception {
        mockMvc.perform(get("/api/v1/roles"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listRoles_returnsCatalogue() throws Exception {
        when(roleService.list()).thenReturn(List.of(
                new RoleView(1L, "admin", "Administrative operations", null, true),
                new RoleView(2L, "staff", "Staff/provider access", null, true)));

        mockMvc.perform(get("/api/v1/roles").with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("admin"))
                .andExpect(jsonPath("$[1].code").value("staff"));
    }

    @Test
    void createRole_returns201() throws Exception {
        when(roleService.create(any(CreateRoleRequest.class))).thenReturn(
                new RoleView(4L, "nurse", "Nursing staff", 3L, true));

        mockMvc.perform(post("/api/v1/roles")
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"nurse\",\"description\":\"Nursing staff\",\"unitId\":3}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.code").value("nurse"));
    }
}
