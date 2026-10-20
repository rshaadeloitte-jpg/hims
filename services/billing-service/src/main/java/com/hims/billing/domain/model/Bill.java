package com.hims.billing.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Clean target of T_OPDBILL / T_IPDBILL header.
 * BR-BILL-001: created with status=true, isSettled=false, billNo='000000000000000'
 * placeholder, cashCounterId default 1; final billNo OPDBILL-&lt;seq&gt; (no admissionId)
 * or IPDBILL-&lt;seq&gt; assigned at finalize.
 * BR-BILL-006: isSettled &lt;=&gt; balanceSelf &lt;= 0 AND balanceNonSelf &lt;= 0.
 */
@Entity
@Table(name = "t_bill")
public class Bill extends BillingBase {

    public static final String PLACEHOLDER_BILL_NO = "000000000000000";

    @Id
    @Column(name = "bill_id")
    private Long billId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "encounter_id")
    private Long encounterId;

    @Column(name = "admission_id")
    private Long admissionId;

    @Column(name = "cash_counter_id", nullable = false)
    private Long cashCounterId = 1L;

    /** Request header field prescriberDoctorId — doctor-share attribution (BR-BILL-002). */
    @Column(name = "prescriber_doctor_id")
    private Long prescriberDoctorId;

    /** SELF company path (BR-BILL-005 asymmetric split discriminator). */
    @Column(name = "is_self_company", nullable = false)
    private Boolean isSelfCompany = true;

    @Column(name = "bill_no", nullable = false, length = 20, unique = true)
    private String billNo = PLACEHOLDER_BILL_NO;

    @Column(name = "is_interim", nullable = false)
    private Boolean isInterim = true;

    @Column(name = "is_settled", nullable = false)
    private Boolean isSettled = false;

    @Column(name = "is_cancelled", nullable = false)
    private Boolean isCancelled = false;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "concession_amount", precision = 12, scale = 2)
    private BigDecimal concessionAmount = BigDecimal.ZERO;

    @Column(name = "concession_authorised_by")
    private Long concessionAuthorisedBy;

    @Column(name = "self_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal selfAmount = BigDecimal.ZERO;

    @Column(name = "non_self_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal nonSelfAmount = BigDecimal.ZERO;

    @Column(name = "balance_self", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceSelf = BigDecimal.ZERO;

    @Column(name = "balance_non_self", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceNonSelf = BigDecimal.ZERO;

    @Column(name = "change_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal changeAmount = BigDecimal.ZERO;

    @Column(name = "finalized_at")
    private Timestamp finalizedAt;

    /** WP-S1 saga: source t_order.order_id when drafted from E-13 OrderCreated (NULL otherwise). */
    @Column(name = "saga_source_order_id")
    private Long sagaSourceOrderId;

    /** WP-S1 saga: E-13 envelope eventId that produced this DRAFT (partial UNIQUE backstop). */
    @Column(name = "saga_event_id", length = 64)
    private String sagaEventId;

    /** M04 sponsor change stub: nullable sponsor company/plan (NULL = self-pay). */
    @Column(name = "sponsor_company_id")
    private Long sponsorCompanyId;

    @Column(name = "sponsor_plan_id")
    private Long sponsorPlanId;

    /** Not persisted: lines loaded via repository in the service layer. */
    @Transient
    private List<Charge> charges = new ArrayList<>();

    public Long getBillId() { return billId; }
    public void setBillId(Long billId) { this.billId = billId; }
    public Long getId() { return billId; }
    public void setId(Long id) { this.billId = id; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getEncounterId() { return encounterId; }
    public void setEncounterId(Long encounterId) { this.encounterId = encounterId; }
    public Long getAdmissionId() { return admissionId; }
    public void setAdmissionId(Long admissionId) { this.admissionId = admissionId; }
    public Long getCashCounterId() { return cashCounterId; }
    public void setCashCounterId(Long cashCounterId) { this.cashCounterId = cashCounterId; }
    public Long getPrescriberDoctorId() { return prescriberDoctorId; }
    public void setPrescriberDoctorId(Long prescriberDoctorId) { this.prescriberDoctorId = prescriberDoctorId; }
    public Boolean getIsSelfCompany() { return isSelfCompany; }
    public void setIsSelfCompany(Boolean v) { this.isSelfCompany = v; }
    public String getBillNo() { return billNo; }
    public void setBillNo(String billNo) { this.billNo = billNo; }
    public Boolean getIsInterim() { return isInterim; }
    public void setIsInterim(Boolean v) { this.isInterim = v; }
    public Boolean getIsSettled() { return isSettled; }
    public void setIsSettled(Boolean v) { this.isSettled = v; }
    public Boolean getIsCancelled() { return isCancelled; }
    public void setIsCancelled(Boolean v) { this.isCancelled = v; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal v) { this.totalAmount = v; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal v) { this.taxAmount = v; }
    public BigDecimal getConcessionAmount() { return concessionAmount; }
    public void setConcessionAmount(BigDecimal v) { this.concessionAmount = v; }
    public Long getConcessionAuthorisedBy() { return concessionAuthorisedBy; }
    public void setConcessionAuthorisedBy(Long v) { this.concessionAuthorisedBy = v; }
    public BigDecimal getSelfAmount() { return selfAmount; }
    public void setSelfAmount(BigDecimal v) { this.selfAmount = v; }
    public BigDecimal getNonSelfAmount() { return nonSelfAmount; }
    public void setNonSelfAmount(BigDecimal v) { this.nonSelfAmount = v; }
    public BigDecimal getBalanceSelf() { return balanceSelf; }
    public void setBalanceSelf(BigDecimal v) { this.balanceSelf = v; }
    public BigDecimal getBalanceNonSelf() { return balanceNonSelf; }
    public void setBalanceNonSelf(BigDecimal v) { this.balanceNonSelf = v; }
    public BigDecimal getChangeAmount() { return changeAmount; }
    public void setChangeAmount(BigDecimal v) { this.changeAmount = v; }
    public Timestamp getFinalizedAt() { return finalizedAt; }
    public void setFinalizedAt(Timestamp finalizedAt) { this.finalizedAt = finalizedAt; }
    public Long getSagaSourceOrderId() { return sagaSourceOrderId; }
    public void setSagaSourceOrderId(Long v) { this.sagaSourceOrderId = v; }
    public String getSagaEventId() { return sagaEventId; }
    public void setSagaEventId(String v) { this.sagaEventId = v; }
    public Long getSponsorCompanyId() { return sponsorCompanyId; }
    public void setSponsorCompanyId(Long v) { this.sponsorCompanyId = v; }
    public Long getSponsorPlanId() { return sponsorPlanId; }
    public void setSponsorPlanId(Long v) { this.sponsorPlanId = v; }
    public List<Charge> getCharges() { return charges; }
    public void setCharges(List<Charge> charges) { this.charges = charges; }

    public boolean isFinalized() { return !Boolean.TRUE.equals(isInterim); }
}
