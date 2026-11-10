package com.hims.adt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
        "com.hims.adt",
        "com.hims.nursing",
        "com.hims.bcma",
        "com.hims.ot"
}, nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
@EntityScan(basePackages = {
        "com.hims.adt",
        "com.hims.nursing",
        "com.hims.bcma",
        "com.hims.ot"
})
@EnableJpaRepositories(basePackages = {
        "com.hims.adt",
        "com.hims.nursing",
        "com.hims.bcma",
        "com.hims.ot"
}, nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
@EnableScheduling
public class AdtServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdtServiceApplication.class, args);
    }
}
