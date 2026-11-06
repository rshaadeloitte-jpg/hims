package com.hims.ops.configuration;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {

    private static final String[] MODULES = {"diet", "cssd", "linen", "waste", "mortuary", "transport", "grievance"};
    private static final String[] SCHEMAS = {
            "ihms_diet", "ihms_cssd", "ihms_linen", "ihms_waste",
            "ihms_mortuary", "ihms_transport", "ihms_grievance"
    };

    @Bean
    public ApplicationRunner opsFlywayMigrator(DataSource dataSource) {
        return new ApplicationRunner() {
            @Override
            public void run(ApplicationArguments args) {
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
        };
    }
}
