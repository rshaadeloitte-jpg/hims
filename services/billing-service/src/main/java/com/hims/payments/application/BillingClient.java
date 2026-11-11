package com.hims.payments.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.payments.application.exceptions.BillingCallException;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Synchronous payments -> billing REST client (Catalog #6 "Billing (S/E
 * settlement update)"). Two calls:
 *   GET /api/v1/bills/{id}                                — outstanding balances
 *   PUT /api/v1/internal/bills/{id}/settlement            — apply paid amounts (BR-BILL-006)
 * The settlement call is request-scoped (documented saga deviation): if it
 * fails, the whole payment transaction rolls back and NO E-08 is emitted.
 */
public class BillingClient {

    private final RestClient restClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public BillingClient(RestClient restClient) {
        this.restClient = restClient;
    }

    /** Outstanding self / non-self balances of the bill (BR-BILL-006 settled <=> both <= 0). */
    public BillView getBill(long billId) {
        try {
            String body = restClient.get()
                    .uri("/api/v1/bills/{id}", billId)
                    .retrieve()
                    .body(String.class);
            if (body == null) {
                throw new BillingCallException("Empty response from billing GET bill " + billId);
            }
            JsonNode node = mapper.readTree(body);
            if (node.has("status") && "ERROR".equals(node.get("status").asText())) {
                throw new BillingCallException("Bill not found on billing-service: " + billId);
            }
            return new BillView(
                    node.path("billId").asLong(billId),
                    BigDecimal.valueOf(node.path("balanceSelf").asDouble(0)),
                    BigDecimal.valueOf(node.path("balanceNonSelf").asDouble(0)));
        } catch (BillingCallException e) {
            throw e;
        } catch (RestClientException e) {
            throw new BillingCallException("billing-service GET /api/v1/bills/" + billId + " failed", e);
        } catch (Exception e) {
            throw new BillingCallException("Unparseable billing GET response for bill " + billId, e);
        }
    }

    /**
     * PUT /api/v1/internal/bills/{id}/settlement with cumulative paid amounts
     * computed from this payment's allocation. Idempotent on the billing side by
     * recomputation (BR-BILL-006 replay-safe).
     */
    public SettlementResult settle(long billId, BigDecimal paidSelf, BigDecimal paidNonSelf) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("paidSelf", paidSelf);
        request.put("paidNonSelf", paidNonSelf);
        try {
            String body = restClient.put()
                    .uri("/api/v1/internal/bills/{id}/settlement", billId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(String.class);
            if (body == null) {
                throw new BillingCallException("Empty response from billing settlement for bill " + billId);
            }
            JsonNode node = mapper.readTree(body);
            return new SettlementResult(
                    BigDecimal.valueOf(node.path("balanceSelf").asDouble(0)),
                    BigDecimal.valueOf(node.path("balanceNonSelf").asDouble(0)),
                    node.path("isSettled").asBoolean(false),
                    BigDecimal.valueOf(node.path("changeAmount").asDouble(0)));
        } catch (RestClientException e) {
            throw new BillingCallException("billing-service PUT settlement for bill " + billId + " failed", e);
        } catch (BillingCallException e) {
            throw e;
        } catch (Exception e) {
            throw new BillingCallException("Unparseable billing settlement response for bill " + billId, e);
        }
    }

    /** Subset of the billing bill view this service needs (GAP-REGISTER WP-13). */
    public record BillView(long billId, BigDecimal balanceSelf, BigDecimal balanceNonSelf) {}

    /** Subset of the settlement response: new balances, settled flag and cash change. */
    public record SettlementResult(BigDecimal balanceSelf, BigDecimal balanceNonSelf,
                                   boolean isSettled, BigDecimal changeAmount) {}
}
