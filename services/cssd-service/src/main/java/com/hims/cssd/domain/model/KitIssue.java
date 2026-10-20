package com.hims.cssd.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

/**
 * Issue of a STERILE kit to an OT schedule / procedure reference (register WP-R3).
 * issued_to_ref NOT NULL; issuing flips the kit to 4=ISSUED — the single-active-issue
 * guard is structural (no second issue can pass the STERILE-only guard).
 */
@Entity
@Table(name = "t_kit_issue")
public class KitIssue extends CssdBase {

    @Id
    @Column(name = "issue_id")
    private Long issueId;

    @Column(name = "kit_id", nullable = false)
    private Long kitId;

    @Column(name = "issued_to_ref", nullable = false, length = 80)
    private String issuedToRef;

    @Column(name = "issued_at", nullable = false)
    private Timestamp issuedAt;

    public Long getIssueId() { return issueId; }
    public void setIssueId(Long issueId) { this.issueId = issueId; }
    @Override public Long getId() { return issueId; }
    @Override public void setId(Long id) { this.issueId = id; }
    public Long getKitId() { return kitId; }
    public void setKitId(Long kitId) { this.kitId = kitId; }
    public String getIssuedToRef() { return issuedToRef; }
    public void setIssuedToRef(String issuedToRef) { this.issuedToRef = issuedToRef; }
    public Timestamp getIssuedAt() { return issuedAt; }
    public void setIssuedAt(Timestamp issuedAt) { this.issuedAt = issuedAt; }
    @Column(name = "dispatched_to_store_id") private Long dispatchedToStoreId;
    public Long getDispatchedToStoreId() { return dispatchedToStoreId; } public void setDispatchedToStoreId(Long v) { this.dispatchedToStoreId = v; }
    @Column(name = "dispatched_at") private Timestamp dispatchedAt;
    public Timestamp getDispatchedAt() { return dispatchedAt; } public void setDispatchedAt(Timestamp v) { this.dispatchedAt = v; }
    @Column(name = "received_at") private Timestamp receivedAt;
    public Timestamp getReceivedAt() { return receivedAt; } public void setReceivedAt(Timestamp v) { this.receivedAt = v; }
    @Column(name = "received_by", length = 64) private String receivedBy;
    public String getReceivedBy() { return receivedBy; } public void setReceivedBy(String v) { this.receivedBy = v; }
}
