package com.hims.diet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * iHMS Diet service (S18) — Wave 2 MIG-023 sub-slice. Single-writer owner of
 * the ihms_diet schema: diet item / session / plan masters, today-menu, diet
 * order lifecycle (raise / sign / cancel), and food item schedule lifecycle
 * (order / advance / uncheck / hold / remove). REST-only: no diet-produced
 * events in KAFKA-EVENT-CATALOG (diet only consumes admission events) — no
 * outbox until a second consumer appears.
 */
@SpringBootApplication
public class DietServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DietServiceApplication.class, args);
    }
}
