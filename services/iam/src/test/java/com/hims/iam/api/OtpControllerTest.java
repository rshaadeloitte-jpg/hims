package com.hims.iam.api;

import com.hims.iam.api.dto.AuthDtos.LoginResponse;
import com.hims.iam.application.OtpService;
import com.hims.iam.configuration.SecurityConfig;
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

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EnableConfigurationProperties(JwtProperties.class)
@WebMvcTest(OtpController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenService.class, InMemoryTokenRevocationService.class})
@AutoConfigureMockMvc
class OtpControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean OtpService otpService;

    @Test
    void generate_requiresAuth() throws Exception {
        mockMvc.perform(post("/api/v1/auth/otp/generate")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"identifier\":\"user1\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void generate_valid_returnsIdAndDevOtp_whenDev() throws Exception {
        var r = new OtpService.OtpGenerateResult(1L, "user1", "SMS", Timestamp.from(Instant.now().plusSeconds(300)), "123456");
        when(otpService.generate(eq("user1"), any())).thenReturn(r);
        mockMvc.perform(post("/api/v1/auth/otp/generate").with(user("devops").roles("DEV")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"user1\",\"channel\":\"SMS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identifier").value("user1"))
                .andExpect(jsonPath("$.devOtp").value("123456"));
    }

    @Test
    void verify_valid_issuesJwt() throws Exception {
        when(otpService.verify(eq("user1"), eq("123456")))
                .thenReturn(new LoginResponse("tok", "Bearer", 1800, 1L, "user1", List.of("staff")));
        mockMvc.perform(post("/api/v1/auth/otp/verify").with(user("devops").roles("DEV")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"user1\",\"otp\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("tok"))
                .andExpect(jsonPath("$.username").value("user1"));
    }
}
