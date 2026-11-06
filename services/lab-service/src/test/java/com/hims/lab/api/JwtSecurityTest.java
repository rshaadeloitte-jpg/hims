package com.hims.lab.api;

import com.hims.lab.api.MachineController;
import com.hims.lab.application.MachineService;
import com.hims.lab.configuration.JwtConfig;
import com.hims.lab.configuration.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MachineController.class)
@Import({SecurityConfig.class, JwtConfig.class})
@AutoConfigureMockMvc
class JwtSecurityTest {

    private static final String SECRET = "test-only-secret-key-0123456789abcdef0123456789abcdef";
    private static final String OTHER_SECRET = "another-test-secret-key-0123456789abcdef0123456789abcdef";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MachineService machineService;


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
        String t = token(SECRET, "iam-service", List.of("staff"), Instant.now().plusSeconds(300));
        mockMvc.perform(get("/api/v1/machines")
                        .header("Authorization", "Bearer " + t))
                .andExpect(status().isOk());
    }

    @Test
    void expiredToken_returns401() throws Exception {
        String t = token(SECRET, "iam-service", List.of("staff"), Instant.now().minusSeconds(300));
        mockMvc.perform(get("/api/v1/machines")
                        .header("Authorization", "Bearer " + t))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongSignature_returns401() throws Exception {
        String t = token(OTHER_SECRET, "iam-service", List.of("staff"), Instant.now().plusSeconds(300));
        mockMvc.perform(get("/api/v1/machines")
                        .header("Authorization", "Bearer " + t))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/machines"))
                .andExpect(status().isUnauthorized());
    }
}
