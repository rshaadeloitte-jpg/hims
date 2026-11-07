package com.hims.pharmacy.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.pharmacy.application.exceptions.StockIssueException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ItemStockClientTest {

    private static final String BASE_URL = "http://item-service:8082";
    private MockRestServiceServer server;
    private ItemStockClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new ItemStockClient(builder.build(), new ObjectMapper());
    }

    @Test
    void availableBatches_sendsStoreIdAndItemId_andMapsFields() {
        String json = """
                [
                  {
                    "batchId": 42,
                    "batchCode": "BATCH-001",
                    "expiryDate": "2026-12-31",
                    "availableQty": 100.5,
                    "mrp": 25.0,
                    "purchaseRate": 20.0,
                    "vatPercentage": 5.0,
                    "vatAmount": 1.0,
                    "discountOnSale": 0.0,
                    "conversionFactor": 1.0,
                    "freezeRecall": false,
                    "itemId": 501,
                    "itemName": "Paracetamol",
                    "itemCode": "PCM"
                  }
                ]
                """;
        server.expect(requestTo(BASE_URL + "/api/v1/stock/available-batches?storeId=7&itemId=501"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        List<ItemStockClient.BatchAvailabilityView> batches = client.availableBatches(7L, 501L);

        server.verify();
        assertThat(batches).hasSize(1);
        ItemStockClient.BatchAvailabilityView b = batches.get(0);
        assertThat(b.batchId()).isEqualTo(42L);
        assertThat(b.batchCode()).isEqualTo("BATCH-001");
        assertThat(b.expiryDate()).isEqualTo(java.sql.Date.valueOf("2026-12-31"));
        assertThat(b.availableQty()).isEqualByComparingTo(new BigDecimal("100.5"));
        assertThat(b.mrp()).isEqualByComparingTo(new BigDecimal("25.0"));
        assertThat(b.freezeRecall()).isFalse();
        assertThat(b.itemId()).isEqualTo(501L);
        assertThat(b.itemName()).isEqualTo("Paracetamol");
        assertThat(b.itemCode()).isEqualTo("PCM");
    }

    @Test
    void availableBatches_itemServiceError_mapsToError() {
        server.expect(requestTo(BASE_URL + "/api/v1/stock/available-batches?storeId=7&itemId=501"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.availableBatches(7L, 501L))
                .isInstanceOf(StockIssueException.class)
                .hasMessageContaining("call failed");

        server.verify();
    }
}
