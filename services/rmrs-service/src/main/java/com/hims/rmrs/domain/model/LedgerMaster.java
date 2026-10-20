package com.hims.rmrs.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Ledger master (register WP-R5 t_ledger_master); head_id FK NOT NULL — the head-ledger consistency guard relies on it. */
@Entity
@Table(name = "t_ledger_master")
public class LedgerMaster extends RmrsAuditableBase {
    @Column(name = "suspense", nullable = false) private Boolean suspense = false;
    public Boolean getSuspense() { return suspense; } public void setSuspense(Boolean v) { this.suspense = v; }
    @Column(name = "suspense_reason", length = 255) private String suspenseReason;
    public String getSuspenseReason() { return suspenseReason; } public void setSuspenseReason(String v) { this.suspenseReason = v; }

    @Id
    @Column(name = "ledger_id")
    private Long ledgerId;

    @Column(name = "ledger_code", nullable = false, length = 20)
    private String ledgerCode;

    @Column(name = "ledger_name", length = 120)
    private String ledgerName;

    @Column(name = "head_id", nullable = false)
    private Long headId;

    public Long getLedgerId() { return ledgerId; }
    public void setLedgerId(Long ledgerId) { this.ledgerId = ledgerId; }
    public Long getId() { return ledgerId; }
    public void setId(Long id) { this.ledgerId = id; }
    public String getLedgerCode() { return ledgerCode; }
    public void setLedgerCode(String ledgerCode) { this.ledgerCode = ledgerCode; }
    public String getLedgerName() { return ledgerName; }
    public void setLedgerName(String ledgerName) { this.ledgerName = ledgerName; }
    public Long getHeadId() { return headId; }
    public void setHeadId(Long headId) { this.headId = headId; }
}
