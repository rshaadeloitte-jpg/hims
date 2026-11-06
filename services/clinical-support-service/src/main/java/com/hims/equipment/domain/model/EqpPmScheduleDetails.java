package com.hims.equipment.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

/**
 * Planned occurrence line of a PM schedule (eqp_pm_schedule_details) — clean
 * target of legacy T_EqpPMScheduleDetails. One planned service visit per line:
 * {@code scheduledDate} maps legacy DATE and {@code preventiveMaintenance} maps
 * legacy ISPREVENTIVEMAINTENANCE (schedules may carry corrective visits too).
 * Server-owned id from ihms_equipment.eqp_pm_schedule_details_seq.
 */
@Entity
@Table(name = "eqp_pm_schedule_details")
public class EqpPmScheduleDetails extends EquipmentBase {

    @Id
    private Long id;

    /** Owning schedule header (FK eqp_pm_schedule). */
    @Column(name = "pm_schedule_id", nullable = false)
    private Long pmScheduleId;

    @Column(name = "scheduled_date")
    private Timestamp scheduledDate;

    /** Legacy ISPREVENTIVEMAINTENANCE — false marks a planned corrective visit. */
    @Column(name = "is_preventive_maintenance", nullable = false)
    private Boolean preventiveMaintenance = true;

    @Override
    public Long getId() { return id; }
    @Override
    public void setId(Long id) { this.id = id; }
    public Long getPmScheduleId() { return pmScheduleId; }
    public void setPmScheduleId(Long pmScheduleId) { this.pmScheduleId = pmScheduleId; }
    public Timestamp getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(Timestamp scheduledDate) { this.scheduledDate = scheduledDate; }
    public Boolean getPreventiveMaintenance() { return preventiveMaintenance; }
    public void setPreventiveMaintenance(Boolean preventiveMaintenance) { this.preventiveMaintenance = preventiveMaintenance; }
}
