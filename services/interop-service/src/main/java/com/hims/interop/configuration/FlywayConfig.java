package com.hims.interop.configuration;

import java.util.Arrays;
import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {

    private static final String[] MODULES = {"interop", "govt", "aasmaan", "abha"};
    private static final String[] SCHEMAS = {
            "ihms_interop", "ihms_govt", "ihms_aasmaan", "ihms_abha"
    };

    @Bean(name = "bundleFlywayMigrator", initMethod = "migrate")
    public FlywayMigrator bundleFlywayMigrator(DataSource dataSource) {
        return new FlywayMigrator(dataSource);
    }

    public static class FlywayMigrator {

        private final DataSource dataSource;

        public FlywayMigrator(DataSource dataSource) {
            this.dataSource = dataSource;
        }

        public void migrate() {
            for (int i = 0; i < MODULES.length; i++) {
                Flyway.configure()
                        .dataSource(dataSource)
                        .schemas(SCHEMAS[i])
                        .defaultSchema(SCHEMAS[i])
                        .createSchemas(true)
                        .locations("classpath:db/migration/" + MODULES[i])
                        .baselineOnMigrate(true)
                        .load()
                        .migrate();
            }
        }
    }

    @Bean
    public static BeanFactoryPostProcessor bundleFlywayDependsOnPostProcessor() {
        return beanFactory -> {
            if (beanFactory.containsBeanDefinition("entityManagerFactory")) {
                BeanDefinition bd = beanFactory.getBeanDefinition("entityManagerFactory");
                String[] dependsOn = bd.getDependsOn();
                if (dependsOn == null) {
                    bd.setDependsOn("bundleFlywayMigrator");
                } else {
                    String[] newDependsOn = Arrays.copyOf(dependsOn, dependsOn.length + 1);
                    newDependsOn[dependsOn.length] = "bundleFlywayMigrator";
                    bd.setDependsOn(newDependsOn);
                }
            }
        };
    }
}
