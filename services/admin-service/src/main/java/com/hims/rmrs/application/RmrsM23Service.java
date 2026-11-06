package com.hims.rmrs.application;

import com.hims.rmrs.application.exceptions.InvalidRmrsRequestException;
import com.hims.rmrs.application.exceptions.RmrsNotFoundException;
import com.hims.rmrs.application.exceptions.DuplicateRmrsResourceException;
import com.hims.rmrs.domain.model.BankStatement;
import com.hims.rmrs.domain.model.JournalVoucher;
import com.hims.rmrs.domain.model.JournalVoucherLeg;
import com.hims.rmrs.domain.model.LedgerMaster;
import com.hims.rmrs.domain.model.PoRequest;
import com.hims.rmrs.domain.repository.BankStatementRepository;
import com.hims.rmrs.domain.repository.HeadMasterRepository;
import com.hims.rmrs.domain.repository.JournalVoucherLegRepository;
import com.hims.rmrs.domain.repository.JournalVoucherRepository;
import com.hims.rmrs.domain.repository.LedgerMasterRepository;
import com.hims.rmrs.domain.repository.PoRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/** M23 RMRS: journal vouchers, suspense ledger, bank statement reconciliation, PO request. */
@Service
public class RmrsM23Service {
    private static final long ACTOR = 1L;

    private final JournalVoucherRepository voucherRepository;
    private final JournalVoucherLegRepository legRepository;
    private final LedgerMasterRepository ledgerRepository;
    private final HeadMasterRepository headRepository;
    private final BankStatementRepository bankStatementRepository;
    private final PoRequestRepository poRequestRepository;

    public RmrsM23Service(JournalVoucherRepository voucherRepository,
                          JournalVoucherLegRepository legRepository,
                          LedgerMasterRepository ledgerRepository,
                          HeadMasterRepository headRepository,
                          BankStatementRepository bankStatementRepository,
                          PoRequestRepository poRequestRepository) {
        this.voucherRepository = voucherRepository;
        this.legRepository = legRepository;
        this.ledgerRepository = ledgerRepository;
        this.headRepository = headRepository;
        this.bankStatementRepository = bankStatementRepository;
        this.poRequestRepository = poRequestRepository;
    }

    public record Leg(Long ledgerId, BigDecimal debit, BigDecimal credit) {}

    // ============================================================ journal voucher

    @Transactional
    public JournalVoucher createVoucher(String narration, String voucherDate, List<Leg> legs) {
        if (legs == null || legs.size() < 2) {
            throw new InvalidRmrsRequestException("A journal voucher needs at least 2 legs");
        }
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        for (Leg leg : legs) {
            if (leg.ledgerId() == null || !ledgerRepository.existsById(leg.ledgerId())) {
                throw new RmrsNotFoundException("Ledger not found: " + leg.ledgerId());
            }
            BigDecimal d = leg.debit() == null ? BigDecimal.ZERO : leg.debit();
            BigDecimal c = leg.credit() == null ? BigDecimal.ZERO : leg.credit();
            if (d.signum() < 0 || c.signum() < 0) {
                throw new InvalidRmrsRequestException("Leg amounts must be non-negative");
            }
            if (d.signum() > 0 && c.signum() > 0) {
                throw new InvalidRmrsRequestException("A leg cannot have both debit and credit");
            }
            if (d.signum() == 0 && c.signum() == 0) {
                throw new InvalidRmrsRequestException("A leg must have a debit or credit amount");
            }
            totalDebit = totalDebit.add(d);
            totalCredit = totalCredit.add(c);
        }
        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new DuplicateRmrsResourceException("JV_BALANCE",
                    "Journal voucher unbalanced: debit=" + totalDebit + " credit=" + totalCredit);
        }
        long id = voucherRepository.nextId();
        JournalVoucher voucher = new JournalVoucher();
        voucher.setId(id);
        voucher.setVoucherId(id);
        voucher.setVoucherNo("JV-" + id);
        voucher.setNarration(narration);
        voucher.setVoucherDate(Date.valueOf(voucherDate));
        voucher.setDocStatus(JournalVoucher.STATUS_DRAFT);
        stamp(voucher);
        voucherRepository.save(voucher);
        for (Leg leg : legs) {
            JournalVoucherLeg row = new JournalVoucherLeg();
            row.setId(legRepository.nextId());
            row.setVoucherId(id);
            row.setLedgerId(leg.ledgerId());
            row.setDebit(leg.debit() == null ? BigDecimal.ZERO : leg.debit());
            row.setCredit(leg.credit() == null ? BigDecimal.ZERO : leg.credit());
            stamp(row);
            legRepository.save(row);
        }
        return voucher;
    }

    @Transactional
    public JournalVoucher postVoucher(long id, Long actorId) {
        JournalVoucher v = getVoucher(id);
        if (v.getDocStatus() != JournalVoucher.STATUS_DRAFT) {
            throw new DuplicateRmrsResourceException("JV_STATE", "Only DRAFT vouchers can be posted");
        }
        v.setDocStatus(JournalVoucher.STATUS_POSTED);
        v.setPostedBy(actorId == null ? ACTOR : actorId);
        v.setPostedDatetime(Timestamp.from(Instant.now()));
        stamp(v);
        return voucherRepository.save(v);
    }

    @Transactional
    public JournalVoucher reverseVoucher(long id, Long actorId) {
        JournalVoucher v = getVoucher(id);
        if (v.getDocStatus() != JournalVoucher.STATUS_POSTED) {
            throw new DuplicateRmrsResourceException("JV_STATE", "Only POSTED vouchers can be reversed");
        }
        v.setDocStatus(JournalVoucher.STATUS_REVERSED);
        v.setReversedBy(actorId == null ? ACTOR : actorId);
        v.setReversedDatetime(Timestamp.from(Instant.now()));
        stamp(v);
        return voucherRepository.save(v);
    }

    @Transactional(readOnly = true)
    public JournalVoucher getVoucher(long id) {
        return voucherRepository.findById(id)
                .orElseThrow(() -> new RmrsNotFoundException("Voucher not found"));
    }
    @Transactional(readOnly = true) public List<JournalVoucher> listVouchers(Short status) {
        if (status != null) return voucherRepository.findByDocStatusOrderByAddedDatetimeDesc(status);
        return voucherRepository.findAll();
    }
    @Transactional(readOnly = true) public List<JournalVoucherLeg> voucherLegs(long id) {
        return legRepository.findByVoucherId(id);
    }

    // ============================================================ suspense ledger

    @Transactional
    public LedgerMaster markSuspense(long ledgerId, String reason) {
        LedgerMaster l = ledgerRepository.findById(ledgerId)
                .orElseThrow(() -> new RmrsNotFoundException("Ledger not found"));
        l.setSuspense(true);
        l.setSuspenseReason(reason);
        stamp(l);
        return ledgerRepository.save(l);
    }

    @Transactional
    public LedgerMaster clearSuspense(long ledgerId) {
        LedgerMaster l = ledgerRepository.findById(ledgerId)
                .orElseThrow(() -> new RmrsNotFoundException("Ledger not found"));
        l.setSuspense(false);
        l.setSuspenseReason(null);
        stamp(l);
        return ledgerRepository.save(l);
    }

    // ============================================================ bank statement

    @Transactional
    public BankStatement importBankStatement(long bankLedgerId, String txnDate,
                                              BigDecimal amount, String direction,
                                              String description, String externalRef) {
        if (bankLedgerId <= 0 || !ledgerRepository.existsById(bankLedgerId)) {
            throw new RmrsNotFoundException("Bank ledger not found");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidRmrsRequestException("amount must be > 0");
        }
        if (!"DEBIT".equals(direction) && !"CREDIT".equals(direction)) {
            throw new InvalidRmrsRequestException("direction must be DEBIT or CREDIT");
        }
        long id = bankStatementRepository.nextId();
        BankStatement s = new BankStatement();
        s.setId(id);
        s.setStatementLineId(id);
        s.setBankLedgerId(bankLedgerId);
        s.setTxnDate(Date.valueOf(txnDate));
        s.setAmount(amount);
        s.setDirection(direction);
        s.setDescription(description);
        s.setExternalRef(externalRef);
        s.setDocStatus(BankStatement.STATUS_IMPORTED);
        stamp(s);
        return bankStatementRepository.save(s);
    }

    @Transactional
    public BankStatement reconcile(long statementLineId, Long matchedEntryId, Long matchedCollectionId) {
        BankStatement s = bankStatementRepository.findById(statementLineId)
                .orElseThrow(() -> new RmrsNotFoundException("Statement line not found"));
        if (matchedEntryId == null && matchedCollectionId == null) {
            throw new InvalidRmrsRequestException("Provide matchedEntryId or matchedCollectionId");
        }
        s.setMatchedEntryId(matchedEntryId);
        s.setMatchedCollectionId(matchedCollectionId);
        s.setDocStatus(BankStatement.STATUS_RECONCILED);
        stamp(s);
        return bankStatementRepository.save(s);
    }

    @Transactional
    public BankStatement dispute(long statementLineId, String reason) {
        BankStatement s = bankStatementRepository.findById(statementLineId)
                .orElseThrow(() -> new RmrsNotFoundException("Statement line not found"));
        s.setDocStatus(BankStatement.STATUS_DISPUTED);
        s.setDescription((s.getDescription() == null ? "" : s.getDescription() + " | ") + "DISPUTED: " + reason);
        stamp(s);
        return bankStatementRepository.save(s);
    }

    @Transactional(readOnly = true) public List<BankStatement> listBankStatements(Short status) {
        if (status != null) return bankStatementRepository.findByDocStatusOrderByTxnDateAsc(status);
        return bankStatementRepository.findAll();
    }

    // ============================================================ PO request

    @Transactional
    public PoRequest createPoRequest(Long requestingDeptId, Long headId, Long ledgerId,
                                       BigDecimal amount, String remarks) {
        if (headId == null || !headRepository.existsById(headId)) {
            throw new RmrsNotFoundException( "Head not found");
        }
        if (ledgerId != null && !ledgerRepository.existsById(ledgerId)) {
            throw new RmrsNotFoundException("Ledger not found");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidRmrsRequestException("amount must be > 0");
        }
        long id = poRequestRepository.nextId();
        PoRequest p = new PoRequest();
        p.setId(id);
        p.setRequestId(id);
        p.setRequestNo("POQ-" + id);
        p.setRequestingDeptId(requestingDeptId);
        p.setHeadId(headId);
        p.setLedgerId(ledgerId);
        p.setAmount(amount);
        p.setRemarks(remarks);
        p.setDocStatus(PoRequest.STATUS_NEW);
        stamp(p);
        return poRequestRepository.save(p);
    }

    @Transactional
    public PoRequest approvePoRequest(long id, Long actorId) {
        PoRequest p = getPoRequest(id);
        if (p.getDocStatus() != PoRequest.STATUS_NEW) {
            throw new DuplicateRmrsResourceException("POQ_STATE", "Only NEW PO requests can be approved");
        }
        p.setDocStatus(PoRequest.STATUS_APPROVED);
        p.setApprovedBy(actorId == null ? ACTOR : actorId);
        p.setApprovedDatetime(Timestamp.from(Instant.now()));
        stamp(p);
        return poRequestRepository.save(p);
    }

    @Transactional
    public PoRequest markIssued(long id, Long issuedPoId) {
        PoRequest p = getPoRequest(id);
        if (p.getDocStatus() != PoRequest.STATUS_APPROVED) {
            throw new DuplicateRmrsResourceException("POQ_STATE", "Only APPROVED PO requests can be marked issued");
        }
        p.setDocStatus(PoRequest.STATUS_ISSUED);
        p.setIssuedPoId(issuedPoId);
        stamp(p);
        return poRequestRepository.save(p);
    }

    @Transactional
    public PoRequest rejectPoRequest(long id) {
        PoRequest p = getPoRequest(id);
        if (p.getDocStatus() != PoRequest.STATUS_NEW) {
            throw new DuplicateRmrsResourceException("POQ_STATE", "Only NEW PO requests can be rejected");
        }
        p.setDocStatus(PoRequest.STATUS_REJECTED);
        stamp(p);
        return poRequestRepository.save(p);
    }

    @Transactional(readOnly = true) public PoRequest getPoRequest(long id) {
        return poRequestRepository.findById(id)
                .orElseThrow(() -> new RmrsNotFoundException("PO request not found"));
    }
    @Transactional(readOnly = true) public List<PoRequest> listPoRequests(Short status) {
        if (status != null) return poRequestRepository.findByDocStatusOrderByAddedDatetimeDesc(status);
        return poRequestRepository.findAll();
    }

    // ============================================================ helpers

    private void stamp(com.hims.rmrs.domain.model.RmrsAuditableBase entity) {
        entity.setAddedBy(ACTOR);
        entity.setAddedDatetime(Timestamp.from(Instant.now()));
        entity.setUpdatedBy(ACTOR);
        entity.setUpdatedDatetime(Timestamp.from(Instant.now()));
        entity.setSynchronizedd(false);
    }
}
