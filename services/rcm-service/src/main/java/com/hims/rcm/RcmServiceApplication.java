package com.hims.rcm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RcmServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RcmServiceApplication.class, args);
    }
}
