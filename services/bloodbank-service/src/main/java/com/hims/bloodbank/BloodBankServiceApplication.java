package com.hims.bloodbank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

/**
 * Blood Bank service (Catalog #13) — Wave-3 tranche 3, GAP-REGISTER WP-9.
 *
 * NOTE ON CRON WIRING: legacy runs BR-BLD-002/003/004/005 as daily scheduled jobs
 * (ApplicationJobs.java:769-848,1045-1061). Here they are exposed as IDEMPOTENT
 * one-shot endpoints POST /api/v1/jobs/{decross-expired,expire-requisitions,
 * cancel-stale-donors}; scheduler wiring (e.g. @Scheduled crons) is DEFERRED —
 * the orchestrator/cron layer is expected to invoke these endpoints.
 *
 * NOTE ON EVENTS: BloodRequisitionCreated is DEFERRED per MICROSERVICE-CATALOG #13
 * / KAFKA-EVENT-CATALOG §3 — this service emits NO events in Wave 3.
 */
@SpringBootApplication
@EnableScheduling
public class BloodBankServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BloodBankServiceApplication.class, args);
    }

    /** Injected clock so BR-BLD-002/003/004/005 time predicates are testable. */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
