package com.hims.payments.configuration;

import com.hims.billing.configuration.JwtConfig;
import com.hims.payments.application.BillingClient;
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
        String baseUrl = "http://billing-service:8098";
        RestClient.Builder builder = RestClient.builder().baseUrl(baseUrl);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(baseUrl + "/api/v1/bills/7"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", Matchers.startsWith("Bearer ")))
                .andRespond(withSuccess("{\"billId\":7,\"balanceSelf\":0,\"balanceNonSelf\":0}", MediaType.APPLICATION_JSON));

        ServiceTokenProvider provider = new ServiceTokenProvider(SECRET);
        BillingClient client = new BillingClientConfig()
                .billingClient(builder, baseUrl, provider);
        client.getBill(7L);

        server.verify();

        JwtDecoder decoder = new JwtConfig().jwtDecoder(SECRET);
        Jwt jwt = decoder.decode(provider.getToken());
        assertThat(jwt.getSubject()).isEqualTo("svc-payments");
        assertThat(jwt.getClaims().get("iss")).isEqualTo("iam-service");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("service");
    }
}
