package com.hims.transport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * iHMS Transport service (S17) — Wave 2 MIG-023 sub-slice. Single-writer owner
 * of the ihms_transport schema: vehicle request lifecycle (raise / allocate /
 * approve / vehicle-out / vehicle-in / cancel), vehicle + driver masters, fuel
 * station details, and vehicle maintenance. REST-only: TransportRequested is
 * rejected phase 1 in KAFKA-EVENT-CATALOG — no outbox until a second consumer
 * appears.
 */
@SpringBootApplication
public class TransportServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransportServiceApplication.class, args);
    }
}
