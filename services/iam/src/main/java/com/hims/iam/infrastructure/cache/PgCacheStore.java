package com.hims.iam.infrastructure.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Key/value cache on the UNLOGGED table ihms_cache.app_cache (iam V4__pg_cache.sql),
 * replacing the Redis slot of Catalog #22.
 *
 * A read returns only unexpired keys and refreshes last_read_timestamp (LRU) in
 * the same statement. The table is truncated after an unclean PostgreSQL
 * shutdown, so callers must treat {@link Optional#empty()} as "ask the source of
 * truth". A cache outage (table missing, DB error) also reads as a miss and a
 * write is dropped with a warning, so the cache can never fail a request.
 * Statements run outside any caller transaction (NOT_SUPPORTED): a failed cache
 * statement must not abort the caller's PostgreSQL transaction.
 */
@Component
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class PgCacheStore {

    private static final Logger log = LoggerFactory.getLogger(PgCacheStore.class);

    private static final String GET_SQL = """
            UPDATE ihms_cache.app_cache SET last_read_timestamp = NOW()
            WHERE cache_key = ? AND expires_at > NOW()
            RETURNING cache_value::text
            """;

    private static final String PUT_SQL = """
            INSERT INTO ihms_cache.app_cache (cache_key, cache_value, expires_at)
            VALUES (?, CAST(? AS jsonb), ?)
            ON CONFLICT (cache_key) DO UPDATE
                SET cache_value = EXCLUDED.cache_value,
                    expires_at = EXCLUDED.expires_at,
                    created_at = NOW(),
                    last_read_timestamp = NOW()
            """;

    private final JdbcTemplate jdbc;

    public PgCacheStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** JSON value of an unexpired key, or empty on miss / cache unavailable. */
    public Optional<String> get(String key) {
        try {
            List<String> rows = jdbc.queryForList(GET_SQL, String.class, key);
            return rows.isEmpty() ? Optional.empty() : Optional.ofNullable(rows.getFirst());
        } catch (Exception e) {
            log.warn("Cache read failed for key={}, treating as miss: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    /** Upsert a JSON value that expires at {@code expiresAt}. */
    public void put(String key, String jsonValue, Instant expiresAt) {
        try {
            jdbc.update(PUT_SQL, key, jsonValue, Timestamp.from(expiresAt));
        } catch (Exception e) {
            log.warn("Cache write failed for key={}, dropped: {}", key, e.getMessage());
        }
    }

    public void put(String key, String jsonValue, Duration ttl) {
        put(key, jsonValue, Instant.now().plus(ttl));
    }

    public void evict(String key) {
        try {
            jdbc.update("DELETE FROM ihms_cache.app_cache WHERE cache_key = ?", key);
        } catch (Exception e) {
            log.warn("Cache evict failed for key={}: {}", key, e.getMessage());
        }
    }

    /** TTL expiry, then LRU trim down to {@code maxRows}, via ihms_cache.maintain(). */
    public void maintain(int maxRows) {
        jdbc.update("CALL ihms_cache.maintain(?)", maxRows);
    }
}
