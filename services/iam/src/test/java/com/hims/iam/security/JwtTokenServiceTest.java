package com.hims.iam.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Pure unit tests for HS256 issue/parse (server-side secret). */
class JwtTokenServiceTest {

    private static final String SECRET = "unit-test-secret-key-0123456789abcdef0123456789abcdef";

    private JwtTokenService service(long ttlMinutes) {
        JwtProperties props = new JwtProperties();
        props.setSecret(SECRET);
        props.setTtlMinutes(ttlMinutes);
        props.setIssuer("iam-service");
        return new JwtTokenService(props);
    }

    @Test
    void issueThenParse_roundTripsClaims() {
        JwtTokenService svc = service(30);
        String token = svc.issue("rsharma", 42L, List.of("admin", "staff"), 7L);

        Claims claims = svc.parse(token);
        assertThat(claims).isNotNull();
        assertThat(claims.getSubject()).isEqualTo("rsharma");
        assertThat(claims.get(JwtTokenService.CLAIM_UID, Long.class)).isEqualTo(42L);
        assertThat(svc.roles(claims)).containsExactly("admin", "staff");
        assertThat(svc.expiryOf(claims)).isAfter(claims.getIssuedAt().toInstant());
    }

    @Test
    void parse_rejectsTamperedToken() {
        JwtTokenService svc = service(30);
        String token = svc.issue("rsharma", 1L, List.of(), null);
        String tampered = token.substring(0, token.length() - 3) + "xxx";
        assertThat(svc.parse(tampered)).isNull();
    }

    @Test
    void parse_rejectsExpiredToken() {
        JwtTokenService svc = service(-5); // expired 5 minutes ago
        String token = svc.issue("rsharma", 1L, List.of(), null);
        assertThat(svc.parse(token)).isNull();
    }

    @Test
    void parse_rejectsForeignIssuerKey() {
        JwtProperties otherProps = new JwtProperties();
        otherProps.setSecret("another-secret-key-0123456789abcdef0123456789abcdef");
        JwtTokenService other = new JwtTokenService(otherProps);
        String token = other.issue("intruder", 1L, List.of(), null);
        assertThat(service(30).parse(token)).isNull();
    }

    @Test
    void shortSecret_isRejected() {
        JwtProperties props = new JwtProperties();
        props.setSecret("too-short");
        try {
            new JwtTokenService(props);
            throw new AssertionError("expected IllegalStateException");
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage()).contains("32 bytes");
        }
    }
}
