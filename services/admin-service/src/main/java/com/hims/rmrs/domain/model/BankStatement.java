package com.hims.rmrs.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

@Entity
@Table(name = "t_bank_statement")
public class BankStatement extends RmrsAuditableBase {
    public static final short STATUS_IMPORTED = 1;
    public static final short STATUS_RECONCILED = 2;
    public static final short STATUS_DISPUTED = 3;

    @Id
    @Column(name = "id") private Long id;
    @Override public Long getId() { return id; }
    @Override public void setId(Long v) { this.id = v; }
    @Column(name = "statement_line_id", nullable = false) private Long statementLineId;
    public Long getStatementLineId() { return statementLineId; } public void setStatementLineId(Long v) { this.statementLineId = v; }
    @Column(name = "bank_ledger_id", nullable = false) private Long bankLedgerId;
    public Long getBankLedgerId() { return bankLedgerId; } public void setBankLedgerId(Long v) { this.bankLedgerId = v; }
    @Column(name = "txn_date", nullable = false) private Date txnDate;
    public Date getTxnDate() { return txnDate; } public void setTxnDate(Date v) { this.txnDate = v; }
    @Column(name = "amount", nullable = false, precision = 19, scale = 2) private BigDecimal amount;
    public BigDecimal getAmount() { return amount; } public void setAmount(BigDecimal v) { this.amount = v; }
    @Column(name = "direction", nullable = false, length = 6) private String direction;
    public String getDirection() { return direction; } public void setDirection(String v) { this.direction = v; }
    @Column(name = "description", length = 500) private String description;
    public String getDescription() { return description; } public void setDescription(String v) { this.description = v; }
    @Column(name = "external_ref", length = 64) private String externalRef;
    public String getExternalRef() { return externalRef; } public void setExternalRef(String v) { this.externalRef = v; }
    @Column(name = "doc_status", nullable = false) private Short docStatus = STATUS_IMPORTED;
    public Short getDocStatus() { return docStatus; } public void setDocStatus(Short v) { this.docStatus = v; }
    @Column(name = "matched_entry_id") private Long matchedEntryId;
    public Long getMatchedEntryId() { return matchedEntryId; } public void setMatchedEntryId(Long v) { this.matchedEntryId = v; }
    @Column(name = "matched_collection_id") private Long matchedCollectionId;
    public Long getMatchedCollectionId() { return matchedCollectionId; } public void setMatchedCollectionId(Long v) { this.matchedCollectionId = v; }
}
