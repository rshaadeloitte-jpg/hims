package com.hims.ot.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Clean target of T_OTSCHEDULEDETAILS: one procedure booked on an OT schedule.
 *
 * BR-OT-001: when {@code dependent_of} is set (references a sibling detail row),
 * the row is a dependent surgery and carries the request-supplied
 * {@code discount_pct}. Procedure {@code rate} defaults to 0 when not supplied
 * (tariff lookup deferred to Reference integration).
 */
@Entity
@Table(name = "t_otscheduledetail")
public class OtScheduleDetail extends OtBase {

    @Id
    @Column(name = "detail_id")
    private Long detailId;

    @Column(name = "schedule_id", nullable = false)
    private Long scheduleId;

    @Column(name = "procedure_code", nullable = false, length = 50)
    private String procedureCode;

    @Column(name = "procedure_name", nullable = false, length = 200)
    private String procedureName;

    /** Defaults 0 when no tariff match (Reference integration deferred). */
    @Column(name = "rate", nullable = false, precision = 12, scale = 2)
    private BigDecimal rate = BigDecimal.ZERO;

    /** Sibling detail id this row depends on (dependent surgery per BR-OT-001). */
    @Column(name = "dependent_of")
    private Long dependentOf;

    /** Applied only for dependent rows (BR-OT-001); 0..100. */
    @Column(name = "discount_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPct = BigDecimal.ZERO;

    /** Shared audit boolean flag (t_otscheduledetail keeps the sibling-convention column). */
    @Column(name = "status", nullable = false)
    private Boolean statusFlag = true;

    public Long getDetailId() { return detailId; }
    public void setDetailId(Long detailId) { this.detailId = detailId; }
    public Long getId() { return detailId; }
    public void setId(Long id) { this.detailId = id; }
    public Long getScheduleId() { return scheduleId; }
    public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }
    public String getProcedureCode() { return procedureCode; }
    public void setProcedureCode(String procedureCode) { this.procedureCode = procedureCode; }
    public String getProcedureName() { return procedureName; }
    public void setProcedureName(String procedureName) { this.procedureName = procedureName; }
    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }
    public Long getDependentOf() { return dependentOf; }
    public void setDependentOf(Long dependentOf) { this.dependentOf = dependentOf; }
    public BigDecimal getDiscountPct() { return discountPct; }
    public void setDiscountPct(BigDecimal discountPct) { this.discountPct = discountPct; }
    public Boolean getStatusFlag() { return statusFlag; }
    public void setStatus(Boolean statusFlag) { this.statusFlag = statusFlag; }

    /** Net chargeable amount after the BR-OT-001 dependent discount. */
    public BigDecimal getNetRate() {
        return OtSchedule.netRate(rate, discountPct);
    }
}
