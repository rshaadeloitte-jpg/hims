package com.hims.bcma;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(nameGenerator = org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator.class)
public class BcmaTestApplication {

    @Bean
    public static BeanPostProcessor bcmaSchemaInitializer() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof DataSource ds) {
                    try (Connection conn = ds.getConnection(); Statement stmt = conn.createStatement()) {
                        stmt.execute("CREATE SCHEMA IF NOT EXISTS \"ihms_bcma\"");
                    } catch (Exception ignored) {
                    }
                }
                return bean;
            }
        };
    }
}
