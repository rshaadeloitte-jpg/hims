package com.hims.chemotherapy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * iHMS Chemotherapy Service (M29), port 8110, schema ihms_chemotherapy.
 * Handles oncology chemotherapy regimens, cycle tracking, and administration.
 */
@SpringBootApplication
public class ChemotherapyServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChemotherapyServiceApplication.class, args);
    }
}
