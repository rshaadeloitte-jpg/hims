-- PostgreSQL-as-a-cache (replaces the Redis slot of Catalog #22).
--
-- Shared platform schema ihms_cache. It lives in iam's Flyway because iam is
-- the only bundle that runs Flyway on startup; every service reaches it on the
-- shared `hims` database.
--
-- UNLOGGED: writes skip the WAL. The table is TRUNCATED after an unclean
-- PostgreSQL shutdown and is not replicated, so every reader must treat a miss
-- as "go to the source of truth" — nothing stored here may be the only copy.

CREATE SCHEMA IF NOT EXISTS ihms_cache;

CREATE UNLOGGED TABLE IF NOT EXISTS ihms_cache.app_cache (
    cache_key           VARCHAR(255) PRIMARY KEY,
    cache_value         JSONB        NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expires_at          TIMESTAMPTZ  NOT NULL,
    last_read_timestamp TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_app_cache_expires_at ON ihms_cache.app_cache (expires_at);
CREATE INDEX IF NOT EXISTS idx_app_cache_last_read ON ihms_cache.app_cache (last_read_timestamp);
COMMENT ON TABLE ihms_cache.app_cache IS 'UNLOGGED key/value cache with TTL (expires_at) and LRU (last_read_timestamp). Truncated on crash; never source of truth.';

-- TTL expiration.
CREATE OR REPLACE PROCEDURE ihms_cache.expire_entries()
LANGUAGE plpgsql AS $$
BEGIN
    DELETE FROM ihms_cache.app_cache WHERE expires_at <= NOW();
END;
$$;

-- LRU eviction: trim the table back to p_max_rows, least recently read first.
CREATE OR REPLACE PROCEDURE ihms_cache.evict_lru(p_max_rows INTEGER)
LANGUAGE plpgsql AS $$
DECLARE
    v_excess BIGINT;
BEGIN
    SELECT COUNT(*) - p_max_rows INTO v_excess FROM ihms_cache.app_cache;
    IF v_excess > 0 THEN
        DELETE FROM ihms_cache.app_cache
        WHERE cache_key IN (SELECT cache_key FROM ihms_cache.app_cache
                            ORDER BY last_read_timestamp ASC
                            LIMIT v_excess);
    END IF;
END;
$$;

CREATE OR REPLACE PROCEDURE ihms_cache.maintain(p_max_rows INTEGER DEFAULT 100000)
LANGUAGE plpgsql AS $$
BEGIN
    CALL ihms_cache.expire_entries();
    CALL ihms_cache.evict_lru(p_max_rows);
END;
$$;

-- Register a per-minute pg_cron job only where the extension is installed
-- (not on the current Windows PostgreSQL 18 host). Elsewhere the Spring
-- @Scheduled CacheMaintenanceJob in iam calls ihms_cache.maintain().
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_cron') THEN
        PERFORM cron.schedule('ihms-cache-maintain', '* * * * *', 'CALL ihms_cache.maintain()');
    END IF;
END;
$$;
