package com.hims.reference.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = {
        "com.hims.reference",
        "com.hims.search",
        "com.hims.file"
}, nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
public class JpaConfig {
}
