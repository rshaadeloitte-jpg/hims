package com.hims.adt.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.sql.Timestamp;

/**
 * BR-IPD-005 — interim-admission auto-abscond (legacy
 * updateAbscondInterimAdmissionStatusJob): unbedded admissions older than the
 * configured window are cancelled as AUTO_ABSCONDED with abscond_datetime set.
 * Exactly-3-days counts (<= boundary), matching the legacy sweep predicate.
 */
@Component
public class AbscondSweepJob {

    private static final Logger log = LoggerFactory.getLogger(AbscondSweepJob.class);

    public static final String REASON_AUTO_ABSCOND = "auto-abscond: unbedded beyond window";

    private final AdmissionService admissionService;
    private final boolean enabled;
    private final long olderThanDays;

    public AbscondSweepJob(AdmissionService admissionService,
                           @Value("${app.jobs.abscond.enabled:true}") boolean enabled,
                           @Value("${app.jobs.abscond.older-than-days:3}") long olderThanDays) {
        this.admissionService = admissionService;
        this.enabled = enabled;
        this.olderThanDays = olderThanDays;
    }

    @Scheduled(cron = "${app.jobs.abscond.cron:0 0 2 * * *}",
               zone = "${app.jobs.abscond.zone:Asia/Kolkata}")
    public void sweep() {
        if (!enabled) {
            return;
        }
        Timestamp cutoff = Timestamp.from(Instant.now().minus(Duration.ofDays(olderThanDays)));
        int swept = admissionService.sweepAbsconds(cutoff).size();
        if (swept > 0) {
            log.info("Abscond sweep: {} interim admissions auto-absconded (cutoff={})", swept, cutoff);
        }
    }
}
