package com.hims.iam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * iHMS Security &amp; IAM service (Catalog #22, Wave 0).
 * Auth (login/logout/validate, HS256 JWT), user CRUD and role management over
 * the ihms_iam schema. Layering identical to sibling services.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class IamServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IamServiceApplication.class, args);
    }
}
