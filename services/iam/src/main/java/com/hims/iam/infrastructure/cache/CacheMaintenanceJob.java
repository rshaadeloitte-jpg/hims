package com.hims.iam.infrastructure.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Spring fallback for the pg_cron jobs registered by V4/V5 when the extension
 * is installed. The current Windows PostgreSQL host has no pg_cron, so this
 * job is the scheduler there: ihms_cache.maintain() every minute and
 * ihms_eventbus.purge() hourly. Both procedures are idempotent, so running
 * alongside pg_cron (or on several iam instances) is harmless.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.cache.maintenance.enabled", havingValue = "true", matchIfMissing = true)
public class CacheMaintenanceJob {

    private static final Logger log = LoggerFactory.getLogger(CacheMaintenanceJob.class);

    private final PgCacheStore cache;
    private final JdbcTemplate jdbc;
    private final int maxRows;

    public CacheMaintenanceJob(PgCacheStore cache, JdbcTemplate jdbc,
                               @Value("${app.cache.max-rows:100000}") int maxRows) {
        this.cache = cache;
        this.jdbc = jdbc;
        this.maxRows = maxRows;
    }

    @Scheduled(fixedDelayString = "${app.cache.maintenance.interval-ms:60000}")
    public void maintainCache() {
        try {
            cache.maintain(maxRows);
        } catch (Exception e) {
            log.warn("Cache maintenance failed: {}", e.getMessage());
        }
    }

    @Scheduled(fixedDelayString = "${app.eventbus.purge.interval-ms:3600000}")
    public void purgeEventBus() {
        try {
            jdbc.update("CALL ihms_eventbus.purge()");
        } catch (Exception e) {
            log.warn("Event-bus purge failed: {}", e.getMessage());
        }
    }
}
