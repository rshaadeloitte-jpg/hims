package com.hims.iam.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HS256 JWT issue/parse (server-side secret, Catalog #22 security target).
 * Claims: sub=username, uid=user id, roles=[role codes], unit=unit id.
 */
@Service
public class JwtTokenService {

    public static final String CLAIM_UID = "uid";
    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_UNIT = "unit";

    private final SecretKey key;
    private final JwtProperties properties;

    public JwtTokenService(JwtProperties properties) {
        byte[] secretBytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException("iam.jwt.secret must be at least 32 bytes for HS256");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.properties = properties;
    }

    public String issue(String username, Long userId, List<String> roles, Long unitId) {
        Instant now = Instant.now();
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_UID, userId);
        if (roles != null) {
            claims.put(CLAIM_ROLES, roles);
        }
        if (unitId != null) {
            claims.put(CLAIM_UNIT, unitId);
        }
        return Jwts.builder()
                .issuer(properties.getIssuer())
                .subject(username)
                .claims(claims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(properties.getTtlMinutes() * 60)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Parse + verify signature/expiry. Returns null when the token is not a valid,
     * non-expired token issued by this service (callers layer revocation on top).
     */
    public Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(properties.getIssuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> roles(Claims claims) {
        Object raw = claims.get(CLAIM_ROLES);
        return raw instanceof List<?> list ? list.stream().map(String::valueOf).toList() : List.of();
    }

    public long ttlSeconds() {
        return properties.getTtlMinutes() * 60;
    }

    public Instant expiryOf(Claims claims) {
        Date expiration = claims.getExpiration();
        return expiration == null ? null : expiration.toInstant();
    }
}
