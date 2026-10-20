package com.hims.pmrd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PMRDServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PMRDServiceApplication.class, args);
    }
}
