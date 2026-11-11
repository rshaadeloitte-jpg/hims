package com.hims.reference;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
        "com.hims.reference",
        "com.hims.search",
        "com.hims.file"
}, nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
@EntityScan(basePackages = {
        "com.hims.reference",
        "com.hims.search",
        "com.hims.file"
})
@EnableJpaRepositories(basePackages = {
        "com.hims.reference",
        "com.hims.search",
        "com.hims.file"
})
@EnableScheduling
public class ReferenceServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReferenceServiceApplication.class, args);
    }

}
