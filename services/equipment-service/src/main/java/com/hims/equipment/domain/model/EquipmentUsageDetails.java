package com.hims.equipment.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Equipment usage log row (equipment_usage_details, V1) — one entry per usage
 * session of an asset (legacy T_EQUIPMENTUSAGEDETAILS): when it ran, for how
 * long and by whom.
 *
 * <p>{@code hoursUsed} maps the NUMERIC(8,2) column (BigDecimal, 2 dp);
 * {@code remarks} maps the TEXT column; {@code operator} is a free name from
 * the legacy VARCHAR(255) (no FK to a staff master in the clean target).
 * Server-owned id from ihms_equipment.equipment_usage_details_seq
 * (IMP-DEC-003). Deliberately NO Java initializers — every business column is
 * optional at the column level and the service owns validation/defaulting, so
 * null-based defaulting (e.g. usageDate -> now on log) stays reachable
 * (note #17 gotcha: an initializer makes the key-absent case indistinguishable).
 */
@Entity
@Table(name = "equipment_usage_details")
public class EquipmentUsageDetails extends EquipmentBase {

    @Id
    private Long id;

    @Column(name = "equipment_id", nullable = false)
    private Long equipmentId;

    /** When the usage session happened — defaults to now when logged without one. */
    @Column(name = "usage_date")
    private Timestamp usageDate;

    /** NUMERIC(8,2) — machine-hours for this session. */
    @Column(name = "hours_used", precision = 8, scale = 2)
    private BigDecimal hoursUsed;

    @Column(name = "operator")
    private String operator;

    /** TEXT column — free-text session remarks. */
    @Column(name = "remarks")
    private String remarks;

    @Override
    public Long getId() { return id; }
    @Override
    public void setId(Long id) { this.id = id; }
    public Long getEquipmentId() { return equipmentId; }
    public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }
    public Timestamp getUsageDate() { return usageDate; }
    public void setUsageDate(Timestamp usageDate) { this.usageDate = usageDate; }
    public BigDecimal getHoursUsed() { return hoursUsed; }
    public void setHoursUsed(BigDecimal hoursUsed) { this.hoursUsed = hoursUsed; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
