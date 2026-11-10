package com.hims.payments.application;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.hims.payments.infrastructure.outbox.OutboxPublisher;
import com.hims.payments.infrastructure.outbox.OutboxService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Payments (Catalog #6 / GAP-REGISTER WP-13).
 *
 * Flow (single DB transaction):
 *  1. validate request (modes sum guard, BR-BILL-007 preconditions);
 *  2. GET outstanding balances from billing-service (BillingClient);
 *  3. BR-BILL-007 advance FIFO: when advanceUse, consume t_advance rows with
 *     balance > 0 in ADVANCE_ID ORDER against the SELF balance first — partial
 *     splits recorded chunk-by-chunk as extra mode-7 rows + t_advance_consumption;
 *  4. allocate the funded total self-first between outstanding balances;
 *  5. call billing PUT /api/v1/internal/bills/{id}/settlement SYNCHRONOUSLY
 *     (BR-BILL-006 settled <=> both balances <= 0, idempotent by recomputation);
 *  6. ONLY AFTER a successful settlement call, append E-08 PaymentReceived to
 *     the transactional outbox IN THE SAME TX (payload keys EXACTLY
 *     {paymentId, billId, patientId, amount, modes[{mode,amount}],
 *     advanceDetails|null, transactionType, changeAmount}, partition key billId).
 *
 * SAGA DEVIATION (documented): the settlement step is request-scoped synchronous
 * REST, not an eventually-retried saga participant — if billing is down the whole
 * payment rolls back and no event is emitted (GAP-REGISTER-4 scope decision).
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    private final PaymentRepository paymentRepository;
    private final PaymentModeDetailRepository modeDetailRepository;
    private final AdvanceRepository advanceRepository;
    private final AdvanceConsumptionRepository consumptionRepository;
    private final BillingClient billingClient;
    private final OutboxService outboxService;

    @PersistenceContext
    private EntityManager em;

    public PaymentService(PaymentRepository paymentRepository,
                          PaymentModeDetailRepository modeDetailRepository,
                          AdvanceRepository advanceRepository,
                          AdvanceConsumptionRepository consumptionRepository,
                          BillingClient billingClient,
                          OutboxService outboxService) {
        this.paymentRepository = paymentRepository;
        this.modeDetailRepository = modeDetailRepository;
        this.advanceRepository = advanceRepository;
        this.consumptionRepository = consumptionRepository;
        this.billingClient = billingClient;
        this.outboxService = outboxService;
    }

    /** One tendered mode line of a payment request. */
    public record ModeLine(Integer mode, BigDecimal amount) {}

    @Transactional
    public Payment pay(Long billId, Long patientId, BigDecimal amount,
                       List<ModeLine> modes, boolean advanceUse) {
        // ---- 1. request validation --------------------------------------------------
        if (billId == null || patientId == null) {
            throw new InvalidPaymentRequestException("PAY_REQ_INVALID", "billId and patientId are required");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidPaymentRequestException("PAY_REQ_INVALID", "amount must be > 0");
        }
        List<ModeLine> lines = modes == null ? List.of() : modes;
        BigDecimal suppliedSum = BigDecimal.ZERO.setScale(2);
        for (ModeLine line : lines) {
            if (line == null || line.mode() == null || line.amount() == null || line.amount().signum() <= 0) {
                throw new InvalidPaymentRequestException("PAY_REQ_INVALID",
                        "Each mode line needs mode and amount > 0");
            }
            suppliedSum = suppliedSum.add(line.amount());
        }
        suppliedSum = suppliedSum.setScale(2, RoundingMode.HALF_UP);
        BigDecimal target = amount.setScale(2, RoundingMode.HALF_UP);
        if (!advanceUse && suppliedSum.compareTo(target) != 0) {
            throw new InvalidPaymentRequestException("PAY_MODE_MISMATCH",
                    "Sum of modes (" + suppliedSum.toPlainString()
                    + ") must equal amount (" + target.toPlainString() + ")");
        }
        if (advanceUse && suppliedSum.compareTo(target) > 0) {
            throw new InvalidPaymentRequestException("PAY_MODE_MISMATCH",
                    "Sum of modes cannot exceed amount when advanceUse is requested");
        }

        // ---- 2. outstanding balances from billing -----------------------------------
        BillingClient.BillView bill = billingClient.getBill(billId);

        // ---- 3. BR-BILL-007 advance FIFO against the SELF balance --------------------
        BigDecimal shortfall = target.subtract(suppliedSum);
        BigDecimal selfOutstanding = positive(bill.balanceSelf());
        BigDecimal advanceCap = shortfall.min(target.min(selfOutstanding)); // advances cover self side only
        List<ConsumedChunk> chunks = new ArrayList<>();
        if (advanceUse && advanceCap.signum() > 0) {
            BigDecimal stillNeeded = advanceCap;
            // FIFO: balance > 0 rows consumed in advance_id order (repository is ordered ASC)
            for (Advance advance : advanceRepository.findByPatientIdOrderByAdvanceIdAsc(patientId)) {
                if (stillNeeded.signum() <= 0) {
                    break;
                }
                BigDecimal available = advance.remaining() == null ? BigDecimal.ZERO : advance.remaining();
                if (available.signum() <= 0) {
                    continue;   // fully used or refunded earlier
                }
                BigDecimal chunk = available.min(stillNeeded).setScale(2, RoundingMode.HALF_UP);
                if (chunk.signum() <= 0) {
                    continue;
                }
                advance.setUsed(nvl(advance.getUsed()).add(chunk));
                advance.setUpdatedBy(AUDIT_ACTOR_ID);
                advance.setUpdatedDatetime(now());
                advanceRepository.save(advance);
                chunks.add(new ConsumedChunk(advance.getAdvanceId(), chunk));
                stillNeeded = stillNeeded.subtract(chunk);
            }
        }

        // ---- 4. self-first allocation of what was actually funded --------------------
        BigDecimal funded = suppliedSum.add(chunks.stream().map(ConsumedChunk::consumed)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        if (funded.signum() <= 0) {
            throw new InvalidPaymentRequestException("PAY_REQ_INVALID",
                    "Nothing to settle: no mode amounts and no usable advance balance");
        }
        funded = funded.setScale(2, RoundingMode.HALF_UP);
        BigDecimal toSelf = funded.min(selfOutstanding);
        BigDecimal toNonSelf = funded.subtract(toSelf).min(positive(bill.balanceNonSelf()));

        // ---- persist payment + mode details (+ consumption ledger) --------------------
        Timestamp nowTs = now();
        Payment payment = new Payment();
        payment.setPaymentId(nextId("t_payment_seq"));
        payment.setBillId(billId);
        payment.setPatientId(patientId);
        payment.setAmount(target);
        payment.setTransactionType(Payment.TYPE_PAYMENT);
        payment.setChangeAmount(BigDecimal.ZERO.setScale(2));
        payment.setStatus(true);
        payment.setSynchronizedd(false);
        payment.setAddedBy(AUDIT_ACTOR_ID);
        payment.setAddedDatetime(nowTs);
        payment = paymentRepository.save(payment);

        List<PaymentModeDetail> persistedModes = new ArrayList<>();
        for (ModeLine line : lines) {
            persistedModes.add(saveMode(payment.getPaymentId(), line.mode().shortValue(),
                    line.amount(), nowTs));
        }
        List<AdvanceConsumption> consumptions = new ArrayList<>();
        for (ConsumedChunk chunk : chunks) {
            // partial split => extra mode-7 row + consumption ledger line
            persistedModes.add(saveMode(payment.getPaymentId(), PaymentModeDetail.MODE_ADVANCE,
                    chunk.consumed(), nowTs));
            AdvanceConsumption c = new AdvanceConsumption();
            c.setConsumptionId(nextId("t_advance_consumption_seq"));
            c.setAdvanceId(chunk.advanceId());
            c.setPaymentId(payment.getPaymentId());
            c.setConsumedAmount(chunk.consumed());
            c.setStatus(true);
            c.setSynchronizedd(false);
            c.setAddedBy(AUDIT_ACTOR_ID);
            c.setAddedDatetime(nowTs);
            consumptions.add(consumptionRepository.save(c));
        }
        payment.setModes(persistedModes);
        payment.setAdvanceConsumptions(consumptions);

        // ---- 5. SYNCHRONOUS settlement call (BR-BILL-006) -----------------------------
        BillingClient.SettlementResult settlement =
                billingClient.settle(billId, toSelf, toNonSelf);
        payment.setChangeAmount(settlement.changeAmount() == null
                ? BigDecimal.ZERO.setScale(2) : settlement.changeAmount());
        payment.setUpdatedBy(AUDIT_ACTOR_ID);
        payment.setUpdatedDatetime(now());
        payment = paymentRepository.save(payment);

        // ---- 6. E-08 outbox append AFTER successful settlement, SAME TX ---------------
        emitPaymentReceived(payment, persistedModes, consumptions);

        log.info("Payment {} recorded for bill {} (self={}, nonSelf={}, advanceChunks={})",
                payment.getPaymentId(), billId, toSelf, toNonSelf, chunks.size());
        return payment;
    }

    /**
     * E-08 PaymentReceived — payload keys EXACTLY {paymentId, billId, patientId,
     * amount, modes[{mode,amount}], advanceDetails[{advanceId,consumed}]|null,
     * transactionType, changeAmount}; partition key billId; emitted once per
     * successful payment, never on failed/rolled-back settlements.
     */
    void emitPaymentReceived(Payment payment,
                             List<PaymentModeDetail> modes,
                             List<AdvanceConsumption> consumptions) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("paymentId", String.valueOf(payment.getPaymentId()));
        payload.put("billId", String.valueOf(payment.getBillId()));
        payload.put("patientId", String.valueOf(payment.getPatientId()));
        payload.put("amount", payment.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString());
        List<Map<String, Object>> modeMaps = new ArrayList<>();
        for (PaymentModeDetail m : modes) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("mode", m.getMode().intValue());
            r.put("amount", m.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString());
            modeMaps.add(r);
        }
        payload.put("modes", modeMaps);
        if (consumptions.isEmpty()) {
            payload.put("advanceDetails", null);
        } else {
            List<Map<String, Object>> adv = new ArrayList<>();
            for (AdvanceConsumption c : consumptions) {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("advanceId", String.valueOf(c.getAdvanceId()));
                r.put("consumed", c.getConsumedAmount().setScale(2, RoundingMode.HALF_UP).toPlainString());
                adv.add(r);
            }
            payload.put("advanceDetails", adv);
        }
        payload.put("transactionType", Payment.TYPE_PAYMENT);
        payload.put("changeAmount", nvl(payment.getChangeAmount()).setScale(2, RoundingMode.HALF_UP).toPlainString());

        outboxService.append(OutboxPublisher.PaymentEvents.PAYMENT_RECEIVED, "payment",
                payment.getPaymentId(), String.valueOf(payment.getBillId()), toJson(payload),
                "payment-received:" + payment.getPaymentId());
    }

    // ------------------------------------------------------------------ queries

    @Transactional(readOnly = true)
    public List<Payment> byBill(Long billId) {
        if (billId == null) {
            throw new InvalidPaymentRequestException("PAY_REQ_INVALID", "billId query parameter is required");
        }
        List<Payment> payments = paymentRepository.findByBillIdOrderByPaymentIdAsc(billId);
        for (Payment p : payments) {
            p.setModes(modeDetailRepository.findByPaymentIdOrderByModeDetailIdAsc(p.getPaymentId()));
            p.setAdvanceConsumptions(consumptionRepository.findByPaymentIdOrderByConsumptionIdAsc(p.getPaymentId()));
        }
        return payments;
    }

    @Transactional(readOnly = true)
    public Payment get(long paymentId) {
        Payment p = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new com.hims.payments.application.exceptions.PaymentEntityNotFoundException(
                        "Payment not found: " + paymentId));
        p.setModes(modeDetailRepository.findByPaymentIdOrderByModeDetailIdAsc(p.getPaymentId()));
        p.setAdvanceConsumptions(consumptionRepository.findByPaymentIdOrderByConsumptionIdAsc(p.getPaymentId()));
        return p;
    }

    // ------------------------------------------------------------------ helpers

    private PaymentModeDetail saveMode(long paymentId, short mode, BigDecimal amount, Timestamp nowTs) {
        PaymentModeDetail m = new PaymentModeDetail();
        m.setModeDetailId(nextId("t_payment_mode_detail_seq"));
        m.setPaymentId(paymentId);
        m.setMode(mode);
        m.setAmount(amount);
        m.setStatus(true);
        m.setSynchronizedd(false);
        m.setAddedBy(AUDIT_ACTOR_ID);
        m.setAddedDatetime(nowTs);
        return modeDetailRepository.save(m);
    }

    private static BigDecimal positive(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v.max(BigDecimal.ZERO);
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return new ObjectMapper().writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
    }

    private Long nextId(String sequenceName) {
        Number n = (Number) em.createNativeQuery("SELECT nextval('ihms_payment." + sequenceName + "')")
                .getSingleResult();
        return n.longValue();
    }

    private static Timestamp now() {
        return Timestamp.from(Instant.now());
    }

    private record ConsumedChunk(Long advanceId, BigDecimal consumed) {}
}
