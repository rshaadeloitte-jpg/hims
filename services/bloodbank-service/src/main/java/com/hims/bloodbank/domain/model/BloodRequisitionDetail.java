package com.hims.bloodbank.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Requisition line (component-level mirror of the BR-BLD-001 header status).
 * Legacy keeps details/header asymmetry (RISK R7): expiry/cancel writes
 * requisition_status=10 + reason at DETAIL level; this service mirrors both.
 */
@Entity
@Table(name = "t_bloodrequisitiondetail")
public class BloodRequisitionDetail extends BloodBankBase {

    @Id
    @Column(name = "detail_id")
    private Long detailId;

    @Column(name = "requisition_id", nullable = false)
    private Long requisitionId;

    @Column(name = "component", nullable = false)
    private String component;

    @Column(name = "volume_ml", nullable = false)
    private Integer volumeMl;

    /** Mirrors t_bloodrequisition.status per BR-BLD-001. */
    @Column(name = "requisition_status", nullable = false)
    private Integer requisitionStatus = BloodRequisition.STATUS_NEW;

    /** Set by BR-BLD-004 expiry scan / cancel. */
    @Column(name = "isorderexpired", nullable = false)
    private Boolean isorderexpired = false;

    /** Legacy literal for BR-BLD-004: 'Order is expired'. */
    @Column(name = "expiry_reason")
    private String expiryReason;

    public Long getDetailId() { return detailId; }
    public void setDetailId(Long detailId) { this.detailId = detailId; }
    public Long getId() { return detailId; }
    public void setId(Long id) { this.detailId = id; }
    public Long getRequisitionId() { return requisitionId; }
    public void setRequisitionId(Long requisitionId) { this.requisitionId = requisitionId; }
    public String getComponent() { return component; }
    public void setComponent(String component) { this.component = component; }
    public Integer getVolumeMl() { return volumeMl; }
    public void setVolumeMl(Integer volumeMl) { this.volumeMl = volumeMl; }
    public Integer getRequisitionStatus() { return requisitionStatus; }
    public void setRequisitionStatus(Integer requisitionStatus) { this.requisitionStatus = requisitionStatus; }
    public Boolean getIsorderexpired() { return isorderexpired; }
    public void setIsorderexpired(Boolean isorderexpired) { this.isorderexpired = isorderexpired; }
    public String getExpiryReason() { return expiryReason; }
    public void setExpiryReason(String expiryReason) { this.expiryReason = expiryReason; }
}
