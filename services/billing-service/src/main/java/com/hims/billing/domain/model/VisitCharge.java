package com.hims.billing.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Clean target of T_DOCTORVISITCHARGES (M06 Doctor Share).
 * Ported from legacy Doctorvisitcharge model.
 * Stores visit-type-based charges for doctors (OPD/IPD visit fees).
 */
@Entity
@Table(name = "t_visit_charge")
public class VisitCharge extends BillingBase {

    @Id
    @Column(name = "visit_charge_id")
    private Long visitChargeId;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @Column(name = "service_id")
    private Long serviceId;

    @Column(name = "tariff_service_id")
    private Long tariffServiceId;

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "ward_id")
    private Long wardId;

    @Column(name = "visit_type")
    private Long visitType;

    @Column(name = "class_id")
    private Long classId;

    @Column(name = "priority_id")
    private Long priorityId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    public Long getVisitChargeId() { return visitChargeId; }
    public void setVisitChargeId(Long visitChargeId) { this.visitChargeId = visitChargeId; }
    public Long getId() { return visitChargeId; }
    public void setId(Long id) { this.visitChargeId = id; }
    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }
    public Long getServiceId() { return serviceId; }
    public void setServiceId(Long serviceId) { this.serviceId = serviceId; }
    public Long getTariffServiceId() { return tariffServiceId; }
    public void setTariffServiceId(Long tariffServiceId) { this.tariffServiceId = tariffServiceId; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public Long getWardId() { return wardId; }
    public void setWardId(Long wardId) { this.wardId = wardId; }
    public Long getVisitType() { return visitType; }
    public void setVisitType(Long visitType) { this.visitType = visitType; }
    public Long getClassId() { return classId; }
    public void setClassId(Long classId) { this.classId = classId; }
    public Long getPriorityId() { return priorityId; }
    public void setPriorityId(Long priorityId) { this.priorityId = priorityId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
