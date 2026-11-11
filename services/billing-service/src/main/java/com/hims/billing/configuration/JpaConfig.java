package com.hims.billing.configuration;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = {
        "com.hims.billing",
        "com.hims.payments",
        "com.hims.rcm"
})
@EnableJpaRepositories(basePackages = {
        "com.hims.billing",
        "com.hims.payments",
        "com.hims.rcm"
}, nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
public class JpaConfig {
}
