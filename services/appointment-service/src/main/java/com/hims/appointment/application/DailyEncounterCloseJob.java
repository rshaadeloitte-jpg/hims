package com.hims.appointment.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Daily encounter closing job (MIG-031; legacy ApplicationJobs:542 parity):
 * every open encounter whose encounter_date has passed is closed and an
 * EncounterClosed event is emitted per row, so OPD visits can never linger
 * open indefinitely and downstream consumers get a deterministic end-of-visit
 * signal.
 */
@Component
public class DailyEncounterCloseJob {

    private static final Logger log = LoggerFactory.getLogger(DailyEncounterCloseJob.class);

    public static final String REASON_AUTO_DAILY_CLOSE = "AUTO_DAILY_CLOSE";

    private final EncounterService encounterService;
    private final boolean enabled;

    public DailyEncounterCloseJob(EncounterService encounterService,
                                  @Value("${app.jobs.encounter-close.enabled:true}") boolean enabled) {
        this.encounterService = encounterService;
        this.enabled = enabled;
    }

    @Scheduled(cron = "${app.jobs.encounter-close.cron:0 30 1 * * *}", zone = "Asia/Kolkata")
    public void closeStaleEncounters() {
        if (!enabled) {
            return;
        }
        int closed = encounterService.closeStaleEncounters(LocalDate.now(), REASON_AUTO_DAILY_CLOSE);
        if (closed > 0) {
            log.info("Daily encounter close job closed {} encounters", closed);
        }
    }
}
