package com.hims.reporting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * iHMS Reporting &amp; Analytics service (Catalog #26, GAP-REGISTER-6 WP-T2),
 * port 8106, schema ihms_reporting.
 * No @EnableScheduling: there is NO outbox relay because this service EMITS
 * NOTHING (register WP-T2 EVENT POLICY) — the integration test asserts the
 * outbox stays empty through every flow. Event consumption from sibling
 * services is modeled as an INTERNAL event-relay ingress API; production would
 * be a Kafka consumer (documented at every site).
 */
@SpringBootApplication
public class ReportingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReportingServiceApplication.class, args);
    }
}
