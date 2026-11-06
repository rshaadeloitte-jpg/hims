package com.hims.rmrs.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * One cash collection / receipt (register WP-R5 t_rmrs_collection).
 * amount NUMERIC(19,2) CHECK &gt; 0; receipt_no UNIQUE (409 RMRS_DUPLICATE_RECEIPT).
 * payment_ref carries the E-08 paymentId (string, cross-service id) — consuming
 * PaymentReceived for auto-receipt generation is a Wave-5b read-model concern.
 */
@Entity
@Table(name = "t_rmrs_collection")
public class RmrsCollection extends RmrsAuditableBase {

    @Id
    @Column(name = "collection_id")
    private Long collectionId;

    @Column(name = "receipt_no", nullable = false, length = 24)
    private String receiptNo;

    @Column(name = "collection_date", nullable = false)
    private LocalDate collectionDate;

    @Column(name = "patient_id")
    private Long patientId;

    /** NUMERIC(19,2), CHECK &gt; 0 at DB level too. */
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "head_id", nullable = false)
    private Long headId;

    @Column(name = "ledger_id", nullable = false)
    private Long ledgerId;

    @Column(name = "authority_id")
    private Long authorityId;

    /** E-08 paymentId linkage — string, cross-service id (Wave-5b read-model ready). */
    @Column(name = "payment_ref", length = 40)
    private String paymentRef;

    /** Legacy counter identifier — column kept per register; NOT part of the WP-R5 API contract. */
    @Column(name = "counter", length = 20)
    private String counter;

    @Column(name = "collected_by", length = 80)
    private String collectedBy;

    /** Not persisted: balanced postings are loaded via AccountingEntryRepository in the service layer. */
    @Transient
    private List<AccountingEntry> entries = new ArrayList<>();

    public Long getCollectionId() { return collectionId; }
    public void setCollectionId(Long collectionId) { this.collectionId = collectionId; }
    public Long getId() { return collectionId; }
    public void setId(Long id) { this.collectionId = id; }
    public String getReceiptNo() { return receiptNo; }
    public void setReceiptNo(String receiptNo) { this.receiptNo = receiptNo; }
    public LocalDate getCollectionDate() { return collectionDate; }
    public void setCollectionDate(LocalDate collectionDate) { this.collectionDate = collectionDate; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Long getHeadId() { return headId; }
    public void setHeadId(Long headId) { this.headId = headId; }
    public Long getLedgerId() { return ledgerId; }
    public void setLedgerId(Long ledgerId) { this.ledgerId = ledgerId; }
    public Long getAuthorityId() { return authorityId; }
    public void setAuthorityId(Long authorityId) { this.authorityId = authorityId; }
    public String getPaymentRef() { return paymentRef; }
    public void setPaymentRef(String paymentRef) { this.paymentRef = paymentRef; }
    public String getCounter() { return counter; }
    public void setCounter(String counter) { this.counter = counter; }
    public String getCollectedBy() { return collectedBy; }
    public void setCollectedBy(String collectedBy) { this.collectedBy = collectedBy; }
    public List<AccountingEntry> getEntries() { return entries; }
    public void setEntries(List<AccountingEntry> entries) { this.entries = entries; }
}
