package com.hims.interop.domain.model;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table(name = "t_abha_consent")
public class AbhaConsent extends InteropBase {
    public static final short STATUS_GRANTED = 1;
    public static final short STATUS_REVOKED = 2;
    public static final short STATUS_EXPIRED = 3;
    @Id @Column(name = "id") private Long id;
    @Override public Long getId() { return id; }
    @Override public void setId(Long v) { this.id = v; }
    @Column(name = "abha_link_id", nullable = false) private Long abhaLinkId;
    public Long getAbhaLinkId() { return abhaLinkId; } public void setAbhaLinkId(Long v) { this.abhaLinkId = v; }
    @Column(name = "consent_request_id", nullable = false, length = 64, unique = true) private String consentRequestId;
    public String getConsentRequestId() { return consentRequestId; } public void setConsentRequestId(String v) { this.consentRequestId = v; }
    @Column(name = "purpose", nullable = false, length = 64) private String purpose;
    public String getPurpose() { return purpose; } public void setPurpose(String v) { this.purpose = v; }
    @Column(name = "hip_id", nullable = false, length = 64) private String hipId;
    public String getHipId() { return hipId; } public void setHipId(String v) { this.hipId = v; }
    @Column(name = "hiu_id", nullable = false, length = 64) private String hiuId;
    public String getHiuId() { return hiuId; } public void setHiuId(String v) { this.hiuId = v; }
    @Column(name = "valid_from", nullable = false) private Timestamp validFrom;
    public Timestamp getValidFrom() { return validFrom; } public void setValidFrom(Timestamp v) { this.validFrom = v; }
    @Column(name = "valid_to", nullable = false) private Timestamp validTo;
    public Timestamp getValidTo() { return validTo; } public void setValidTo(Timestamp v) { this.validTo = v; }
    @Column(name = "consent_status", nullable = false) private Short consentStatus = STATUS_GRANTED;
    public Short getConsentStatus() { return consentStatus; } public void setConsentStatus(Short v) { this.consentStatus = v; }
    @Column(name = "granted_at") private Timestamp grantedAt;
    public Timestamp getGrantedAt() { return grantedAt; } public void setGrantedAt(Timestamp v) { this.grantedAt = v; }
    @Column(name = "revoked_at") private Timestamp revokedAt;
    public Timestamp getRevokedAt() { return revokedAt; } public void setRevokedAt(Timestamp v) { this.revokedAt = v; }
}
