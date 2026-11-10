package com.hims.payments.application;

import com.hims.payments.application.exceptions.BillingCallException;
import com.hims.payments.application.exceptions.InvalidPaymentRequestException;
import com.hims.payments.domain.model.Advance;
import com.hims.payments.domain.model.AdvanceConsumption;
import com.hims.payments.domain.model.Payment;
import com.hims.payments.domain.model.PaymentModeDetail;
import com.hims.payments.domain.repository.AdvanceConsumptionRepository;
import com.hims.payments.domain.repository.AdvanceRepository;
import com.hims.payments.domain.repository.PaymentModeDetailRepository;
import com.hims.payments.domain.repository.PaymentRepository;
import com.hims.payments.infrastructure.outbox.OutboxEvent;
import com.hims.payments.infrastructure.outbox.OutboxPublisher;
import com.hims.payments.infrastructure.outbox.OutboxService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * L1 — payment rules without Spring context: modes-sum guard, BR-BILL-007
 * advance FIFO (full / partial / multi-advance id order against the SELF side),
 * synchronous billing settlement stubbed with MockRestServiceServer (register
 * requirement) and the EXACT E-08 outbox contract emitted ONLY after a
 * successful settlement.
 */
class PaymentServiceTest {

    private static final String BILLING_BASE = "http://billing";

    private final ObjectMapper mapper = new ObjectMapper();

    private PaymentRepository paymentRepository;
    private PaymentModeDetailRepository modeDetailRepository;
    private AdvanceRepository advanceRepository;
    private AdvanceConsumptionRepository consumptionRepository;
    private OutboxService outboxService;
    private MockRestServiceServer billingServer;
    private PaymentService service;
    private final AtomicLong seq = new AtomicLong(900);

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PaymentRepository.class);
        modeDetailRepository = mock(PaymentModeDetailRepository.class);
        advanceRepository = mock(AdvanceRepository.class);
        consumptionRepository = mock(AdvanceConsumptionRepository.class);
        outboxService = mock(OutboxService.class);

        EntityManager em = mock(EntityManager.class);
        Query seqQuery = mock(Query.class);
        when(seqQuery.getSingleResult()).thenAnswer(inv -> seq.incrementAndGet());
        when(em.createNativeQuery(contains("nextval"))).thenReturn(seqQuery);

        RestClient.Builder builder = RestClient.builder().baseUrl(BILLING_BASE);
        billingServer = MockRestServiceServer.bindTo(builder).build();
        BillingClient billingClient = new BillingClient(builder.build());

        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(modeDetailRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(consumptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(advanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service = new PaymentService(paymentRepository, modeDetailRepository,
                advanceRepository, consumptionRepository, billingClient, outboxService);
        ReflectionTestUtils.setField(service, "em", em);
    }

    // ------------------------------------------------------------- fixtures

    private static Advance advance(long id, long patientId, String amount, String used) {
        Advance a = new Advance();
        a.setAdvanceId(id);
        a.setPatientId(patientId);
        a.setAmount(new BigDecimal(amount));
        a.setUsed(new BigDecimal(used));   // remaining() falls back to amount-used pre-flush
        return a;
    }

    private void stubBill(long billId, String balanceSelf, String balanceNonSelf) {
        billingServer.expect(requestTo(BILLING_BASE + "/api/v1/bills/" + billId))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"billId\":" + billId
                        + ",\"balanceSelf\":" + balanceSelf
                        + ",\"balanceNonSelf\":" + balanceNonSelf + "}", MediaType.APPLICATION_JSON));
    }

    private void stubSettle(long billId, String expectedPaidSelf, String expectedPaidNonSelf,
                            boolean settled, String changeAmount) {
        billingServer.expect(requestTo(BILLING_BASE + "/api/v1/internal/bills/" + billId + "/settlement"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().json("{\"paidSelf\":" + expectedPaidSelf
                        + ",\"paidNonSelf\":" + expectedPaidNonSelf + "}"))
                .andRespond(withSuccess("{\"billId\":" + billId
                        + ",\"balanceSelf\":0,\"balanceNonSelf\":0"
                        + ",\"isSettled\":" + settled
                        + ",\"changeAmount\":" + changeAmount + "}", MediaType.APPLICATION_JSON));
    }

    // ------------------------------------------------------------- validation

    @Test
    void pay_modesSumMustEqualAmount_400_PAY_MODE_MISMATCH() {
        assertThatThrownBy(() -> service.pay(5L, 505L, new BigDecimal("100"),
                        List.of(new PaymentService.ModeLine(1, new BigDecimal("90"))), false))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .extracting(e -> ((InvalidPaymentRequestException) e).getErrorCode())
                .isEqualTo("PAY_MODE_MISMATCH");
        billingServer.verify();   // no HTTP call happened either
    }

    @Test
    void pay_modesExceedingAmountWithAdvanceUse_rejected() {
        assertThatThrownBy(() -> service.pay(5L, 505L, new BigDecimal("100"),
                        List.of(new PaymentService.ModeLine(1, new BigDecimal("120"))), true))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .extracting(e -> ((InvalidPaymentRequestException) e).getErrorCode())
                .isEqualTo("PAY_MODE_MISMATCH");
    }

    @Test
    void pay_nonPositiveAmount_rejected_beforeAnyHttpCall() {
        assertThatThrownBy(() -> service.pay(5L, 505L, BigDecimal.ZERO,
                List.of(new PaymentService.ModeLine(1, BigDecimal.ONE)), false))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .hasMessageContaining("amount");
    }

    // ------------------------------------------------------------- BR-BILL-007 FIFO

    @Test
    void fifo_singleAdvance_coversShortfall_againstSelfBalanceOnly() throws Exception {
        stubBill(5L, "600", "400");
        // self-first allocation of amount=600 between outstanding balances: self 600, non-self 0
        stubSettle(5L, "600", "0", true, "0");

        Advance a1 = advance(11L, 505L, "300", "0");
        when(advanceRepository.findByPatientIdOrderByAdvanceIdAsc(505L)).thenReturn(List.of(a1));

        // cash 300 + advance fills the 300 shortfall -> funded 600 covers the whole self side
        Payment p = service.pay(5L, 505L, new BigDecimal("600"), List.of(
                new PaymentService.ModeLine(1, new BigDecimal("300"))), true);

        // advance fully consumed
        assertThat(a1.getUsed()).isEqualByComparingTo("300");
        // mode rows: supplied cash + ONE extra mode-7 row for the split
        var capturedModes = p.getModes();
        assertThat(capturedModes).hasSize(2);
        assertThat(capturedModes.get(0).getMode()).isEqualTo((short) 1);
        assertThat(capturedModes.get(1).getMode()).isEqualTo(PaymentModeDetail.MODE_ADVANCE);
        assertThat(capturedModes.get(1).getAmount()).isEqualByComparingTo("300");
        // consumption ledger
        assertThat(p.getAdvanceConsumptions()).hasSize(1);
        assertThat(p.getAdvanceConsumptions().get(0).getAdvanceId()).isEqualTo(11L);
        assertThat(p.getAdvanceConsumptions().get(0).getConsumedAmount()).isEqualByComparingTo("300");

        JsonNode payload = assertSingleE08(p, "600.00");
        assertThat(payload.get("modes")).hasSize(2);
        assertThat(payload.get("modes").get(1).fieldNames())
                .toIterable().containsExactlyInAnyOrder("mode", "amount");
        assertThat(payload.get("modes").get(1).get("mode").asInt()).isEqualTo(7);
        assertThat(payload.get("advanceDetails")).hasSize(1);
        assertThat(payload.get("advanceDetails").get(0).get("advanceId").asText()).isEqualTo("11");
        assertThat(payload.get("advanceDetails").get(0).get("consumed").asText()).isEqualTo("300.00");
        assertThat(payload.get("transactionType").asInt()).isEqualTo(2);
        assertThat(payload.get("changeAmount").asText()).isEqualTo("0.00");
        billingServer.verify();
    }

    @Test
    void fifo_partialSplit_advanceBiggerThanNeed() throws Exception {
        stubBill(5L, "500", "500");
        stubSettle(5L, "500", "0", false, "0");

        Advance a1 = advance(11L, 505L, "1000", "0");   // far more than needed
        when(advanceRepository.findByPatientIdOrderByAdvanceIdAsc(505L)).thenReturn(List.of(a1));

        Payment p = service.pay(5L, 505L, new BigDecimal("500"),
                List.of(), true);   // pure-advance payment

        // only 500 of the 1000 advance consumed -> PARTIAL split as one mode-7 row
        assertThat(a1.getUsed()).isEqualByComparingTo("500");
        assertThat(a1.remaining()).isEqualByComparingTo("500");
        assertThat(p.getModes()).hasSize(1);
        assertThat(p.getModes().get(0).getMode()).isEqualTo(PaymentModeDetail.MODE_ADVANCE);

        JsonNode payload = assertSingleE08(p, "500.00");
        assertThat(payload.get("advanceDetails").get(0).get("consumed").asText()).isEqualTo("500.00");
        billingServer.verify();
    }

    @Test
    void fifo_multipleAdvances_consumedInIdOrder_withSplitAcrossBoth() throws Exception {
        stubBill(5L, "900", "200");
        stubSettle(5L, "900", "0", false, "0");

        // NOTE: returned deliberately OUT of id order to prove the FIFO sort contract
        Advance a2 = advance(22L, 505L, "400", "0");
        Advance a1 = advance(11L, 505L, "300", "0");
        when(advanceRepository.findByPatientIdOrderByAdvanceIdAsc(505L)).thenReturn(List.of(a1, a2));

        Payment p = service.pay(5L, 505L, new BigDecimal("900"), List.of(
                new PaymentService.ModeLine(2, new BigDecimal("200"))), true);

        // shortfall 700: a1 (id 11) drained FIRST (300), then a2 takes 400
        assertThat(a1.getUsed()).isEqualByComparingTo("300");
        assertThat(a2.getUsed()).isEqualByComparingTo("400");

        assertThat(p.getModes()).hasSize(3);   // cash + two mode-7 chunks
        assertThat(p.getModes().stream().filter(m -> m.getMode() == PaymentModeDetail.MODE_ADVANCE)
                .map(m -> m.getAmount()).allMatch(x -> x.signum() > 0)).isTrue();

        JsonNode payload = assertSingleE08(p, "900.00");
        assertThat(payload.get("advanceDetails")).hasSize(2);
        assertThat(payload.get("advanceDetails").get(0).get("advanceId").asText()).isEqualTo("11");
        assertThat(payload.get("advanceDetails").get(0).get("consumed").asText()).isEqualTo("300.00");
        assertThat(payload.get("advanceDetails").get(1).get("advanceId").asText()).isEqualTo("22");
        assertThat(payload.get("advanceDetails").get(1).get("consumed").asText()).isEqualTo("400.00");
        billingServer.verify();
    }

    @Test
    void fifo_zeroBalanceAdvances_skipped() throws Exception {
        stubBill(5L, "100", "0");
        stubSettle(5L, "100", "0", false, "0");

        Advance empty = advance(10L, 505L, "500", "500");   // balance 0 -> skipped
        Advance good = advance(12L, 505L, "150", "0");
        when(advanceRepository.findByPatientIdOrderByAdvanceIdAsc(505L)).thenReturn(List.of(empty, good));

        Payment p = service.pay(5L, 505L, new BigDecimal("100"), List.of(), true);

        assertThat(empty.getUsed()).isEqualByComparingTo("500");       // untouched
        assertThat(good.getUsed()).isEqualByComparingTo("100");        // FIFO continues past empty
        JsonNode payload = assertSingleE08(p, "100.00");
        assertThat(payload.get("advanceDetails").get(0).get("advanceId").asText()).isEqualTo("12");
        billingServer.verify();
    }

    @Test
    void pay_withoutAdvanceUse_emitsNullAdvanceDetails_andRecordsChange() throws Exception {
        stubBill(5L, "80", "20");
        stubSettle(5L, "80", "20", true, "15.50");

        Payment p = service.pay(5L, 505L, new BigDecimal("100"), List.of(
                new PaymentService.ModeLine(1, new BigDecimal("100"))), false);

        assertThat(p.getChangeAmount()).isEqualByComparingTo("15.50");
        JsonNode payload = assertSingleE08(p, "100.00");
        assertThat(payload.get("advanceDetails").isNull()).isTrue();
        assertThat(payload.get("modes")).hasSize(1);
        assertThat(payload.get("modes").get(0).get("mode").asInt()).isEqualTo(1);
        assertThat(payload.get("changeAmount").asText()).isEqualTo("15.50");
        billingServer.verify();
    }

    // ------------------------------------------------------------- settlement failure

    @Test
    void settle_failure_rollsBack_noOutboxEvent() {
        stubBill(5L, "600", "400");
        billingServer.expect(requestTo(BILLING_BASE + "/api/v1/internal/bills/5/settlement"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withServerError());

        when(advanceRepository.findByPatientIdOrderByAdvanceIdAsc(505L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.pay(5L, 505L, new BigDecimal("100"),
                        List.of(new PaymentService.ModeLine(1, new BigDecimal("100"))), false))
                .isInstanceOf(BillingCallException.class);

        // documented saga deviation: NOTHING recorded, NO E-08 when settlement fails
        verify(outboxService, never()).append(any(), any(), any(), any(), any(), any());
    }

    // ------------------------------------------------------------- helpers

    /** Exactly one E-08 appended, with the exact envelope + payload key contract. */
    private JsonNode assertSingleE08(Payment p, String expectedAmount) throws Exception {
        ArgumentCaptor<String> type = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> aggregateType = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> aggregateId = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> partitionKey = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> payloadJson = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> idempotencyKey = ArgumentCaptor.forClass(String.class);
        verify(outboxService, times(1)).append(type.capture(), aggregateType.capture(),
                aggregateId.capture(), partitionKey.capture(), payloadJson.capture(),
                idempotencyKey.capture());

        assertThat(type.getValue()).isEqualTo(OutboxPublisher.PaymentEvents.PAYMENT_RECEIVED);  // EXACT (E-08)
        assertThat(aggregateType.getValue()).isEqualTo("payment");
        assertThat(partitionKey.getValue()).isEqualTo(String.valueOf(p.getBillId()));            // billId
        assertThat(idempotencyKey.getValue()).isEqualTo("payment-received:" + p.getPaymentId());

        JsonNode payload = mapper.readTree(payloadJson.getValue());
        assertThat(payload.fieldNames()).toIterable()
                .containsExactlyInAnyOrder("paymentId", "billId", "patientId", "amount",
                        "modes", "advanceDetails", "transactionType", "changeAmount");
        assertThat(payload.get("paymentId").asText()).isEqualTo(String.valueOf(p.getPaymentId()));
        assertThat(payload.get("billId").asText()).isEqualTo(String.valueOf(p.getBillId()));
        assertThat(payload.get("patientId").asText()).isEqualTo(String.valueOf(p.getPatientId()));
        assertThat(payload.get("amount").asText()).isEqualTo(expectedAmount);
        return payload;
    }
}
