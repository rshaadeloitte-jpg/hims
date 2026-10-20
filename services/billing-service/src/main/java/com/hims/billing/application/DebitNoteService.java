package com.hims.billing.application;

import com.hims.billing.application.exceptions.BillingEntityNotFoundException;
import com.hims.billing.application.exceptions.InvalidBillingRequestException;
import com.hims.billing.domain.model.Bill;
import com.hims.billing.domain.model.DebitNote;
import com.hims.billing.domain.repository.BillRepository;
import com.hims.billing.domain.repository.DebitNoteRepository;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Debit note service (M04 gap). Ported from legacy T_DEBITNOTE. Opposite of
 * credit note: a debit note ADDS to a finalized bill's total. Requires
 * approval (approved_by/at) before being applied.
 *
 * BR-BILL-007: A debit note cannot exceed the original bill total * 1.5
 * (safety guard against runaway adjustments; mirrors legacy
 * CIMS_CHECKDEBITNOTE pattern).
 */
@Service
public class DebitNoteService {

    private static final Logger log = LoggerFactory.getLogger(DebitNoteService.class);
    public static final long AUDIT_ACTOR_ID = 1L;
    private static final BigDecimal MAX_RATIO = new BigDecimal("1.5");

    private final DebitNoteRepository repository;
    private final BillRepository billRepository;
    private final OutboxService outboxService;

    @PersistenceContext
    private EntityManager em;

    public DebitNoteService(DebitNoteRepository repository,
                            BillRepository billRepository,
                            OutboxService outboxService) {
        this.repository = repository;
        this.billRepository = billRepository;
        this.outboxService = outboxService;
    }

    @Transactional
    public DebitNote issue(long billId, BigDecimal amount, String reason) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidBillingRequestException("BEXA003", "amount must be > 0");
        }
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new BillingEntityNotFoundException("NOT_FOUND",
                        "Bill not found: " + billId));
        if (!Boolean.TRUE.equals(bill.isFinalized())) {
            throw new InvalidBillingRequestException("BILL_NOT_FINALIZED",
                    "Debit notes can only be issued against finalized bills");
        }
        if (Boolean.TRUE.equals(bill.getIsCancelled())) {
            throw new InvalidBillingRequestException("BILL_CANCELLED",
                    "Cannot issue a debit note on a cancelled bill");
        }
        // BR-BILL-007: cap at 1.5x the original bill total
        BigDecimal cap = bill.getTotalAmount() != null
                ? bill.getTotalAmount().multiply(MAX_RATIO) : MAX_RATIO;
        if (amount.compareTo(cap) > 0) {
            throw new InvalidBillingRequestException("DEBIT_LIMIT",
                    "Debit note amount exceeds the safety cap (1.5x original bill total)");
        }

        DebitNote dn = new DebitNote();
        dn.setDebitNoteId(nextId());
        dn.setBillId(billId);
        dn.setPatientId(bill.getPatientId());
        dn.setDebitNoteNo("DBN" + Instant.now().toEpochMilli() + "-" + dn.getDebitNoteId());
        dn.setReason(reason);
        dn.setAmount(amount);
        dn.setIsApplied(false);
        dn = repository.save(dn);
        log.info("Issued debit note {} on bill {} for amount {}", dn.getDebitNoteNo(), billId, amount);
        return dn;
    }

    @Transactional
    public DebitNote approve(long debitNoteId, Long approvedBy) {
        DebitNote dn = repository.findById(debitNoteId)
                .orElseThrow(() -> new BillingEntityNotFoundException("NOT_FOUND",
                        "Debit note not found: " + debitNoteId));
        if (Boolean.TRUE.equals(dn.getIsApplied())) {
            throw new InvalidBillingRequestException("DEBIT_ALREADY_APPLIED",
                    "Debit note is already applied");
        }
        dn.setApprovedBy(approvedBy != null ? approvedBy : AUDIT_ACTOR_ID);
        dn.setApprovedAt(Timestamp.from(Instant.now()));
        dn.setIsApplied(true);
        DebitNote saved = repository.save(dn);

        // Apply to bill total
        Bill bill = billRepository.findById(saved.getBillId())
                .orElseThrow(() -> new BillingEntityNotFoundException("NOT_FOUND",
                        "Bill not found for debit note: " + saved.getBillId()));
        BigDecimal newTotal = nzOrZero(bill.getTotalAmount()).add(saved.getAmount());
        bill.setTotalAmount(newTotal);
        bill.setBalanceSelf(nzOrZero(bill.getBalanceSelf()).add(dn.getAmount()));
        bill.setUpdatedBy(AUDIT_ACTOR_ID);
        bill.setUpdatedDatetime(Timestamp.from(Instant.now()));
        billRepository.save(bill);

        // Emit DebitNoteApplied
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("debitNoteId", saved.getDebitNoteId());
        payload.put("debitNoteNo", saved.getDebitNoteNo());
        payload.put("billId", saved.getBillId());
        payload.put("patientId", saved.getPatientId());
        payload.put("amount", dn.getAmount());
        payload.put("appliedAt", Instant.now().toString());
        outboxService.appendOutbox(
                "DebitNoteApplied", "v1", "DebitNote", dn.getDebitNoteId(),
                String.valueOf(dn.getBillId()), payload,
                "billing-service", "debitnote-applied.v1");
        return dn;
    }

    @Transactional(readOnly = true)
    public DebitNote get(long debitNoteId) {
        return repository.findById(debitNoteId)
                .orElseThrow(() -> new BillingEntityNotFoundException("NOT_FOUND",
                        "Debit note not found: " + debitNoteId));
    }

    @Transactional(readOnly = true)
    public List<DebitNote> listForBill(long billId) {
        return repository.findByBillId(billId);
    }

    @Transactional(readOnly = true)
    public List<DebitNote> listForPatient(long patientId) {
        return repository.findByPatientIdOrderByAddedDatetimeDesc(patientId);
    }

    private Long nextId() {
        Number n = (Number) em.createNativeQuery(
                "SELECT nextval('ihms_billing.t_debit_note_seq')").getSingleResult();
        return n.longValue();
    }

    private static BigDecimal nzOrZero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
