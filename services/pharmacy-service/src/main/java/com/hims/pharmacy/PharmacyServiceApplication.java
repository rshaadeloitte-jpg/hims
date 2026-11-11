package com.hims.pharmacy;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
        "com.hims.pharmacy",
        "com.hims.item"
}, nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
@EntityScan(basePackages = {
        "com.hims.pharmacy",
        "com.hims.item"
})
@EnableJpaRepositories(basePackages = {
        "com.hims.pharmacy",
        "com.hims.item"
}, nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
@EnableScheduling
public class PharmacyServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PharmacyServiceApplication.class, args);
    }

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
