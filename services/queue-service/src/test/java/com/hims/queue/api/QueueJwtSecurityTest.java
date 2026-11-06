package com.hims.queue.api;

import com.hims.queue.application.QueueService;
import com.hims.queue.configuration.DevSecurityConfig;
import com.hims.queue.configuration.JwtConfig;
import com.hims.queue.configuration.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({QueueTokenController.class, QueueCounterController.class})
@Import({SecurityConfig.class, JwtConfig.class})
@AutoConfigureMockMvc
class QueueJwtSecurityTest {

    private static final String SECRET = "test-only-secret-key-0123456789abcdef0123456789abcdef";
    private static final String OTHER_SECRET = "another-test-secret-key-0123456789abcdef0123456789abcdef";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QueueService service;

    private String token(String secret, String issuer, List<String> roles, Instant expiry) throws Exception {
        MACSigner signer = new MACSigner(secret.getBytes(StandardCharsets.UTF_8));
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject("tester")
                .claim("roles", roles)
                .issueTime(Date.from(Instant.now()))
                .expirationTime(Date.from(expiry))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(signer);
        return jwt.serialize();
    }

    @Test
    void validToken_returns200OnBusinessEndpoint() throws Exception {
        when(service.dashboard(null)).thenReturn(Map.of("waiting", 0L));
        String t = token(SECRET, "iam-service", List.of("staff"), Instant.now().plusSeconds(300));
        mockMvc.perform(get("/api/v1/queue/dashboard")
                        .header("Authorization", "Bearer " + t))
                .andExpect(status().isOk());
    }

    @Test
    void expiredToken_returns401() throws Exception {
        String t = token(SECRET, "iam-service", List.of("staff"), Instant.now().minusSeconds(300));
        mockMvc.perform(get("/api/v1/queue/dashboard")
                        .header("Authorization", "Bearer " + t))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongSignature_returns401() throws Exception {
        String t = token(OTHER_SECRET, "iam-service", List.of("staff"), Instant.now().plusSeconds(300));
        mockMvc.perform(get("/api/v1/queue/dashboard")
                        .header("Authorization", "Bearer " + t))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongIssuer_returns401() throws Exception {
        String t = token(SECRET, "evil-issuer", List.of("staff"), Instant.now().plusSeconds(300));
        mockMvc.perform(get("/api/v1/queue/dashboard")
                        .header("Authorization", "Bearer " + t))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/queue/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nonAdminToken_returns403OnAdmin() throws Exception {
        String t = token(SECRET, "iam-service", List.of("staff"), Instant.now().plusSeconds(300));
        mockMvc.perform(get("/admin/foo")
                        .header("Authorization", "Bearer " + t))
                .andExpect(status().isForbidden());
    }

    @Test
    void decoderRefusesCommittedDefaultSecret() {
        JwtConfig config = new JwtConfig();
        assertThatThrownBy(() -> config.jwtDecoder("dev-only-secret-key-change-me-0123456789abcdef"))
                .isInstanceOf(IllegalStateException.class);
    }
}

@WebMvcTest({QueueTokenController.class, QueueCounterController.class})
@ActiveProfiles("dev")
@Import({DevSecurityConfig.class, JwtConfig.class, QueueDevBasicSecurityTest.TestUsers.class})
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.security.user.name=devops",
        "spring.security.user.password=devops-dev-only",
        "spring.security.user.roles=DEV"
})
class QueueDevBasicSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QueueService service;

    @TestConfiguration
    static class TestUsers {
        @Bean
        UserDetailsService userDetailsService() {
            UserDetails u = User.withUsername("devops").password("{noop}devops-dev-only").roles("DEV").build();
            return new InMemoryUserDetailsManager(u);
        }
    }

    @Test
    void devBasicStillWorksOnDevChain() throws Exception {
        when(service.dashboard(null)).thenReturn(Map.of("waiting", 0L));
        mockMvc.perform(get("/api/v1/queue/dashboard")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic("devops", "devops-dev-only")))
                .andExpect(status().isOk());
    }
}
