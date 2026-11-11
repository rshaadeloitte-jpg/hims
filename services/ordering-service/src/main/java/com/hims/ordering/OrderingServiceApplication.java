package com.hims.ordering;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
        "com.hims.ordering",
        "com.hims.lab",
        "com.hims.radiology"
}, nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
@EntityScan(basePackages = {
        "com.hims.ordering",
        "com.hims.lab",
        "com.hims.radiology"
})
@EnableJpaRepositories(basePackages = {
        "com.hims.ordering",
        "com.hims.lab",
        "com.hims.radiology"
})
@EnableScheduling
public class OrderingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderingServiceApplication.class, args);
    }
}
