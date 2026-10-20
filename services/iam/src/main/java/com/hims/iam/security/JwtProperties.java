package com.hims.iam.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** JWT issuer settings (iam.jwt.*). Secret MUST come from the environment outside dev (Catalog #22 secret remediation). */
@ConfigurationProperties(prefix = "iam.jwt")
public class JwtProperties {

    /** HS256 signing secret — at least 32 bytes. */
    private String secret = "dev-only-secret-key-change-me-0123456789abcdef";

    /** Access-token TTL in minutes; 30 mirrors the legacy Redis session TTL. */
    private long ttlMinutes = 30;

    private String issuer = "iam-service";

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public long getTtlMinutes() { return ttlMinutes; }
    public void setTtlMinutes(long ttlMinutes) { this.ttlMinutes = ttlMinutes; }
    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
}
