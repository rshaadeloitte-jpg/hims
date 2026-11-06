package com.hims.iam.api;

import com.hims.iam.api.dto.AuthDtos.LoginResponse;
import com.hims.iam.application.AuthService;
import com.hims.iam.application.NotificationTemplateService;
import com.hims.iam.application.OtpService;
import com.hims.iam.application.PermissionService;
import com.hims.iam.application.RoleService;
import com.hims.iam.application.UserService;
import com.hims.iam.configuration.DevSecurityConfig;
import com.hims.iam.configuration.SecurityConfig;
import org.springframework.test.context.ActiveProfiles;
import com.hims.iam.security.InMemoryTokenRevocationService;
import com.hims.iam.security.JwtAuthenticationFilter;
import com.hims.iam.security.JwtProperties;
import com.hims.iam.security.JwtTokenService;
import com.hims.iam.security.TokenRevocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.nullValue;

@EnableConfigurationProperties(JwtProperties.class)
@WebMvcTest({UserController.class, RoleController.class, PermissionController.class,
        NotificationTemplateController.class, AuthController.class, OtpController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenService.class,
        InMemoryTokenRevocationService.class})
@AutoConfigureMockMvc
class IamSecurityAuthorizationTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenService jwtTokenService;
    @Autowired TokenRevocationService revocationService;

    @MockitoBean UserService userService;
    @MockitoBean RoleService roleService;
    @MockitoBean PermissionService permissionService;
    @MockitoBean NotificationTemplateService notificationTemplateService;
    @MockitoBean AuthService authService;
    @MockitoBean OtpService otpService;

    @BeforeEach
    void stubs() {
        when(userService.list(null, null)).thenReturn(List.of());
        when(authService.login(any())).thenReturn(
                new LoginResponse("jwt-token", "Bearer", 1800L, 10L, "rsharma", List.of("staff")));
        when(otpService.generate(any(), any())).thenReturn(
                new OtpService.OtpGenerateResult(1L, "user1", "SMS",
                        Timestamp.from(Instant.now().plusSeconds(300)), null));
    }

    private String adminToken() {
        return jwtTokenService.issue("admin", 1L, List.of("admin"), null);
    }

    private String staffToken() {
        return jwtTokenService.issue("staff", 2L, List.of("staff"), null);
    }

    @Test
    void anonymous_getUsers_401() throws Exception {
        mockMvc.perform(get("/api/v1/users")).andExpect(status().isUnauthorized());
    }

    @Test
    void anonymous_getRoles_401() throws Exception {
        mockMvc.perform(get("/api/v1/roles")).andExpect(status().isUnauthorized());
    }

    @Test
    void anonymous_getPermissions_401() throws Exception {
        mockMvc.perform(get("/api/v1/permissions")).andExpect(status().isUnauthorized());
    }

    @Test
    void anonymous_getNotificationTemplates_401() throws Exception {
        mockMvc.perform(get("/api/v1/notification-templates")).andExpect(status().isUnauthorized());
    }

    @Test
    void staff_getUsers_403() throws Exception {
        mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + staffToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void staff_getRoles_403() throws Exception {
        mockMvc.perform(get("/api/v1/roles").header("Authorization", "Bearer " + staffToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void staff_getPermissions_403() throws Exception {
        mockMvc.perform(get("/api/v1/permissions").header("Authorization", "Bearer " + staffToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void staff_getNotificationTemplates_403() throws Exception {
        mockMvc.perform(get("/api/v1/notification-templates").header("Authorization", "Bearer " + staffToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void staff_postUsers_403() throws Exception {
        mockMvc.perform(post("/api/v1/users").header("Authorization", "Bearer " + staffToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"x\",\"password\":\"Str0ngPass!x\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_getUsers_200() throws Exception {
        mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk());
    }

    @Test
    void login_anonymous_reachable() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"rsharma\",\"password\":\"secret\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void otpGenerate_anonymous_reachable_noDevOtp() throws Exception {
        mockMvc.perform(post("/api/v1/auth/otp/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"user1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.devOtp").doesNotExist());
    }

    @Test
    void revokedToken_401() throws Exception {
        String token = jwtTokenService.issue("revoked-user", 999L, List.of("staff"), null);
        revocationService.revoke(token, jwtTokenService.parse(token).getExpiration().toInstant());
        mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }
}

@ActiveProfiles("dev")
@EnableConfigurationProperties(JwtProperties.class)
@WebMvcTest({UserController.class, RoleController.class, PermissionController.class,
        NotificationTemplateController.class, AuthController.class, OtpController.class})
@Import({DevSecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenService.class,
        InMemoryTokenRevocationService.class})
@AutoConfigureMockMvc
class IamSecurityAuthorizationDevTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenService jwtTokenService;

    @MockitoBean UserService userService;
    @MockitoBean RoleService roleService;
    @MockitoBean PermissionService permissionService;
    @MockitoBean NotificationTemplateService notificationTemplateService;
    @MockitoBean AuthService authService;
    @MockitoBean OtpService otpService;

    @BeforeEach
    void stubs() {
        when(userService.list(null, null)).thenReturn(List.of());
        when(authService.login(any())).thenReturn(
                new LoginResponse("jwt-token", "Bearer", 1800L, 10L, "rsharma", List.of("staff")));
        when(otpService.generate(any(), any())).thenReturn(
                new OtpService.OtpGenerateResult(1L, "user1", "SMS",
                        Timestamp.from(Instant.now().plusSeconds(300)), null));
    }

    private String adminToken() {
        return jwtTokenService.issue("admin", 1L, List.of("admin"), null);
    }

    @Test
    void anonymous_getUsers_401() throws Exception {
        mockMvc.perform(get("/api/v1/users")).andExpect(status().isUnauthorized());
    }

    @Test
    void anonymous_getRoles_401() throws Exception {
        mockMvc.perform(get("/api/v1/roles")).andExpect(status().isUnauthorized());
    }

    @Test
    void anonymous_getPermissions_401() throws Exception {
        mockMvc.perform(get("/api/v1/permissions")).andExpect(status().isUnauthorized());
    }

    @Test
    void anonymous_getNotificationTemplates_401() throws Exception {
        mockMvc.perform(get("/api/v1/notification-templates")).andExpect(status().isUnauthorized());
    }

    @Test
    void admin_getUsers_200() throws Exception {
        mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk());
    }
}