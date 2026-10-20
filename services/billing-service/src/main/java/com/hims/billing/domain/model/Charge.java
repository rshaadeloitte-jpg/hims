package com.hims.billing.domain.model;

import com.hims.billing.application.BillingCalculator.ChargeCalculation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Clean target of the legacy T_BILLDETAIL-style service-order charge line.
 * Created TEMP (billId null) via POST /api/v1/charges; attached to a bill by
 * POST /api/v1/bills. Snapshot *BeforeFinalization columns are filled at finalize
 * (BR-BILL-008 restore contract; cancellation itself stays monolith-side this tranche).
 *
 * specialization 1=patho 2=rad is NOTED ONLY — BR-BILL-003 order auto-creation is
 * post-saga and deliberately NOT implemented (register WP-11).
 */
@Entity
@Table(name = "t_charge")
public class Charge extends BillingBase {

    @Id
    @Column(name = "charge_id")
    private Long chargeId;

    @Column(name = "bill_id")
    private Long billId;

    @Column(name = "service_id", nullable = false)
    private Long serviceId;

    @Column(name = "service_name", nullable = false, length = 200)
    private String serviceName;

    @Column(name = "rate", nullable = false, precision = 19, scale = 2)
    private BigDecimal rate;

    @Column(name = "qty", nullable = false)
    private Integer qty = 1;

    @Column(name = "tax_pct_a", precision = 5, scale = 2)
    private BigDecimal taxPctA = BigDecimal.ZERO;

    @Column(name = "tax_pct_b", precision = 5, scale = 2)
    private BigDecimal taxPctB = BigDecimal.ZERO;

    @Column(name = "tax_pct_c", precision = 5, scale = 2)
    private BigDecimal taxPctC = BigDecimal.ZERO;

    @Column(name = "apply_after_concession", nullable = false)
    private Boolean applyAfterConcession = false;

    /** 0 => treated as 100% self (deductable rule, BR-BILL-005). */
    @Column(name = "co_pay_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal coPayPct = BigDecimal.ZERO;

    @Column(name = "concession_plan_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal concessionPlanPct = BigDecimal.ZERO;

    @Column(name = "concession_bill_pct", precision = 5, scale = 2)
    private BigDecimal concessionBillPct;

    @Column(name = "flat_concession_amount", precision = 12, scale = 2)
    private BigDecimal flatConcessionAmount;

    @Column(name = "doctor_share_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal doctorSharePct = BigDecimal.ZERO;

    /** 1=patho 2=rad — post-saga order creation marker ONLY (BR-BILL-003 deferred). */
    @Column(name = "specialization")
    private Short specialization;

    @Column(name = "is_ot_procedure", nullable = false)
    private Boolean isOtProcedure = false;

    @Column(name = "total_before_finalization", precision = 19, scale = 2)
    private BigDecimal totalBeforeFinalization;

    @Column(name = "self_before_finalization", precision = 19, scale = 2)
    private BigDecimal selfBeforeFinalization;

    @Column(name = "nonself_before_finalization", precision = 19, scale = 2)
    private BigDecimal nonselfBeforeFinalization;

    @Column(name = "is_cancelled", nullable = false)
    private Boolean isCancelled = false;

    /** Not persisted: computed at finalize for the API response / event payload. */
    @Transient
    private List<ChargeCalculation> calculations = new ArrayList<>();

    public Long getChargeId() { return chargeId; }
    public void setChargeId(Long chargeId) { this.chargeId = chargeId; }
    public Long getId() { return chargeId; }
    public void setId(Long id) { this.chargeId = id; }
    public Long getBillId() { return billId; }
    public void setBillId(Long billId) { this.billId = billId; }
    public Long getServiceId() { return serviceId; }
    public void setServiceId(Long serviceId) { this.serviceId = serviceId; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }
    public Integer getQty() { return qty; }
    public void setQty(Integer qty) { this.qty = qty; }
    public BigDecimal getTaxPctA() { return taxPctA; }
    public void setTaxPctA(BigDecimal v) { this.taxPctA = v; }
    public BigDecimal getTaxPctB() { return taxPctB; }
    public void setTaxPctB(BigDecimal v) { this.taxPctB = v; }
    public BigDecimal getTaxPctC() { return taxPctC; }
    public void setTaxPctC(BigDecimal v) { this.taxPctC = v; }
    public Boolean getApplyAfterConcession() { return applyAfterConcession; }
    public void setApplyAfterConcession(Boolean v) { this.applyAfterConcession = v; }
    public BigDecimal getCoPayPct() { return coPayPct; }
    public void setCoPayPct(BigDecimal v) { this.coPayPct = v; }
    public BigDecimal getConcessionPlanPct() { return concessionPlanPct; }
    public void setConcessionPlanPct(BigDecimal v) { this.concessionPlanPct = v; }
    public BigDecimal getConcessionBillPct() { return concessionBillPct; }
    public void setConcessionBillPct(BigDecimal v) { this.concessionBillPct = v; }
    public BigDecimal getFlatConcessionAmount() { return flatConcessionAmount; }
    public void setFlatConcessionAmount(BigDecimal v) { this.flatConcessionAmount = v; }
    public BigDecimal getDoctorSharePct() { return doctorSharePct; }
    public void setDoctorSharePct(BigDecimal v) { this.doctorSharePct = v; }
    public Short getSpecialization() { return specialization; }
    public void setSpecialization(Short specialization) { this.specialization = specialization; }
    public Boolean getIsOtProcedure() { return isOtProcedure; }
    public void setIsOtProcedure(Boolean v) { this.isOtProcedure = v; }
    public BigDecimal getTotalBeforeFinalization() { return totalBeforeFinalization; }
    public void setTotalBeforeFinalization(BigDecimal v) { this.totalBeforeFinalization = v; }
    public BigDecimal getSelfBeforeFinalization() { return selfBeforeFinalization; }
    public void setSelfBeforeFinalization(BigDecimal v) { this.selfBeforeFinalization = v; }
    public BigDecimal getNonselfBeforeFinalization() { return nonselfBeforeFinalization; }
    public void setNonselfBeforeFinalization(BigDecimal v) { this.nonselfBeforeFinalization = v; }
    public Boolean getIsCancelled() { return isCancelled; }
    public void setIsCancelled(Boolean v) { this.isCancelled = v; }
    public List<ChargeCalculation> getCalculations() { return calculations; }
    public void setCalculations(List<ChargeCalculation> c) { this.calculations = c; }
}
