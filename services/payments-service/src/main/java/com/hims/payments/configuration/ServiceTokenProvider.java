package com.hims.payments.configuration;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * Short-lived HS256 service token for payments-service -> item-service calls.
 * Cached and renewed ~30s before the 5-minute expiry. Never logged.
 */
@Component
public class ServiceTokenProvider {

    private static final long TTL_SECONDS = 300;
    private static final long RENEW_BEFORE_SECONDS = 30;

    private final byte[] secret;
    private final String subject;

    private volatile String cachedToken;
    private volatile Instant cachedExpiry;

    @Autowired
    public ServiceTokenProvider(@Value("${ihms.security.jwt.secret}") String secret) {
        this(secret, "svc-payments");
    }

    // Visible for tests / other subjects.
    public ServiceTokenProvider(String secret, String subject) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("ihms.security.jwt.secret must not be blank");
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("ihms.security.jwt.secret must be at least 32 bytes for HS256");
        }
        this.secret = bytes;
        this.subject = subject;
    }

    public synchronized String getToken() {
        Instant now = Instant.now();
        if (cachedToken == null || cachedExpiry == null
                || !now.isBefore(cachedExpiry.minusSeconds(RENEW_BEFORE_SECONDS))) {
            cachedToken = issue(now);
        }
        return cachedToken;
    }

    private String issue(Instant now) {
        Instant expiry = now.plusSeconds(TTL_SECONDS);
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer("iam-service")
                    .subject(subject)
                    .claim("roles", List.of("service"))
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(expiry))
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(secret));
            cachedExpiry = expiry;
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to issue service token", e);
        }
    }
}
