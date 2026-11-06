package com.hims.iam.api;

import com.hims.iam.api.dto.AuthDtos.LoginRequest;
import com.hims.iam.api.dto.AuthDtos.LoginResponse;
import com.hims.iam.api.dto.AuthDtos.ValidateResponse;
import com.hims.iam.application.AuthService;
import com.hims.iam.application.exceptions.InvalidCredentialsException;
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

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contract tests for /api/v1/auth/* (default-deny enforced; service + JWT filter mocked). */
@EnableConfigurationProperties(JwtProperties.class)
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenService.class,
        InMemoryTokenRevocationService.class})
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void login_isPublic_andReturnsTokenEnvelope() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(
                new LoginResponse("jwt-token", "Bearer", 1800L, 10L, "rsharma", List.of("staff")));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"rsharma\",\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(1800))
                .andExpect(jsonPath("$.roles[0]").value("staff"));
    }

    @Test
    void login_badCredentials_mapsTo401_withErrorCode() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException("IAM001", "Invalid username or password"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"rsharma\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("IAM001"))
                .andExpect(jsonPath("$.status").value("ERROR"));
    }

    @Test
    void login_missingFields_mapsTo400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"rsharma\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("IAM003"));
    }

    @Test
    void logout_requiresAuthentication_defaultDeny() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer some-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_withAuth_delegatesToService() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(user("devops").roles("DEV"))
                        .header("Authorization", "Bearer some-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void validate_isPublic_andEchoesValidity() throws Exception {
        when(authService.validate("good-token")).thenReturn(
                new ValidateResponse(true, "rsharma", 10L, List.of("admin"), Instant.now().plusSeconds(300), null));

        mockMvc.perform(post("/api/v1/auth/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"good-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.username").value("rsharma"));
    }

    @Test
    void changePassword_shortNewPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/change-password")
                        .with(user("rsharma").roles("STAFF"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"secret\",\"newPassword\":\"short\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changePassword_sameAsCurrent_returns400() throws Exception {
        // service-level rejection surfaces as 400 via InvalidRequestException
        org.mockito.Mockito.doThrow(new com.hims.iam.application.exceptions.InvalidRequestException("IAM008", "New password must differ from the current password"))
                .when(authService).changePassword(any(), anyString(), anyString());

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .with(user("rsharma").roles("STAFF"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"secret\",\"newPassword\":\"secret1234\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("IAM008"));
    }

    @Test
    void changePassword_anonymous_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"secret\",\"newPassword\":\"newpass12\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changePassword_bodyWithUsernameOrUserId_doesNotTargetOtherUser() throws Exception {
        mockMvc.perform(post("/api/v1/auth/change-password")
                        .with(user("rsharma").roles("STAFF"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"secret\",\"newPassword\":\"newpass12\",\"username\":\"victim\",\"userId\":999}"))
                .andExpect(status().isOk());

        org.mockito.Mockito.verify(authService).changePassword(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq("secret"),
                org.mockito.ArgumentMatchers.eq("newpass12"));
    }

    @Test
    void changePassword_wrongCurrent_returns401() throws Exception {
        org.mockito.Mockito.doThrow(new com.hims.iam.application.exceptions.InvalidCredentialsException("IAM001", "Invalid username or password"))
                .when(authService).changePassword(any(), anyString(), anyString());

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .with(user("rsharma").roles("STAFF"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"wrong\",\"newPassword\":\"newpass12\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("IAM001"));
    }

}
