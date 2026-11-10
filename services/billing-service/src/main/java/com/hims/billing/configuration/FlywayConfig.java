package com.hims.billing.configuration;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.orm.jpa.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {

    private static final String[] MODULES = {"billing", "payments", "rcm"};
    private static final String[] SCHEMAS = {
            "ihms_billing", "ihms_payment", "ihms_rcm"
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
            try (java.sql.Connection conn = dataSource.getConnection()) {
                String dbName = conn.getMetaData().getDatabaseProductName();
                if (dbName != null && dbName.toLowerCase().contains("h2")) {
                    return;
                }
            } catch (Exception ignored) {
            }
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
    public static EntityManagerFactoryDependsOnPostProcessor bundleFlywayDependsOnPostProcessor() {
        return new EntityManagerFactoryDependsOnPostProcessor("bundleFlywayMigrator");
    }
}
