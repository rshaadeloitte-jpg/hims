package com.hims.interop.domain.model;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table(name = "t_abha_link")
public class AbhaLink extends InteropBase {
    public static final short STATUS_PENDING_VERIFY = 1;
    public static final short STATUS_VERIFIED = 2;
    public static final short STATUS_DELINKED = 3;
    public static final short STATUS_REVOKED = 4;
    @Id @Column(name = "id") private Long id;
    @Override public Long getId() { return id; }
    @Override public void setId(Long v) { this.id = v; }
    @Column(name = "abha_address", nullable = false, length = 64) private String abhaAddress;
    public String getAbhaAddress() { return abhaAddress; } public void setAbhaAddress(String v) { this.abhaAddress = v; }
    @Column(name = "health_id_number", length = 64) private String healthIdNumber;
    public String getHealthIdNumber() { return healthIdNumber; } public void setHealthIdNumber(String v) { this.healthIdNumber = v; }
    @Column(name = "patient_id") private Long patientId;
    public Long getPatientId() { return patientId; } public void setPatientId(Long v) { this.patientId = v; }
    @Column(name = "txn_id", nullable = false, length = 64, unique = true) private String txnId;
    public String getTxnId() { return txnId; } public void setTxnId(String v) { this.txnId = v; }
    @Column(name = "link_status", nullable = false) private Short linkStatus = STATUS_PENDING_VERIFY;
    public Short getLinkStatus() { return linkStatus; } public void setLinkStatus(Short v) { this.linkStatus = v; }
    @Column(name = "verified_at") private Timestamp verifiedAt;
    public Timestamp getVerifiedAt() { return verifiedAt; } public void setVerifiedAt(Timestamp v) { this.verifiedAt = v; }
    @Column(name = "delinked_at") private Timestamp delinkedAt;
    public Timestamp getDelinkedAt() { return delinkedAt; } public void setDelinkedAt(Timestamp v) { this.delinkedAt = v; }
}
