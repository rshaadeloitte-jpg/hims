package com.hims.pharmacy.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.pharmacy.application.ItemStockClient;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ServiceTokenClientTest {

    private static final String SECRET = "test-only-secret-key-0123456789abcdef0123456789abcdef";

    @Test
    void attachesBearerHeaderAndTokenValidates() {
        String baseUrl = "http://item-service:8082";
        RestClient.Builder builder = RestClient.builder().baseUrl(baseUrl);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(baseUrl + "/api/v1/stock/movements"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", Matchers.startsWith("Bearer ")))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        ServiceTokenProvider provider = new ServiceTokenProvider(SECRET);
        ItemStockClient client = new ItemStockClientConfig()
                .itemStockClient(builder, new ObjectMapper(), baseUrl, provider);
        client.issueForSale(1L, 2L, "3", 4, 5L, 6L);

        server.verify();

        JwtDecoder decoder = new JwtConfig().jwtDecoder(SECRET);
        Jwt jwt = decoder.decode(provider.getToken());
        assertThat(jwt.getSubject()).isEqualTo("svc-pharmacy");
        assertThat(jwt.getClaims().get("iss")).isEqualTo("iam-service");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("service");
    }
}
