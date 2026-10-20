package com.hims.payments.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.payments.application.exceptions.InvalidPaymentRequestException;
import com.hims.payments.application.exceptions.PaymentEntityNotFoundException;
import com.hims.payments.domain.model.Advance;
import com.hims.payments.domain.model.Refund;
import com.hims.payments.domain.repository.AdvanceRepository;
import com.hims.payments.domain.repository.RefundRepository;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Refunds (Catalog #6 / GAP-REGISTER WP-13, BR-BILL-008):
 *  - transactionType 3 = ADVANCE refund: requires t_advance.balance > 0, else
 *    legacy error code BEXAR002. The refund reduces the advance's remaining
 *    value; because balance is GENERATED ALWAYS AS (amount - used) STORED and
 *    {@code used} tracks BR-BILL-007 bill consumption, the refund decrements
 *    {@code amount}. (GAP-REGISTER wording "decrements used" interpreted as
 *    reducing the REMAINING balance — decrementing used would INCREASE it;
 *    PRELIMINARY interpretation flagged in the WP-13 report.)
 *  - transactionType 4 = BILL refund: records the refund row only. The
 *    restore-of-charges compensation (charges restored from
 *    *BeforeFinalization snapshots, IsCancelled=true, T_Itemsale reset,
 *    doctor-share status=false) stays billing-side later per BR-BILL-008.
 *
 * Emits E-12 RefundIssued (PRELIMINARY per KAFKA-EVENT-CATALOG): payload keys
 * EXACTLY {refundId, billId|null, advanceId|null, patientId, amount, reason,
 * restoredCharges[]} with restoredCharges always [] until the billing-side
 * compensation lands; partition key billId ("advance:<id>" fallback when the
 * refund is advance-only — documented, column is NOT NULL).
 */
@Service
public class RefundService {

    private static final Logger log = LoggerFactory.getLogger(RefundService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    public static final String ERR_ADVANCE_BALANCE = "BEXAR002";   // legacy code

    private final RefundRepository refundRepository;
    private final AdvanceRepository advanceRepository;
    private final OutboxService outboxService;

    @PersistenceContext
    private EntityManager em;

    public RefundService(RefundRepository refundRepository,
                         AdvanceRepository advanceRepository,
                         OutboxService outboxService) {
        this.refundRepository = refundRepository;
        this.advanceRepository = advanceRepository;
        this.outboxService = outboxService;
    }

    @Transactional
    public Refund create(Long transactionType, Long patientId, Long billId, Long advanceId,
                         BigDecimal amount, String reason) {
        if (transactionType == null || (transactionType != Refund.TYPE_ADVANCE_REFUND
                && transactionType != Refund.TYPE_BILL_REFUND)) {
            throw new InvalidPaymentRequestException("PAY_REQ_INVALID",
                    "transactionType must be 3 (advance refund) or 4 (bill refund)");
        }
        if (patientId == null) {
            throw new InvalidPaymentRequestException("PAY_REQ_INVALID", "patientId is required");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidPaymentRequestException("PAY_REQ_INVALID", "amount must be > 0");
        }
        if (reason == null || reason.isBlank()) {
            throw new InvalidPaymentRequestException("PAY_REQ_INVALID", "reason is required");
        }

        Advance advance = null;
        if (transactionType == Refund.TYPE_ADVANCE_REFUND) {
            if (advanceId == null) {
                throw new InvalidPaymentRequestException("PAY_REQ_INVALID",
                        "advanceId is required for transactionType 3");
            }
            advance = advanceRepository.findById(advanceId)
                    .orElseThrow(() -> new PaymentEntityNotFoundException("Advance not found: " + advanceId));
            // BR-BILL-008 guard: refundable value must exist
            BigDecimal balance = advance.remaining() == null ? BigDecimal.ZERO : advance.remaining();
            if (balance.signum() <= 0) {
                throw new InvalidPaymentRequestException(ERR_ADVANCE_BALANCE,
                        "Advance " + advanceId + " has no refundable balance");
            }
            if (amount.compareTo(balance) > 0) {
                throw new InvalidPaymentRequestException(ERR_ADVANCE_BALANCE,
                        "Refund amount exceeds advance balance " + balance.toPlainString());
            }
        } else if (billId == null) {
            throw new InvalidPaymentRequestException("PAY_REQ_INVALID",
                    "billId is required for transactionType 4");
        }

        Timestamp nowTs = now();
        Refund refund = new Refund();
        refund.setRefundId(nextId("t_refund_seq"));
        refund.setTransactionType(transactionType.shortValue());
        refund.setBillId(billId);
        refund.setAdvanceId(advanceId);
        refund.setPatientId(patientId);
        refund.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
        refund.setReason(reason.trim());
        refund.setStatus(true);
        refund.setSynchronizedd(false);
        refund.setAddedBy(AUDIT_ACTOR_ID);
        refund.setAddedDatetime(nowTs);
        refund = refundRepository.save(refund);

        if (advance != null) {
            // reduce the advance's face value: generated balance drops accordingly
            advance.setAmount(advance.getAmount().subtract(refund.getAmount()));
            advance.setUpdatedBy(AUDIT_ACTOR_ID);
            advance.setUpdatedDatetime(nowTs);
            advanceRepository.save(advance);
            em.flush();            // push the UPDATE down before re-reading the generated column
            em.refresh(advance);
        }

        emitRefundIssued(refund);
        log.info("Refund {} recorded (type {}, amount {}, bill {}, advance {})",
                refund.getRefundId(), transactionType, refund.getAmount(), billId, advanceId);
        return refund;
    }

    /**
     * E-12 RefundIssued — PRELIMINARY event per KAFKA-EVENT-CATALOG §E-12.
     * Payload keys EXACTLY {refundId, billId, advanceId, patientId, amount,
     * reason, restoredCharges}; restoredCharges stays [] until billing-side
     * compensation exists (BR-BILL-008 restore deferred).
     */
    void emitRefundIssued(Refund refund) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("refundId", String.valueOf(refund.getRefundId()));
        payload.put("billId", refund.getBillId() == null ? null : String.valueOf(refund.getBillId()));
        payload.put("advanceId", refund.getAdvanceId() == null ? null : String.valueOf(refund.getAdvanceId()));
        payload.put("patientId", String.valueOf(refund.getPatientId()));
        payload.put("amount", refund.getAmount().toPlainString());
        payload.put("reason", refund.getReason());
        payload.put("restoredCharges", List.of());   // deferred: billing-side compensation

        // partition key billId per catalog; documented fallback for advance-only refunds
        String partitionKey = refund.getBillId() != null
                ? String.valueOf(refund.getBillId())
                : "advance:" + refund.getAdvanceId();
        outboxService.append(OutboxPublisher.PaymentEvents.REFUND_ISSUED, "refund",
                refund.getRefundId(), partitionKey, toJson(payload),
                "refund-issued:" + refund.getRefundId());
    }

    // ------------------------------------------------------------------ queries

    @Transactional(readOnly = true)
    public List<Refund> byBill(Long billId) {
        if (billId == null) {
            throw new InvalidPaymentRequestException("PAY_REQ_INVALID", "billId query parameter is required");
        }
        return refundRepository.findByBillIdOrderByRefundIdAsc(billId);
    }

    @Transactional(readOnly = true)
    public Refund get(long refundId) {
        return refundRepository.findById(refundId)
                .orElseThrow(() -> new PaymentEntityNotFoundException("Refund not found: " + refundId));
    }

    // ------------------------------------------------------------------ helpers

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
}
