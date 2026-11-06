package com.hims.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "com.hims.forms",
        "com.hims.rmrs",
        "com.hims.reporting",
        "com.hims.admin"
}, nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
@EntityScan(basePackages = {
        "com.hims.forms",
        "com.hims.rmrs",
        "com.hims.reporting",
        "com.hims.admin"
})
@EnableJpaRepositories(basePackages = {
        "com.hims.forms",
        "com.hims.rmrs",
        "com.hims.reporting",
        "com.hims.admin"
})
public class AdminServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminServiceApplication.class, args);
    }
}
