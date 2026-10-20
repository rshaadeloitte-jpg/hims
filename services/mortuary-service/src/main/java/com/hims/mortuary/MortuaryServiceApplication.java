package com.hims.mortuary;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

/**
 * Mortuary &amp; Death service (M28) — Wave-3 tranche 3, Catalog #28.
 *
 * Owns ihms_mortuary schema: dead body registry, autopsy requests, release certificates.
 * Integrates with adt-service for death linkage (admission_id references t_admission).
 *
 * NOTE ON EVENTS: all events DEFERRED per KAFKA-EVENT-CATALOG §3 — nothing emitted yet.
 */
@SpringBootApplication
public class MortuaryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MortuaryServiceApplication.class, args);
    }

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
