package com.hims.appointment.configuration;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = {
        "com.hims.appointment",
        "com.hims.queue",
        "com.hims.kiosk"
})
@EnableJpaRepositories(basePackages = {
        "com.hims.appointment",
        "com.hims.queue",
        "com.hims.kiosk"
})
public class JpaConfig {
}
