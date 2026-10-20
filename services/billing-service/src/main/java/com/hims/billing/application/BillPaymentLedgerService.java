package com.hims.billing.application;

import com.hims.billing.application.exceptions.BillingEntityNotFoundException;
import com.hims.billing.application.exceptions.InvalidBillingRequestException;
import com.hims.billing.domain.model.Bill;
import com.hims.billing.domain.model.BillPaymentLedger;
import com.hims.billing.domain.repository.BillPaymentLedgerRepository;
import com.hims.billing.domain.repository.BillRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Bill payment ledger (M04 partial-payment). One row per (re)computation event.
 * Authoritative payment state lives in payments-service; this is the
 * billing-UI mirror for partial-payment visualization.
 */
@Service
public class BillPaymentLedgerService {

    public static final long AUDIT_ACTOR_ID = 1L;

    private final BillPaymentLedgerRepository ledgerRepository;
    private final BillRepository billRepository;

    @PersistenceContext
    private EntityManager em;

    public BillPaymentLedgerService(BillPaymentLedgerRepository ledgerRepository,
                                     BillRepository billRepository) {
        this.ledgerRepository = ledgerRepository;
        this.billRepository = billRepository;
    }

    @Transactional
    public BillPaymentLedger record(long billId, String eventType, BigDecimal paidSelf, BigDecimal paidNonSelf,
                                     String sourceRef, String sourceService, String notes) {
        if (!List.of(BillPaymentLedger.EVENT_PAYMENT,
                BillPaymentLedger.EVENT_REFUND,
                BillPaymentLedger.EVENT_ADVANCE_USE,
                BillPaymentLedger.EVENT_CANCEL).contains(eventType)) {
            throw new InvalidBillingRequestException("BEXA003",
                    "eventType must be PAYMENT | REFUND | ADVANCE_USE | CANCEL");
        }
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new BillingEntityNotFoundException("NOT_FOUND",
                        "Bill not found: " + billId));
        BigDecimal balanceSelf = nzOrZero(bill.getBalanceSelf());
        BigDecimal balanceNonSelf = nzOrZero(bill.getBalanceNonSelf());
        boolean settled = balanceSelf.signum() <= 0 && balanceNonSelf.signum() <= 0;

        BillPaymentLedger row = new BillPaymentLedger();
        row.setLedgerId(nextId());
        row.setBillId(billId);
        row.setEventType(eventType);
        row.setPaidSelf(paidSelf);
        row.setPaidNonSelf(paidNonSelf);
        row.setBalanceSelf(balanceSelf);
        row.setBalanceNonSelf(balanceNonSelf);
        row.setIsSettled(settled);
        row.setSourceRef(sourceRef);
        row.setSourceService(sourceService);
        row.setNotes(notes);
        return ledgerRepository.save(row);
    }

    @Transactional(readOnly = true)
    public List<BillPaymentLedger> listForBill(long billId) {
        return ledgerRepository.findByBillIdOrderByAddedDatetimeDesc(billId);
    }

    /**
     * Returns running balance summary (latest ledger row + bill total).
     * Designed for the billing UI's "partial payment status" widget.
     */
    @Transactional(readOnly = true)
    public PaymentStatus paymentStatus(long billId) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new BillingEntityNotFoundException("NOT_FOUND",
                        "Bill not found: " + billId));
        List<BillPaymentLedger> rows = ledgerRepository.findByBillIdOrderByAddedDatetimeDesc(billId);
        BillPaymentLedger latest = rows.isEmpty() ? null : rows.get(0);
        return new PaymentStatus(
                billId,
                bill.getBillNo(),
                bill.getTotalAmount(),
                bill.getSelfAmount().subtract(bill.getBalanceSelf()),
                bill.getNonSelfAmount().subtract(bill.getBalanceNonSelf()),
                bill.getBalanceSelf(),
                bill.getBalanceNonSelf(),
                Boolean.TRUE.equals(bill.getIsSettled()),
                latest != null ? latest.getLedgerId() : null,
                latest != null ? latest.getAddedDatetime() : null,
                rows.size());
    }

    private Long nextId() {
        Number n = (Number) em.createNativeQuery(
                "SELECT nextval('ihms_billing.t_bill_payment_ledger_seq')").getSingleResult();
        return n.longValue();
    }

    private static BigDecimal nzOrZero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    public record PaymentStatus(
            long billId,
            String billNo,
            BigDecimal totalAmount,
            BigDecimal paidSelf,
            BigDecimal paidNonSelf,
            BigDecimal balanceSelf,
            BigDecimal balanceNonSelf,
            boolean settled,
            Long latestLedgerId,
            java.sql.Timestamp latestEventAt,
            int eventCount) {}
}
