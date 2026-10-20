package com.hims.payments.configuration;

import com.hims.payments.application.BillingClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Synchronous payments -> billing settlement client (Catalog #6 dependency
 * "Billing (S/E settlement update)"). Spring {@link RestClient} against the
 * billing-service base URL; unit tests bind a MockRestServiceServer via
 * MockServerRestClientCustomizer, the integration test points this at a real
 * HTTP stub (billing-service itself lands with WP-11).
 */
@Configuration
public class BillingClientConfig {

    @Bean
    public BillingClient billingClient(RestClient.Builder builder,
                                       @Value("${app.billing.base-url}") String baseUrl) {
        return new BillingClient(builder.baseUrl(baseUrl).build());
    }
}
