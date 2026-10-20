package com.hims.payments.application;

import com.hims.payments.application.exceptions.InvalidPaymentRequestException;
import com.hims.payments.domain.model.Advance;
import com.hims.payments.domain.model.Refund;
import com.hims.payments.domain.repository.AdvanceRepository;
import com.hims.payments.domain.repository.RefundRepository;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * L1 — refund rules: BR-BILL-008 BEXAR002 guard on advance refunds,
 * transactionType validation and the E-12 RefundIssued (PRELIMINARY) outbox
 * contract with restoredCharges always [] (billing-side compensation deferred).
 */
class RefundServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private RefundRepository refundRepository;
    private AdvanceRepository advanceRepository;
    private OutboxService outboxService;
    private RefundService service;
    private final AtomicLong seq = new AtomicLong(700);

    @BeforeEach
    void setUp() {
        refundRepository = mock(RefundRepository.class);
        advanceRepository = mock(AdvanceRepository.class);
        outboxService = mock(OutboxService.class);

        EntityManager em = mock(EntityManager.class);
        Query seqQuery = mock(Query.class);
        when(seqQuery.getSingleResult()).thenAnswer(inv -> seq.incrementAndGet());
        when(em.createNativeQuery(contains("nextval"))).thenReturn(seqQuery);

        when(refundRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(advanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service = new RefundService(refundRepository, advanceRepository, outboxService);
        ReflectionTestUtils.setField(service, "em", em);
    }

    private static Advance advance(long id, String amount, String used) {
        Advance a = new Advance();
        a.setAdvanceId(id);
        a.setPatientId(505L);
        a.setAmount(new BigDecimal(amount));
        a.setUsed(new BigDecimal(used));   // remaining() falls back to amount-used pre-flush
        return a;
    }

    @Test
    void transactionType_mustBe3or4() {
        assertThatThrownBy(() -> service.create(2L, 505L, 5L, null, BigDecimal.TEN, "x"))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .extracting(e -> ((InvalidPaymentRequestException) e).getErrorCode())
                .isEqualTo("PAY_REQ_INVALID");
    }

    @Test
    void type3_requiresAdvanceId() {
        assertThatThrownBy(() -> service.create(3L, 505L, null, null, BigDecimal.TEN, "x"))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .hasMessageContaining("advanceId");
    }

    @Test
    void type4_requiresBillId() {
        assertThatThrownBy(() -> service.create(4L, 505L, null, 11L, BigDecimal.TEN, "x"))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .hasMessageContaining("billId");
    }

    @Test
    void type3_zeroBalance_advance_refused_BEXAR002() {
        when(advanceRepository.findById(11L)).thenReturn(Optional.of(advance(11L, "300", "300"))); // balance 0

        assertThatThrownBy(() -> service.create(3L, 505L, null, 11L, BigDecimal.TEN, "refund"))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .extracting(e -> ((InvalidPaymentRequestException) e).getErrorCode())
                .isEqualTo("BEXAR002");
        verify(outboxService, times(0)).append(any(), any(), any(), any(), any(), any());
    }

    @Test
    void type3_amountAboveBalance_refused_BEXAR002() {
        when(advanceRepository.findById(11L)).thenReturn(Optional.of(advance(11L, "100", "0")));

        assertThatThrownBy(() -> service.create(3L, 505L, null, 11L, new BigDecimal("150"), "refund"))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .extracting(e -> ((InvalidPaymentRequestException) e).getErrorCode())
                .isEqualTo("BEXAR002");
    }

    @Test
    void type3_happyPath_reducesRemainingBalance_andEmitsE12() throws Exception {
        Advance a = advance(11L, "500", "200");   // balance 300
        when(advanceRepository.findById(11L)).thenReturn(Optional.of(a));

        Refund r = service.create(3L, 505L, null, 11L, new BigDecimal("100"), "patient leaves");

        assertThat(r.getTransactionType()).isEqualTo((short) 3);
        // balance is generated as amount-used; the refund reduces amount => balance drops to 200
        assertThat(a.getAmount()).isEqualByComparingTo("400");
        assertThat(a.remaining()).isEqualByComparingTo("200");

        ArgumentCaptor<String> type = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> aggregateType = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> partitionKey = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> payloadJson = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> idempotencyKey = ArgumentCaptor.forClass(String.class);
        verify(outboxService, times(1)).append(type.capture(), aggregateType.capture(),
                any(), partitionKey.capture(), payloadJson.capture(), idempotencyKey.capture());

        assertThat(type.getValue()).isEqualTo(OutboxPublisher.PaymentEvents.REFUND_ISSUED);   // EXACT (E-12)
        assertThat(partitionKey.getValue()).isEqualTo("advance:11");   // documented fallback (billId null)
        assertThat(idempotencyKey.getValue()).isEqualTo("refund-issued:" + r.getRefundId());

        JsonNode payload = mapper.readTree(payloadJson.getValue());
        assertThat(payload.fieldNames()).toIterable()
                .containsExactlyInAnyOrder("refundId", "billId", "advanceId", "patientId",
                        "amount", "reason", "restoredCharges");
        assertThat(payload.get("refundId").asText()).isEqualTo(String.valueOf(r.getRefundId()));
        assertThat(payload.get("billId").isNull()).isTrue();
        assertThat(payload.get("advanceId").asText()).isEqualTo("11");
        assertThat(payload.get("amount").asText()).isEqualTo("100.00");
        assertThat(payload.get("reason").asText()).isEqualTo("patient leaves");
        assertThat(payload.get("restoredCharges")).isEmpty();   // deferred billing-side compensation
    }

    @Test
    void type4_billRefund_recordsRow_partitionKeyBillId_restoredChargesEmpty() throws Exception {
        Refund r = service.create(4L, 505L, 9L, null, new BigDecimal("75.25"), "overcharge");

        assertThat(r.getTransactionType()).isEqualTo((short) 4);
        assertThat(r.getBillId()).isEqualTo(9L);

        ArgumentCaptor<String> type = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> partitionKey = ArgumentCaptor.forClass(String.class);
        verify(outboxService, times(1)).append(type.capture(), any(),
                any(), partitionKey.capture(), any(), any());
        assertThat(type.getValue()).isEqualTo("RefundIssued");
        assertThat(partitionKey.getValue()).isEqualTo("9");   // partition key billId

        ArgumentCaptor<String> payloadJson = ArgumentCaptor.forClass(String.class);
        verify(outboxService).append(any(), any(), any(), any(), payloadJson.capture(), any());
        JsonNode payload = mapper.readTree(payloadJson.getValue());
        assertThat(payload.fieldNames()).toIterable()
                .containsExactlyInAnyOrder("refundId", "billId", "advanceId", "patientId",
                        "amount", "reason", "restoredCharges");
        assertThat(payload.get("restoredCharges")).isEmpty();
    }
}
