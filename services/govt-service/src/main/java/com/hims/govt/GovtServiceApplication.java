package com.hims.govt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * iHMS Govt Programs service (Catalog #28, GAP-REGISTER-6 WP-T4), port 8108, schema
 * ihms_govt. No @EnableScheduling: like cssd-service there is NO outbox relay because
 * this service emits NO events — IncentiveComputed / CaseUpdated are DEFERRED per
 * KAFKA-EVENT-CATALOG (negative outbox assertion in the integration test). External
 * PCTS / Chiranjeevi-Yojana pushes are OUT OF SCOPE (comment-only).
 */
@SpringBootApplication
public class GovtServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GovtServiceApplication.class, args);
    }
}
