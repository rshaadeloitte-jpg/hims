package com.hims.interop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * iHMS Interop &amp; Notifications service (Catalog #25, GAP-REGISTER-6 WP-T1),
 * port 8105, schema ihms_interop.
 *
 * No @EnableScheduling: unlike ordering-service there is NO outbox relay because
 * this service EMITS NOTHING — it is a consumer/relay, not a producer. The
 * internal event-relay ingress API models the future Kafka consumer; the HL7 /
 * ABDM / WhatsApp dispatchers that would drain these queues are OUT OF SCOPE
 * (comment-only per register).
 */
@SpringBootApplication
public class InteropServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InteropServiceApplication.class, args);
    }
}
