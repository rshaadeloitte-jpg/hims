package com.hims.search.configuration;

import com.hims.search.infrastructure.indexer.HttpSolrIndexer;
import com.hims.search.infrastructure.indexer.InMemoryIndexer;
import com.hims.search.infrastructure.indexer.SearchIndexer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Indexer backend wiring (register WP-T3): interface SearchIndexer with TWO
 * implementations —
 *   (a) {@link HttpSolrIndexer} when app.search.backend=solr (prod sets
 *       SEARCH_BACKEND=solr + SOLR_BASE_URL; RestClient to the Solr root URL),
 *   (b) {@link InMemoryIndexer} deterministic fallback otherwise (default for
 *       tests/local — DISCLOSED in WP-T3 report which backend ran).
 * Exactly one bean exists in every context.
 */
@Configuration
public class SearchIndexerConfiguration {

    @Bean
    @ConditionalOnProperty(name = "app.search.backend", havingValue = "solr")
    public SearchIndexer httpSolrIndexer(
            RestClient.Builder builder,
            @Value("${app.search.solr.base-url}") String baseUrl,
            @Value("${app.search.solr.connect-timeout-ms:3000}") long connectTimeoutMs,
            @Value("${app.search.solr.read-timeout-ms:10000}") long readTimeoutMs,
            @Value("${app.search.patient-qf}") String patientQf) {
        var requestFactory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        RestClient restClient = builder
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
        return new HttpSolrIndexer(restClient, patientQf);
    }

    @Bean
    @ConditionalOnMissingBean(SearchIndexer.class)
    public SearchIndexer inMemoryIndexer() {
        return new InMemoryIndexer();
    }
}
