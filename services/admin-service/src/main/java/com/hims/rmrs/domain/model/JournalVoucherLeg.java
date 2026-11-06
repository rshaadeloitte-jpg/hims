package com.hims.rmrs.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

@Entity
@Table(name = "t_journal_voucher_leg")
public class JournalVoucherLeg extends RmrsAuditableBase {

    @Id
    @Column(name = "id") private Long id;
    @Override public Long getId() { return id; }
    @Override public void setId(Long v) { this.id = v; }
    @Column(name = "voucher_id", nullable = false) private Long voucherId;
    public Long getVoucherId() { return voucherId; } public void setVoucherId(Long v) { this.voucherId = v; }
    @Column(name = "ledger_id", nullable = false) private Long ledgerId;
    public Long getLedgerId() { return ledgerId; } public void setLedgerId(Long v) { this.ledgerId = v; }
    @Column(name = "debit", nullable = false, precision = 19, scale = 2) private BigDecimal debit = BigDecimal.ZERO;
    public BigDecimal getDebit() { return debit; } public void setDebit(BigDecimal v) { this.debit = v; }
    @Column(name = "credit", nullable = false, precision = 19, scale = 2) private BigDecimal credit = BigDecimal.ZERO;
    public BigDecimal getCredit() { return credit; } public void setCredit(BigDecimal v) { this.credit = v; }
}
