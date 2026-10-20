package com.hims.billing.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Clean target of T_DOCTORINCENTIVE (M06 Doctor Share).
 * Ported from legacy EjsIncentives model.
 * Stores doctor incentive amounts (fixed additions/deductions).
 */
@Entity
@Table(name = "t_doctor_incentive")
public class DoctorIncentive extends BillingBase {

    @Id
    @Column(name = "incentive_id")
    private Long incentiveId;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @Column(name = "incentive_type", nullable = false, length = 20)
    private String incentiveType;  // ADDITION or DEDUCTION

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "remark", length = 500)
    private String remark;

    @Column(name = "is_billed", nullable = false)
    private Boolean isBilled = false;

    @Column(name = "incentive_date")
    private java.sql.Timestamp incentiveDate;

    public Long getIncentiveId() { return incentiveId; }
    public void setIncentiveId(Long incentiveId) { this.incentiveId = incentiveId; }
    public Long getId() { return incentiveId; }
    public void setId(Long id) { this.incentiveId = id; }
    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }
    public String getIncentiveType() { return incentiveType; }
    public void setIncentiveType(String incentiveType) { this.incentiveType = incentiveType; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Boolean getIsBilled() { return isBilled; }
    public void setIsBilled(Boolean isBilled) { this.isBilled = isBilled; }
    public java.sql.Timestamp getIncentiveDate() { return incentiveDate; }
    public void setIncentiveDate(java.sql.Timestamp incentiveDate) { this.incentiveDate = incentiveDate; }
}
