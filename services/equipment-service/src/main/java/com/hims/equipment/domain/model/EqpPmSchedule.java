package com.hims.equipment.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

/**
 * Preventive-maintenance schedule header (eqp_pm_schedule) — clean target of
 * legacy T_EqpPMSchedule. One recurring service plan per asset.
 *
 * <p>{@code frequency} = months between services; {@code jan}..{@code dec} are
 * the legacy month-of-year flags picking which months a service falls in;
 * {@code pmDate} maps legacy PMDATE (plan start). {@code nextDueDate}/
 * {@code lastCompletedDate} are the spec Task 9 additions that make due-listing
 * and completion bookkeeping explicit (legacy derived them on the fly from
 * PMDATE + FREQUENCY + month flags). {@code pmType} holds a
 * maintenance_type_master id (V1 seed, ids ARE the codes: 1=PREVENTIVE
 * 2=CORRECTIVE 3=CALIBRATION), default PREVENTIVE. {@code scheduleCode} is the
 * human-facing reference, unique per partial index uq_eqp_pm_schedule_code.
 * Server-owned id from ihms_equipment.eqp_pm_schedule_seq (IMP-DEC-003).
 */
@Entity
@Table(name = "eqp_pm_schedule")
public class EqpPmSchedule extends EquipmentBase {

    @Id
    private Long id;

    @Column(name = "equipment_id", nullable = false)
    private Long equipmentId;

    /** Human-facing schedule reference; unique when present (partial index). */
    @Column(name = "schedule_code")
    private String scheduleCode;

    /** Months between services (legacy FREQUENCY). */
    @Column(name = "frequency")
    private Long frequency;

    /** Legacy PMDATE — plan start date. */
    @Column(name = "pm_date")
    private Timestamp pmDate;

    /** Legacy month-of-year flags: which months a service falls in. */
    @Column(name = "jan", nullable = false)
    private Boolean jan = false;
    @Column(name = "feb", nullable = false)
    private Boolean feb = false;
    @Column(name = "mar", nullable = false)
    private Boolean mar = false;
    @Column(name = "apr", nullable = false)
    private Boolean apr = false;
    @Column(name = "may", nullable = false)
    private Boolean may = false;
    @Column(name = "jun", nullable = false)
    private Boolean jun = false;
    @Column(name = "jul", nullable = false)
    private Boolean jul = false;
    @Column(name = "aug", nullable = false)
    private Boolean aug = false;
    @Column(name = "sep", nullable = false)
    private Boolean sep = false;
    @Column(name = "oct", nullable = false)
    private Boolean oct = false;
    @Column(name = "nov", nullable = false)
    private Boolean nov = false;
    @Column(name = "dec", nullable = false)
    private Boolean dec = false;

    /** Next service due (spec Task 9 addition for due listing). */
    @Column(name = "next_due_date")
    private Timestamp nextDueDate;

    /** Set when a completion is recorded against this schedule. */
    @Column(name = "last_completed_date")
    private Timestamp lastCompletedDate;

    /** maintenance_type_master id: 1=PREVENTIVE 2=CORRECTIVE 3=CALIBRATION. */
    @Column(name = "pm_type", nullable = false)
    private Long pmType = 1L;

    /** Store snapshot carried alongside the asset's current department (legacy STOREID). */
    @Column(name = "store_id")
    private Long storeId;

    @Override
    public Long getId() { return id; }
    @Override
    public void setId(Long id) { this.id = id; }
    public Long getEquipmentId() { return equipmentId; }
    public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }
    public String getScheduleCode() { return scheduleCode; }
    public void setScheduleCode(String scheduleCode) { this.scheduleCode = scheduleCode; }
    public Long getFrequency() { return frequency; }
    public void setFrequency(Long frequency) { this.frequency = frequency; }
    public Timestamp getPmDate() { return pmDate; }
    public void setPmDate(Timestamp pmDate) { this.pmDate = pmDate; }
    public Boolean getJan() { return jan; }
    public void setJan(Boolean jan) { this.jan = jan; }
    public Boolean getFeb() { return feb; }
    public void setFeb(Boolean feb) { this.feb = feb; }
    public Boolean getMar() { return mar; }
    public void setMar(Boolean mar) { this.mar = mar; }
    public Boolean getApr() { return apr; }
    public void setApr(Boolean apr) { this.apr = apr; }
    public Boolean getMay() { return may; }
    public void setMay(Boolean may) { this.may = may; }
    public Boolean getJun() { return jun; }
    public void setJun(Boolean jun) { this.jun = jun; }
    public Boolean getJul() { return jul; }
    public void setJul(Boolean jul) { this.jul = jul; }
    public Boolean getAug() { return aug; }
    public void setAug(Boolean aug) { this.aug = aug; }
    public Boolean getSep() { return sep; }
    public void setSep(Boolean sep) { this.sep = sep; }
    public Boolean getOct() { return oct; }
    public void setOct(Boolean oct) { this.oct = oct; }
    public Boolean getNov() { return nov; }
    public void setNov(Boolean nov) { this.nov = nov; }
    public Boolean getDec() { return dec; }
    public void setDec(Boolean dec) { this.dec = dec; }
    public Timestamp getNextDueDate() { return nextDueDate; }
    public void setNextDueDate(Timestamp nextDueDate) { this.nextDueDate = nextDueDate; }
    public Timestamp getLastCompletedDate() { return lastCompletedDate; }
    public void setLastCompletedDate(Timestamp lastCompletedDate) { this.lastCompletedDate = lastCompletedDate; }
    public Long getPmType() { return pmType; }
    public void setPmType(Long pmType) { this.pmType = pmType; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
}
