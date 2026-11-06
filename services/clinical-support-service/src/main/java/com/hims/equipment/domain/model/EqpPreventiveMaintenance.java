package com.hims.equipment.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Completed maintenance record (eqp_preventive_maintenance) — clean target of
 * legacy T_EqpPreventiveMaintenance (PM / corrective / calibration execution).
 *
 * <p>Carries the legacy checklist-result block as TEXT columns:
 * {@code functionalCheckRemark}, {@code preventiveMaintenanceRemark},
 * {@code electricalSafetyRemark}, {@code calibration}, plus the generic spec
 * {@code remarks} alongside legacy {@code furtherActions}/{@code commentsOnUse}.
 * {@code pmScheduleId} is nullable: legacy had no schedule FK (only
 * PMSCHEDULEDETAILID), so ad-hoc completions remain legal while history can
 * join to the plan. {@code pmStatus} is the execution lifecycle, ids ARE the
 * codes 1=SCHEDULED 2=IN_PROGRESS 3=COMPLETED 4=CANCELLED (CHECK-enforced,
 * same shape as complaint_status in V2). Server-owned id from
 * ihms_equipment.eqp_preventive_maintenance_seq.
 */
@Entity
@Table(name = "eqp_preventive_maintenance")
public class EqpPreventiveMaintenance extends EquipmentBase {

    @Id
    private Long id;

    @Column(name = "equipment_id", nullable = false)
    private Long equipmentId;

    /** Owning plan when the execution belongs to a schedule (nullable — ad-hoc allowed). */
    @Column(name = "pm_schedule_id")
    private Long pmScheduleId;

    /** Planned occurrence this execution fulfils (legacy PMSCHEDULEDETAILID). */
    @Column(name = "pm_schedule_detail_id")
    private Long pmScheduleDetailId;

    @Column(name = "item_id")
    private Long itemId;

    /** Human-facing execution reference (e.g. PM-2026-00001). */
    @Column(name = "maintenance_no")
    private String maintenanceNo;

    @Column(name = "maintenance_date")
    private Timestamp maintenanceDate;

    /** Engineer who performed the service (legacy BIOMEDICALENGINEERID). */
    @Column(name = "performed_by")
    private Long performedBy;

    /** Legacy FUNCTIONALCHECKREMARK (TEXT). */
    @Column(name = "functional_check_remark")
    private String functionalCheckRemark;

    /** Legacy PREVENTIVEMAINTENANCEREMARK (TEXT). */
    @Column(name = "preventive_maintenance_remark")
    private String preventiveMaintenanceRemark;

    /** Legacy ELECTRICALSAFETYREMARK (TEXT). */
    @Column(name = "electrical_safety_remark")
    private String electricalSafetyRemark;

    /** Legacy CALIBRATION note (TEXT). */
    @Column(name = "calibration")
    private String calibration;

    /** Legacy FURTHERACTIONS (TEXT). */
    @Column(name = "further_actions")
    private String furtherActions;

    /** Legacy COMMENTSONUSE (TEXT). */
    @Column(name = "comments_on_use")
    private String commentsOnUse;

    /** Spec Task 9 generic notes column (legacy REMARKS). */
    @Column(name = "remarks")
    private String remarks;

    @Column(name = "dept_in_charge")
    private Long deptInCharge;

    @Column(name = "biomedical_engineer_id")
    private Long biomedicalEngineerId;

    @Column(name = "biomedical_in_charge_id")
    private Long biomedicalInChargeId;

    @Column(name = "verified_by")
    private Long verifiedBy;

    /** NUMERIC(19,2) service cost. */
    @Column(name = "amount_incurred", precision = 19, scale = 2)
    private BigDecimal amountIncurred;

    /** AMC/service agent (legacy typed BigDecimal — corrected to BIGINT id). */
    @Column(name = "service_agent_id")
    private Long serviceAgentId;

    /** Execution lifecycle: 1=SCHEDULED 2=IN_PROGRESS 3=COMPLETED 4=CANCELLED. */
    @Column(name = "pm_status", nullable = false)
    private Long pmStatus = 1L;

    @Override
    public Long getId() { return id; }
    @Override
    public void setId(Long id) { this.id = id; }
    public Long getEquipmentId() { return equipmentId; }
    public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }
    public Long getPmScheduleId() { return pmScheduleId; }
    public void setPmScheduleId(Long pmScheduleId) { this.pmScheduleId = pmScheduleId; }
    public Long getPmScheduleDetailId() { return pmScheduleDetailId; }
    public void setPmScheduleDetailId(Long pmScheduleDetailId) { this.pmScheduleDetailId = pmScheduleDetailId; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public String getMaintenanceNo() { return maintenanceNo; }
    public void setMaintenanceNo(String maintenanceNo) { this.maintenanceNo = maintenanceNo; }
    public Timestamp getMaintenanceDate() { return maintenanceDate; }
    public void setMaintenanceDate(Timestamp maintenanceDate) { this.maintenanceDate = maintenanceDate; }
    public Long getPerformedBy() { return performedBy; }
    public void setPerformedBy(Long performedBy) { this.performedBy = performedBy; }
    public String getFunctionalCheckRemark() { return functionalCheckRemark; }
    public void setFunctionalCheckRemark(String functionalCheckRemark) { this.functionalCheckRemark = functionalCheckRemark; }
    public String getPreventiveMaintenanceRemark() { return preventiveMaintenanceRemark; }
    public void setPreventiveMaintenanceRemark(String preventiveMaintenanceRemark) { this.preventiveMaintenanceRemark = preventiveMaintenanceRemark; }
    public String getElectricalSafetyRemark() { return electricalSafetyRemark; }
    public void setElectricalSafetyRemark(String electricalSafetyRemark) { this.electricalSafetyRemark = electricalSafetyRemark; }
    public String getCalibration() { return calibration; }
    public void setCalibration(String calibration) { this.calibration = calibration; }
    public String getFurtherActions() { return furtherActions; }
    public void setFurtherActions(String furtherActions) { this.furtherActions = furtherActions; }
    public String getCommentsOnUse() { return commentsOnUse; }
    public void setCommentsOnUse(String commentsOnUse) { this.commentsOnUse = commentsOnUse; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public Long getDeptInCharge() { return deptInCharge; }
    public void setDeptInCharge(Long deptInCharge) { this.deptInCharge = deptInCharge; }
    public Long getBiomedicalEngineerId() { return biomedicalEngineerId; }
    public void setBiomedicalEngineerId(Long biomedicalEngineerId) { this.biomedicalEngineerId = biomedicalEngineerId; }
    public Long getBiomedicalInChargeId() { return biomedicalInChargeId; }
    public void setBiomedicalInChargeId(Long biomedicalInChargeId) { this.biomedicalInChargeId = biomedicalInChargeId; }
    public Long getVerifiedBy() { return verifiedBy; }
    public void setVerifiedBy(Long verifiedBy) { this.verifiedBy = verifiedBy; }
    public BigDecimal getAmountIncurred() { return amountIncurred; }
    public void setAmountIncurred(BigDecimal amountIncurred) { this.amountIncurred = amountIncurred; }
    public Long getServiceAgentId() { return serviceAgentId; }
    public void setServiceAgentId(Long serviceAgentId) { this.serviceAgentId = serviceAgentId; }
    public Long getPmStatus() { return pmStatus; }
    public void setPmStatus(Long pmStatus) { this.pmStatus = pmStatus; }
}
