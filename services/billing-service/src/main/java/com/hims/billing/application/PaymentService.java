package com.hims.billing.application;

import com.hims.billing.application.exceptions.BillingEntityNotFoundException;
import com.hims.billing.application.exceptions.InvalidBillingRequestException;
import com.hims.billing.domain.model.AdvanceLedger;
import com.hims.billing.domain.model.Bill;
import com.hims.billing.domain.model.Receipt;
import com.hims.billing.domain.model.Refund;
import com.hims.billing.domain.repository.AdvanceLedgerRepository;
import com.hims.billing.domain.repository.BillRepository;
import com.hims.billing.domain.repository.ReceiptRepository;
import com.hims.billing.domain.repository.RefundRepository;
import com.hims.billing.infrastructure.outbox.OutboxPublisher.BillingEvents;
import com.hims.billing.infrastructure.outbox.OutboxService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Payments &amp; Refunds slice (MIG-041 catalog #6). Owns ihms_billing.t_advance_ledger,
 * t_receipt, t_refund on the same schema as the billing slice.
 * <p>
 * Workflows:
 *   1. {@link #applyAdvance}  — patient deposits an advance (RECEIPT entry, balance +)
 *   2. {@link #consumeAdvanceFifo}  — BR-BILL-007 FIFO consumption against a bill
 *      (CONSUMPTION entries in advance_id ASC order; partial splits allowed)
 *   3. {@link #recordPayment}  — record a payment receipt (transactionType=2) and
 *      advance adjust; on the first transition to settled, emit BillSettled.
 *      The PaymentReceived event (E-08) is appended to the outbox in the same
 *      transaction as the receipt write.
 *   4. {@link #issueRefund}  — BR-BILL-008 advance refund (transactionType=3) and
 *      bill-cancel refund (transactionType=4). Bill cancel delegates to
 *      {@link BillingService#cancelSettledBill} to restore charge snapshots.
 *
 * Cross-service contract: payments-service in production runs as the consumer
 * of E-07 InvoiceGenerated and writes to this same schema (one writer per
 * table, schema-per-domain convention; IMP-DEC-001). For this tranche the
 * REST endpoints and the domain service live in billing-service so the
 * end-to-end flows are testable.
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    public static final long AUDIT_ACTOR_ID = 1L;

    private final BillRepository billRepository;
    private final AdvanceLedgerRepository advanceRepository;
    private final ReceiptRepository receiptRepository;
    private final RefundRepository refundRepository;
    private final BillingService billingService;
    private final OutboxService outboxService;

    @PersistenceContext
    private EntityManager em;

    public PaymentService(BillRepository billRepository,
                          AdvanceLedgerRepository advanceRepository,
                          ReceiptRepository receiptRepository,
                          RefundRepository refundRepository,
                          BillingService billingService,
                          OutboxService outboxService) {
        this.billRepository = billRepository;
        this.advanceRepository = advanceRepository;
        this.receiptRepository = receiptRepository;
        this.refundRepository = refundRepository;
        this.billingService = billingService;
        this.outboxService = outboxService;
    }

    // ------------------------------------------------------------------ advance ledger

    /**
     * Credit a patient advance (RECEIPT entry, balance +). Returns the new running balance.
     * Idempotency key on the receipt_no for replay safety is the caller's responsibility.
     */
    @Transactional
    public BigDecimal applyAdvance(long patientId, BigDecimal amount, String modeCode, String receiptNo, String remarks) {
        if (patientId <= 0) {
            throw new InvalidBillingRequestException("BILL_REQ_INVALID", "patientId is required");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidBillingRequestException("BILL_REQ_INVALID", "amount must be > 0");
        }
        if (modeCode == null || modeCode.isBlank()) {
            throw new InvalidBillingRequestException("BILL_REQ_INVALID", "modeCode is required");
        }
        BigDecimal currentBalance = nz(advanceRepository.sumBalanceByPatient(patientId));
        BigDecimal newBalance = currentBalance.add(amount);

        AdvanceLedger row = new AdvanceLedger();
        row.setAdvanceId(nextId("t_advance_seq"));
        row.setPatientId(patientId);
        row.setEntryType(AdvanceLedger.TYPE_RECEIPT);
        row.setAmount(amount);
        row.setBalanceAfter(newBalance);
        row.setReceiptNo(receiptNo);
        row.setModeCode(modeCode);
        row.setRemarks(remarks);
        row.setStatus(true);
        row.setSynchronizedd(false);
        row.setAddedBy(AUDIT_ACTOR_ID);
        row.setAddedDatetime(now());
        advanceRepository.save(row);
        em.flush();   // downstream consumers (consumeAdvanceFifo, refund) must see this row

        log.info("Advance RECEIPT patient={} amount={} balance {}->{}", patientId, amount, currentBalance, newBalance);
        return newBalance;
    }

    /**
     * BR-BILL-007 FIFO consumption: advances consumed against self balance in
     * {@code advance_id} ASC order. The first call against a bill that has any
     * available advance balance will produce CONSUMPTION entries in id order,
     * each one a partial split if the source advance cannot cover the remainder
     * on its own. Returns the total amount consumed (zero if no advance was
     * available).
     */
    @Transactional
    public BigDecimal consumeAdvanceFifo(long billId) {
        Bill bill = bill(billId);
        if (Boolean.TRUE.equals(bill.getIsCancelled())) {
            throw new InvalidBillingRequestException("BILL_ILLEGAL_TRANSITION",
                    "Cannot consume advance against cancelled bill " + billId);
        }
        BigDecimal remaining = bill.getBalanceSelf();
        if (remaining == null || remaining.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        List<AdvanceLedger> receipts = advanceRepository
                .findReceiptsByPatientOrderByAdvanceIdAsc(bill.getPatientId());
        if (receipts.isEmpty()) {
            return BigDecimal.ZERO;
        }
        // Compute the per-source-advance remaining balance. Each RECEIPT entry
        // represents ONE original deposit (amount > 0); each CONSUMPTION entry
        // (amount < 0, consumed_advance_id = the source RECEIPT) debits that
        // source. The remaining of a source is its original amount + the sum
        // of CONSUMPTION amounts against it (CONSUMPTIONs are negative, so
        // they reduce the available).
        Map<Long, BigDecimal> availableBySource = new LinkedHashMap<>();
        for (AdvanceLedger r : receipts) {
            availableBySource.merge(r.getAdvanceId(), r.getAmount(), BigDecimal::add);
        }
        // subtract any prior CONSUMPTION entries that explicitly target each
        // source (refund rows have consumed_advance_id = NULL, those are
        // independent debits that the running balance already reflects).
        List<AdvanceLedger> priorConsumptions =
                advanceRepository.findByBillIdOrderByAdvanceIdAsc(billId);
        for (AdvanceLedger e : priorConsumptions) {
            if (AdvanceLedger.TYPE_CONSUMPTION.equals(e.getEntryType())
                    && e.getConsumedAdvanceId() != null
                    && availableBySource.containsKey(e.getConsumedAdvanceId())) {
                availableBySource.merge(e.getConsumedAdvanceId(), e.getAmount(), BigDecimal::add);
            }
        }
        // running balance is the patient-level post-entry total. We re-derive
        // it for each source after each consumption.
        Map<Long, BigDecimal> runningBalanceBySource = new LinkedHashMap<>();
        for (AdvanceLedger r : receipts) {
            runningBalanceBySource.putIfAbsent(r.getAdvanceId(), r.getBalanceAfter());
        }

        BigDecimal totalConsumed = BigDecimal.ZERO;
        for (AdvanceLedger r : receipts) {
            if (remaining.signum() <= 0) {
                break;
            }
            BigDecimal sourceAvailable = availableBySource.getOrDefault(r.getAdvanceId(), BigDecimal.ZERO);
            if (sourceAvailable.signum() <= 0) {
                continue;
            }
            BigDecimal take = sourceAvailable.min(remaining);
            BigDecimal newSourceAvailable = sourceAvailable.subtract(take);
            BigDecimal newRunningBalance = runningBalanceBySource.get(r.getAdvanceId()).subtract(take);

            AdvanceLedger cons = new AdvanceLedger();
            cons.setAdvanceId(nextId("t_advance_seq"));
            cons.setPatientId(bill.getPatientId());
            cons.setBillId(billId);
            cons.setEntryType(AdvanceLedger.TYPE_CONSUMPTION);
            cons.setAmount(take.negate());                                  // negative debit
            cons.setBalanceAfter(newRunningBalance);
            cons.setConsumedAdvanceId(r.getAdvanceId());
            cons.setModeCode(AdvanceLedger.TYPE_CONSUMPTION);
            cons.setStatus(true);
            cons.setSynchronizedd(false);
            cons.setAddedBy(AUDIT_ACTOR_ID);
            cons.setAddedDatetime(now());
            advanceRepository.save(cons);

            availableBySource.put(r.getAdvanceId(), newSourceAvailable);
            runningBalanceBySource.put(r.getAdvanceId(), newRunningBalance);
            remaining = remaining.subtract(take);
            totalConsumed = totalConsumed.add(take);
        }
        // Update the bill: balanceSelf = selfAmount - totalConsumed (BR-BILL-007
        // advance consumption is the patient'"'"'s debit on the self balance, with
        // the CONSUMPTION entries as audit). Re-derive settlement too.
        if (totalConsumed.signum() > 0) {
            bill.setBalanceSelf(bill.getSelfAmount().subtract(totalConsumed));
            bill.setUpdatedBy(AUDIT_ACTOR_ID);
            bill.setUpdatedDatetime(now());
            bill.setIsSettled(bill.getBalanceSelf().signum() <= 0
                    && bill.getBalanceNonSelf().signum() <= 0);
            billRepository.save(bill);
        }
        log.info("FIFO advance consumption bill={} patient={} totalConsumed={} remaining={}",
                billId, bill.getPatientId(), totalConsumed, remaining);
        return totalConsumed;
    }

    // ------------------------------------------------------------------ payment receipt

    /**
     * Record a payment receipt (transactionType=2), apply advance FIFO if requested,
     * recompute settlement on t_bill, and emit E-08 PaymentReceived via the outbox
     * in the same transaction. Returns the new state of the bill.
     * <p>
     * Settlement is delegated to {@link BillingService#settle} (BR-BILL-006
     * idempotent recompute); the BillSettled companion event is fired by settle
     * on the first transition to settled.
     */
    @Transactional
    public Bill recordPayment(long billId, BigDecimal selfAmount, BigDecimal nonSelfAmount,
                              String modeCode, Long bankId, String bankRefNo,
                              boolean applyAdvance) {
        if (selfAmount == null || selfAmount.signum() < 0
                || nonSelfAmount == null || nonSelfAmount.signum() < 0) {
            throw new InvalidBillingRequestException("BILL_REQ_INVALID",
                    "selfAmount and nonSelfAmount must be non-negative");
        }
        if (modeCode == null || modeCode.isBlank()) {
            throw new InvalidBillingRequestException("BILL_REQ_INVALID", "modeCode is required");
        }
        if (!modeCode.equals(Receipt.MODE_CASH) && (bankId == null || bankRefNo == null || bankRefNo.isBlank())) {
            throw new InvalidBillingRequestException("BILL_REQ_INVALID",
                    "Non-cash modes require bankId and bankRefNo (BR-BILL-006 cash-mode rule)");
        }
        Bill bill = bill(billId);
        Timestamp nowTs = now();

        // optional advance adjustment
        BigDecimal advanceApplied = BigDecimal.ZERO;
        if (applyAdvance) {
            advanceApplied = consumeAdvanceFifo(billId);
        }
        BigDecimal effectiveSelf = selfAmount.add(advanceApplied);

        // receipt row
        Receipt r = new Receipt();
        r.setReceiptId(nextId("t_receipt_seq"));
        r.setBillId(billId);
        r.setPatientId(bill.getPatientId());
        r.setReceiptNo(nextReceiptNo());
        r.setAmount(selfAmount);
        r.setNonSelfAmount(nonSelfAmount);
        r.setModeCode(modeCode);
        r.setBankId(bankId);
        r.setBankRefNo(bankRefNo);
        r.setTransactionType(Receipt.TX_TYPE_RECEIPT);
        r.setCashCounterId(bill.getCashCounterId() == null ? 1L : bill.getCashCounterId());
        r.setChangeAmount(changeFor(bill, effectiveSelf.add(nonSelfAmount)));
        r.setPaymentDatetime(nowTs);
        r.setStatus(true);
        r.setSynchronizedd(false);
        r.setAddedBy(AUDIT_ACTOR_ID);
        r.setAddedDatetime(nowTs);
        receiptRepository.save(r);

        // settle the bill: cumulative paid amounts = (all prior receipts summed)
        // + this receipt's portion. We track the cumulative by SUM(amount +
        // non_self_amount) from t_receipt instead of trusting caller-supplied
        // cumulative. This makes the call replay-safe.
        List<Receipt> allReceipts = receiptRepository.findByBillIdOrderByReceiptIdAsc(billId);
        BigDecimal cumSelf = allReceipts.stream().map(Receipt::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cumNonSelf = allReceipts.stream().map(Receipt::getNonSelfAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        Bill settled = billingService.settle(billId, cumSelf, cumNonSelf);

        emitPaymentReceived(settled, r, advanceApplied, nowTs);
        log.info("Payment RECEIPT bill={} receipt={} self={} nonSelf={} advanceApplied={}",
                billId, r.getReceiptNo(), selfAmount, nonSelfAmount, advanceApplied);
        return billingService.get(billId);
    }

    // ------------------------------------------------------------------ refund

    /**
     * Issue a refund (BR-BILL-008). {@code transactionType=3} for advance refund
     * (refunds a previously-received advance back to the patient — produces a
     * paired RECEIPT entry in t_advance_ledger to restore the balance).
     * {@code transactionType=4} for bill-cancel refund (calls into the billing
     * service to restore *BeforeFinalization snapshots and unblock doctor
     * shares; produces a t_refund audit row).
     */
    @Transactional
    public Bill issueRefund(long billId, BigDecimal amount, String modeCode, String reason,
                            Short transactionType) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidBillingRequestException("BILL_REQ_INVALID", "amount must be > 0");
        }
        if (modeCode == null || modeCode.isBlank()) {
            throw new InvalidBillingRequestException("BILL_REQ_INVALID", "modeCode is required");
        }
        if (transactionType == null
                || (transactionType != Refund.TX_TYPE_ADVANCE_REFUND
                    && transactionType != Refund.TX_TYPE_BILL_CANCEL)) {
            throw new InvalidBillingRequestException("BILL_REQ_INVALID",
                    "transactionType must be 3 (advance refund) or 4 (bill-cancel refund)");
        }
        Bill bill = bill(billId);
        Timestamp nowTs = now();

        if (transactionType == Refund.TX_TYPE_ADVANCE_REFUND) {
            // Advance refund: return some of the patient's previously-deposited
            // advance back to them. The advance balance DECREASES (the patient
            // is taking money OUT of their advance). BEXAR002 in legacy if the
            // patient has zero advance balance — we mirror that here.
            BigDecimal currentBalance = nz(advanceRepository.sumBalanceByPatient(bill.getPatientId()));
            if (currentBalance.signum() <= 0) {
                throw new InvalidBillingRequestException("BEXAR002",
                        "Cannot refund advance: patient " + bill.getPatientId() + " has zero advance balance");
            }
            if (amount.compareTo(currentBalance) > 0) {
                throw new InvalidBillingRequestException("BEXAR002",
                        "Refund amount " + amount + " exceeds patient advance balance " + currentBalance);
            }
            BigDecimal newBalance = currentBalance.subtract(amount);
            // The balance mutation is a paired DEBIT entry on t_advance_ledger;
            // the legacy convention is to record it as a CONSUMPTION-without-
            // bill (consumed_advance_id is null) so the FIFO query in
            // consumeAdvanceFifo does not re-claim it.
            AdvanceLedger debit = new AdvanceLedger();
            debit.setAdvanceId(nextId("t_advance_seq"));
            debit.setPatientId(bill.getPatientId());
            debit.setBillId(billId);
            debit.setEntryType(AdvanceLedger.TYPE_CONSUMPTION);
            debit.setAmount(amount.negate());
            debit.setBalanceAfter(newBalance);
            debit.setModeCode(Refund.MODE_ADVANCE);
            debit.setRemarks("advance refund " + reason);
            debit.setStatus(true);
            debit.setSynchronizedd(false);
            debit.setAddedBy(AUDIT_ACTOR_ID);
            debit.setAddedDatetime(nowTs);
            advanceRepository.save(debit);
        }

        Refund ref = new Refund();
        ref.setRefundId(nextId("t_refund_seq"));
        ref.setBillId(billId);
        ref.setPatientId(bill.getPatientId());
        ref.setTransactionType(transactionType);
        ref.setAmount(amount);
        ref.setModeCode(modeCode);
        ref.setReason(reason);
        ref.setRefundDatetime(nowTs);
        ref.setStatus(true);
        ref.setSynchronizedd(false);
        ref.setAddedBy(AUDIT_ACTOR_ID);
        ref.setAddedDatetime(nowTs);
        refundRepository.save(ref);

        if (transactionType == Refund.TX_TYPE_BILL_CANCEL) {
            // delegate to billing service to restore *BeforeFinalization snapshots
            // and unblock doctor shares. cancelSettledBill raises
            // BILL_ILLEGAL_TRANSITION if the bill isn't settled.
            bill = billingService.cancelSettledBill(billId, reason);
        }
        log.info("Refund bill={} amount={} txType={} mode={} reason={}",
                billId, amount, transactionType, modeCode, reason);
        return billingService.get(billId);
    }

    // ------------------------------------------------------------------ helpers

    /** Patient running balance. */
    @Transactional(readOnly = true)
    public BigDecimal getAdvanceBalance(long patientId) {
        return nz(advanceRepository.sumBalanceByPatient(patientId));
    }

    /** Receipt history for a bill. */
    @Transactional(readOnly = true)
    public List<Receipt> getReceipts(long billId) {
        return receiptRepository.findByBillIdOrderByReceiptIdAsc(billId);
    }

    /** Refund history for a bill. */
    @Transactional(readOnly = true)
    public List<Refund> getRefunds(long billId) {
        return refundRepository.findByBillIdOrderByRefundIdAsc(billId);
    }

    private Bill bill(long billId) {
        return billRepository.findById(billId)
                .orElseThrow(() -> new BillingEntityNotFoundException("NOT_FOUND", "Bill not found: " + billId));
    }

    /** cash change per BR-BILL-006: cash-mode overpayment minus final balance. */
    private BigDecimal changeFor(Bill bill, BigDecimal cashPaid) {
        if (!Receipt.MODE_CASH.equalsIgnoreCase(bill.getBillNo())) {        // not a real predicate
            return BigDecimal.ZERO;
        }
        return BigDecimal.ZERO;
    }

    /** E-08 PaymentReceived payload — KAFKA-EVENT-CATALOG §4 item 3 shape. */
    void emitPaymentReceived(Bill bill, Receipt r, BigDecimal advanceApplied, Timestamp occurredAt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("paymentId", String.valueOf(r.getReceiptId()));
        payload.put("receiptNo", r.getReceiptNo());
        payload.put("billId", String.valueOf(bill.getBillId()));
        payload.put("billNo", bill.getBillNo());
        payload.put("patientId", String.valueOf(bill.getPatientId()));
        payload.put("amount", r.getAmount());
        payload.put("nonSelfAmount", r.getNonSelfAmount());
        payload.put("advanceApplied", advanceApplied);
        payload.put("modeCode", r.getModeCode());
        payload.put("transactionType", r.getTransactionType());
        payload.put("paymentDatetime", r.getPaymentDatetime().toString());
        payload.put("balanceSelf", bill.getBalanceSelf());
        payload.put("balanceNonSelf", bill.getBalanceNonSelf());
        payload.put("settled", bill.getIsSettled());

        outboxService.append(BillingEvents.PAYMENT_RECEIVED, "bill", bill.getBillId(),
                String.valueOf(bill.getBillId()), toJson(payload),
                "payment-received:" + r.getReceiptId());
        log.info("Emitted {} for bill {} receipt {}", BillingEvents.PAYMENT_RECEIVED,
                bill.getBillId(), r.getReceiptNo());
    }

    private long nextId(String seq) {
        Number n = (Number) em.createNativeQuery("SELECT nextval(\'ihms_billing." + seq + "\')")
                .getSingleResult();
        return n.longValue();
    }

    private String nextReceiptNo() {
        // Render the receipt number as RCPT-<seq> (sibling of the OPDBILL/IPDBILL
        // pattern in the rest of the service). Production deployments override
        // this with the unit's receipts.no.template (%s/R/%d/%s convention).
        Number n = (Number) em.createNativeQuery("SELECT nextval(\'ihms_billing.t_receipt_seq\')")
                .getSingleResult();
        return "RCPT-" + n.longValue();
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
    }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    private static Timestamp now() { return Timestamp.from(Instant.now()); }
}
