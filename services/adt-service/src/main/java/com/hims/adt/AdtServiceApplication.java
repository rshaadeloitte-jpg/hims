package com.hims.adt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AdtServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdtServiceApplication.class, args);
    }
}
