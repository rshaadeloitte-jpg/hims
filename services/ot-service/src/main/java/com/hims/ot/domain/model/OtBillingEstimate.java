package com.hims.ot.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "t_ot_billing_estimate")
public class OtBillingEstimate extends OtBase {

    @Id
    @Column(name = "estimate_id")
    private Long estimateId;

    @Column(name = "schedule_id")
    private Long scheduleId;

    @Column(name = "procedure_code", length = 50)
    private String procedureCode;

    @Column(name = "total_estimated", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalEstimated = BigDecimal.ZERO;

    @Column(name = "surgeon_fee", precision = 12, scale = 2)
    private BigDecimal surgeonFee = BigDecimal.ZERO;

    @Column(name = "anesthetist_fee", precision = 12, scale = 2)
    private BigDecimal anesthetistFee = BigDecimal.ZERO;

    @Column(name = "ot_charges", precision = 12, scale = 2)
    private BigDecimal otCharges = BigDecimal.ZERO;

    @Column(name = "implant_cost", precision = 12, scale = 2)
    private BigDecimal implantCost = BigDecimal.ZERO;

    @Column(name = "consumable_cost", precision = 12, scale = 2)
    private BigDecimal consumableCost = BigDecimal.ZERO;

    public Long getId() { return estimateId; }
    public void setId(Long id) { this.estimateId = id; }
    public Long getEstimateId() { return estimateId; }
    public void setEstimateId(Long estimateId) { this.estimateId = estimateId; }
    public Long getScheduleId() { return scheduleId; }
    public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }
    public String getProcedureCode() { return procedureCode; }
    public void setProcedureCode(String procedureCode) { this.procedureCode = procedureCode; }
    public BigDecimal getTotalEstimated() { return totalEstimated; }
    public void setTotalEstimated(BigDecimal totalEstimated) { this.totalEstimated = totalEstimated; }
    public BigDecimal getSurgeonFee() { return surgeonFee; }
    public void setSurgeonFee(BigDecimal surgeonFee) { this.surgeonFee = surgeonFee; }
    public BigDecimal getAnesthetistFee() { return anesthetistFee; }
    public void setAnesthetistFee(BigDecimal anesthetistFee) { this.anesthetistFee = anesthetistFee; }
    public BigDecimal getOtCharges() { return otCharges; }
    public void setOtCharges(BigDecimal otCharges) { this.otCharges = otCharges; }
    public BigDecimal getImplantCost() { return implantCost; }
    public void setImplantCost(BigDecimal implantCost) { this.implantCost = implantCost; }
    public BigDecimal getConsumableCost() { return consumableCost; }
    public void setConsumableCost(BigDecimal consumableCost) { this.consumableCost = consumableCost; }
}
