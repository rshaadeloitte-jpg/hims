package com.hims.pharmacy.api;

import com.hims.pharmacy.api.DrugInteractionController;
import com.hims.pharmacy.application.DrugInteractionService;
import com.hims.pharmacy.configuration.DevSecurityConfig;
import com.hims.pharmacy.configuration.JwtConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
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

@WebMvcTest(DrugInteractionController.class)
@ActiveProfiles("dev")
@Import({DevSecurityConfig.class, JwtConfig.class})
@AutoConfigureMockMvc
class DevJwtSecurityTest {

    private static final String SECRET = "test-only-secret-key-0123456789abcdef0123456789abcdef";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private com.hims.pharmacy.application.DrugInteractionService drugInteractionService;

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
    void noToken_returns401OnBusinessEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/drug-interactions/patient/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validToken_getsPastSecurity() throws Exception {
        String t = token(SECRET, "iam-service", List.of("staff"), Instant.now().plusSeconds(300));
        mockMvc.perform(get("/api/v1/drug-interactions/patient/1")
                        .header("Authorization", "Bearer " + t))
                .andExpect(status().isOk());
    }
}
