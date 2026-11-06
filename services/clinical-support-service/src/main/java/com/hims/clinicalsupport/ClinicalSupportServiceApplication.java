package com.hims.clinicalsupport;

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
        "com.hims.bloodbank",
        "com.hims.chemotherapy",
        "com.hims.equipment",
        "com.hims.clinicalsupport"
}, nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
@EntityScan(basePackages = {
        "com.hims.bloodbank",
        "com.hims.chemotherapy",
        "com.hims.equipment",
        "com.hims.clinicalsupport"
})
@EnableJpaRepositories(basePackages = {
        "com.hims.bloodbank",
        "com.hims.chemotherapy",
        "com.hims.equipment",
        "com.hims.clinicalsupport"
})
@EnableScheduling
public class ClinicalSupportServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClinicalSupportServiceApplication.class, args);
    }

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
