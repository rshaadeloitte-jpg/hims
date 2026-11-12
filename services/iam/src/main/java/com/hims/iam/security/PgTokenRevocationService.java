package com.hims.iam.security;

import com.hims.iam.infrastructure.cache.PgCacheStore;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Revoke list on the shared PostgreSQL cache (the Redis-backed impl planned for
 * Catalog #22), so a revocation is seen by every iam instance and survives a
 * clean restart. Entries expire with the token. The cache is UNLOGGED: after a
 * PostgreSQL crash the list is empty again, the same exposure the in-memory list
 * has on every restart, bounded by the access-token TTL.
 *
 * Keys are hashed here because a raw JWT can exceed the 255-char cache key.
 */
@Service
@Primary
public class PgTokenRevocationService implements TokenRevocationService {

    static final String KEY_PREFIX = "iam:revoked:";
    /** Upper bound for a revocation with no expiry; access tokens live 30 minutes. */
    static final Duration MAX_REVOCATION = Duration.ofDays(1);

    private final PgCacheStore cache;

    public PgTokenRevocationService(PgCacheStore cache) {
        this.cache = cache;
    }

    @Override
    public void revoke(String tokenIdOrHash, Instant until) {
        if (tokenIdOrHash != null && !tokenIdOrHash.isBlank()) {
            Instant cap = Instant.now().plus(MAX_REVOCATION);
            Instant expiresAt = until == null || until.isAfter(cap) ? cap : until;
            cache.put(key(tokenIdOrHash), "true", expiresAt);
        }
    }

    @Override
    public boolean isRevoked(String tokenIdOrHash) {
        return tokenIdOrHash != null && cache.get(key(tokenIdOrHash)).isPresent();
    }

    /** Expired rows are removed by ihms_cache.maintain() (pg_cron or CacheMaintenanceJob). */
    @Override
    public int purgeExpired() {
        return 0;
    }

    static String key(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return KEY_PREFIX + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
