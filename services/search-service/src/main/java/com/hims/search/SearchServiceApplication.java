package com.hims.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * iHMS Search &amp; Index service (Catalog #27, GAP-REGISTER-6 WP-T3), port 8107,
 * schema ihms_search.
 * No @EnableScheduling: this service EMITS NOTHING (pure consumer of sibling
 * events via the internal event-routing ingress) — there is no outbox relay and
 * the outbox table stays empty (asserted by tests, cssd-service convention).
 */
@SpringBootApplication
public class SearchServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SearchServiceApplication.class, args);
    }
}
