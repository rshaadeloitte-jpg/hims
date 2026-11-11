package com.hims.interop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * iHMS Interop &amp; Notifications service — bundles interop, govt, aasmaan, and abha modules.
 */
@SpringBootApplication(scanBasePackages = {
        "com.hims.interop",
        "com.hims.govt",
        "com.hims.aasmaan",
        "com.hims.abha"
}, nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
@EntityScan(basePackages = {
        "com.hims.interop",
        "com.hims.govt",
        "com.hims.aasmaan",
        "com.hims.abha"
})
@EnableJpaRepositories(basePackages = {
        "com.hims.interop",
        "com.hims.govt",
        "com.hims.aasmaan",
        "com.hims.abha"
})
public class InteropServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InteropServiceApplication.class, args);
    }
}
