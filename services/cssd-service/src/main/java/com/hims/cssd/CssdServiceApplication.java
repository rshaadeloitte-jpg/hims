package com.hims.cssd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * iHMS CSSD service (Catalog #15, GAP-REGISTER-5 WP-R3), port 8102, schema ihms_cssd.
 * No @EnableScheduling: unlike ordering-service there is NO outbox relay because this
 * service emits NO events (KitIssued / SterilizationCompleted REJECTED phase 1).
 */
@SpringBootApplication
public class CssdServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CssdServiceApplication.class, args);
    }
}
