package com.hims.ops;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "com.hims.diet",
        "com.hims.cssd",
        "com.hims.linen",
        "com.hims.waste",
        "com.hims.mortuary",
        "com.hims.transport",
        "com.hims.grievance",
        "com.hims.ops"
}, nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
@EntityScan(basePackages = {
        "com.hims.diet",
        "com.hims.cssd",
        "com.hims.linen",
        "com.hims.waste",
        "com.hims.mortuary",
        "com.hims.transport",
        "com.hims.grievance",
        "com.hims.ops"
})
@EnableJpaRepositories(basePackages = {
        "com.hims.diet",
        "com.hims.cssd",
        "com.hims.linen",
        "com.hims.waste",
        "com.hims.mortuary",
        "com.hims.transport",
        "com.hims.grievance",
        "com.hims.ops"
})
public class OpsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpsServiceApplication.class, args);
    }

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
