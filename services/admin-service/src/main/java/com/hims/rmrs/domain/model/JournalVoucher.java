package com.hims.rmrs.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

@Entity
@Table(name = "t_journal_voucher")
public class JournalVoucher extends RmrsAuditableBase {
    public static final short STATUS_DRAFT = 1;
    public static final short STATUS_POSTED = 2;
    public static final short STATUS_REVERSED = 3;

    @Id
    @Column(name = "id") private Long id;
    @Override public Long getId() { return id; }
    @Override public void setId(Long v) { this.id = v; }
    @Column(name = "voucher_id", nullable = false) private Long voucherId;
    public Long getVoucherId() { return voucherId; } public void setVoucherId(Long v) { this.voucherId = v; }
    @Column(name = "voucher_no", nullable = false, length = 64, unique = true) private String voucherNo;
    public String getVoucherNo() { return voucherNo; } public void setVoucherNo(String v) { this.voucherNo = v; }
    @Column(name = "narration", length = 500) private String narration;
    public String getNarration() { return narration; } public void setNarration(String v) { this.narration = v; }
    @Column(name = "voucher_date", nullable = false) private Date voucherDate;
    public Date getVoucherDate() { return voucherDate; } public void setVoucherDate(Date v) { this.voucherDate = v; }
    @Column(name = "doc_status", nullable = false) private Short docStatus = STATUS_DRAFT;
    public Short getDocStatus() { return docStatus; } public void setDocStatus(Short v) { this.docStatus = v; }
    @Column(name = "posted_by") private Long postedBy;
    public Long getPostedBy() { return postedBy; } public void setPostedBy(Long v) { this.postedBy = v; }
    @Column(name = "posted_datetime") private Timestamp postedDatetime;
    public Timestamp getPostedDatetime() { return postedDatetime; } public void setPostedDatetime(Timestamp v) { this.postedDatetime = v; }
    @Column(name = "reversed_by") private Long reversedBy;
    public Long getReversedBy() { return reversedBy; } public void setReversedBy(Long v) { this.reversedBy = v; }
    @Column(name = "reversed_datetime") private Timestamp reversedDatetime;
    public Timestamp getReversedDatetime() { return reversedDatetime; } public void setReversedDatetime(Timestamp v) { this.reversedDatetime = v; }
}
