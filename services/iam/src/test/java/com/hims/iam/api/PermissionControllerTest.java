package com.hims.iam.api;

import com.hims.iam.application.PermissionService;
import com.hims.iam.configuration.SecurityConfig;
import com.hims.iam.domain.model.ModulePermission;
import com.hims.iam.security.InMemoryTokenRevocationService;
import com.hims.iam.security.JwtAuthenticationFilter;
import com.hims.iam.security.JwtProperties;
import com.hims.iam.security.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EnableConfigurationProperties(JwtProperties.class)
@WebMvcTest(PermissionController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenService.class, InMemoryTokenRevocationService.class})
@AutoConfigureMockMvc
class PermissionControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean PermissionService permissionService;

    @Test
    void list_requiresAuth() throws Exception {
        mockMvc.perform(get("/api/v1/permissions")).andExpect(status().isUnauthorized());
    }

    @Test
    void create_valid_returns201() throws Exception {
        ModulePermission p = perm(1L, "LAB_READ", "lab");
        when(permissionService.createPermission(eq("LAB_READ"), eq("lab"), any())).thenReturn(p);
        mockMvc.perform(post("/api/v1/permissions").with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"LAB_READ\",\"module\":\"lab\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("LAB_READ"));
    }

    @Test
    void setRolePermissions_returnsIds() throws Exception {
        var rp = new com.hims.iam.domain.model.RolePermission();
        rp.setRoleId(1L); rp.setEnabledPermissionIds("[1,2]");
        when(permissionService.setRolePermissions(eq(1L), any())).thenReturn(rp);
        mockMvc.perform(put("/api/v1/roles/1/permissions").with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"permissionIds\":[1,2]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabledPermissionIds").value("[1,2]"));
    }

    @Test
    void getRolePermissions_returnsSet() throws Exception {
        when(permissionService.getRolePermissions(1L)).thenReturn(Set.of(1L, 2L));
        mockMvc.perform(get("/api/v1/roles/1/permissions").with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getUserPermissions_returnsEffectiveUnion() throws Exception {
        when(permissionService.getUserEffectivePermissions(5L)).thenReturn(Set.of(1L));
        mockMvc.perform(get("/api/v1/users/5/permissions").with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    private static ModulePermission perm(long id, String code, String module) {
        ModulePermission p = new ModulePermission();
        p.setId(id); p.setCode(code); p.setModule(module); p.setStatus(true);
        return p;
    }
}
