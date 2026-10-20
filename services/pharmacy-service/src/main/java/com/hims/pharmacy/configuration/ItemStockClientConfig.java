package com.hims.pharmacy.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.pharmacy.application.ItemStockClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Synchronous pharmacy -&gt; item-service stock client (GAP-REGISTER-5 WP-R2).
 * STOCK STAYS IN item-service (single-writer decision D-1; the retired legacy
 * dual-write hotspot CIMS_ADDITEMSTOCK / PharmacyServiceImpl:2140 is NOT revived):
 * every dispense POSTs a SUBTRACT movement to the single-writer stock API.
 *
 * Unit tests bind a MockRestServiceServer via MockServerRestClientCustomizer-style
 * builder binding; the PG16 integration test points this at a real-HTTP stub that
 * replicates item-service's wire shape field-by-field (disclosed in the test and
 * in outputs/implementation/WP-R2-pharmacy.md — it is NOT a live item-service).
 */
@Configuration
public class ItemStockClientConfig {

    @Bean
    public ItemStockClient itemStockClient(RestClient.Builder builder,
                                           ObjectMapper objectMapper,
                                           @Value("${app.item-service.base-url}") String baseUrl) {
        return new ItemStockClient(builder.baseUrl(baseUrl).build(), objectMapper);
    }
}
