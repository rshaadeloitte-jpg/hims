package com.hims.billing.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Clean target of T_DOCTORSHARE (CIMS_ADDDOCTORSHARE, BR-BILL-002).
 * share = totalAmount*pct/100 CEILING scale 2; hospital = total - share HALF_UP scale 2;
 * is_billed=false at insert (DoctorShare accrual consumes E-07 later).
 */
@Entity
@Table(name = "t_doctor_share")
public class DoctorShare extends BillingBase {

    @Id
    @Column(name = "share_id")
    private Long shareId;

    @Column(name = "bill_id", nullable = false)
    private Long billId;

    @Column(name = "charge_id", nullable = false)
    private Long chargeId;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @Column(name = "share_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal shareAmount;

    @Column(name = "hospital_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal hospitalAmount;

    @Column(name = "is_billed", nullable = false)
    private Boolean isBilled = false;

    public Long getShareId() { return shareId; }
    public void setShareId(Long shareId) { this.shareId = shareId; }
    public Long getId() { return shareId; }
    public void setId(Long id) { this.shareId = id; }
    public Long getBillId() { return billId; }
    public void setBillId(Long billId) { this.billId = billId; }
    public Long getChargeId() { return chargeId; }
    public void setChargeId(Long chargeId) { this.chargeId = chargeId; }
    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }
    public BigDecimal getShareAmount() { return shareAmount; }
    public void setShareAmount(BigDecimal v) { this.shareAmount = v; }
    public BigDecimal getHospitalAmount() { return hospitalAmount; }
    public void setHospitalAmount(BigDecimal v) { this.hospitalAmount = v; }
    public Boolean getIsBilled() { return isBilled; }
    public void setIsBilled(Boolean v) { this.isBilled = v; }
}
