package com.hims.rmrs.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One double-entry posting line (register WP-R5 t_accounting_entry).
 * Invariant pinned here + in service + DB CHECK: EXACTLY ONE of debit/credit is
 * &gt; 0 and the other side = 0 (ck_t_accounting_entry_one_sided); per collection
 * SUM(debit) = SUM(credit) (enforced in CollectionService inside the same tx).
 */
@Entity
@Table(name = "t_accounting_entry")
public class AccountingEntry extends RmrsAuditableBase {

    @Id
    @Column(name = "entry_id")
    private Long entryId;

    @Column(name = "collection_id", nullable = false)
    private Long collectionId;

    @Column(name = "ledger_id", nullable = false)
    private Long ledgerId;

    @Column(name = "debit", nullable = false, precision = 19, scale = 2)
    private BigDecimal debit = BigDecimal.ZERO;

    @Column(name = "credit", nullable = false, precision = 19, scale = 2)
    private BigDecimal credit = BigDecimal.ZERO;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    /** Exactly one side > 0, other = 0 (mirrors the DB CHECK). */
    public boolean isOneSided() {
        boolean debitSide = debit != null && debit.signum() > 0
                && (credit == null || credit.signum() == 0);
        boolean creditSide = credit != null && credit.signum() > 0
                && (debit == null || debit.signum() == 0);
        return debitSide ^ creditSide;
    }

    public Long getEntryId() { return entryId; }
    public void setEntryId(Long entryId) { this.entryId = entryId; }
    public Long getId() { return entryId; }
    public void setId(Long id) { this.entryId = id; }
    public Long getCollectionId() { return collectionId; }
    public void setCollectionId(Long collectionId) { this.collectionId = collectionId; }
    public Long getLedgerId() { return ledgerId; }
    public void setLedgerId(Long ledgerId) { this.ledgerId = ledgerId; }
    public BigDecimal getDebit() { return debit; }
    public void setDebit(BigDecimal debit) { this.debit = debit; }
    public BigDecimal getCredit() { return credit; }
    public void setCredit(BigDecimal credit) { this.credit = credit; }
    public LocalDate getEntryDate() { return entryDate; }
    public void setEntryDate(LocalDate entryDate) { this.entryDate = entryDate; }
}
