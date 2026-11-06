package com.hims.equipment.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

/**
 * Complaint line item (equipment_complaint_details) — clean target of legacy
 * T_eqpComplaintDetails. Per-line asset snapshot (item/serial/model) plus the
 * reporting/priority context captured when the complaint was logged.
 * {@code eqpMachineStatus} holds an equipment_working_status_master id at
 * report time. Server-owned id from
 * ihms_equipment.equipment_complaint_details_seq (IMP-DEC-003).
 */
@Entity
@Table(name = "equipment_complaint_details")
public class EquipmentComplaintDetails extends EquipmentBase {

    @Id
    private Long id;

    @Column(name = "complaint_id", nullable = false)
    private Long complaintId;

    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "serial_no")
    private String serialNo;

    @Column(name = "model_no")
    private String modelNo;

    @Column(name = "problem_date")
    private Timestamp problemDate;

    @Column(name = "problem_time")
    private Timestamp problemTime;

    @Column(name = "reported_date")
    private Timestamp reportedDate;

    @Column(name = "reported_time")
    private Timestamp reportedTime;

    @Column(name = "reported_by")
    private Long reportedBy;

    /** service_agent_master id. */
    @Column(name = "service_agent_id")
    private Long serviceAgentId;

    @Column(name = "priority_id")
    private Long priorityId;

    @Column(name = "expected_resolution_date")
    private Timestamp expectedResolutionDate;

    @Column(name = "expected_resolution_time")
    private Timestamp expectedResolutionTime;

    /** equipment_working_status_master id at report time. */
    @Column(name = "eqp_machine_status")
    private Long eqpMachineStatus;

    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "warranty_type_id")
    private Long warrantyTypeId;

    /** TEXT column — per-line description (legacy COMPLAINTDETAILS). */
    @Column(name = "description")
    private String description;

    @Override
    public Long getId() { return id; }
    @Override
    public void setId(Long id) { this.id = id; }
    public Long getComplaintId() { return complaintId; }
    public void setComplaintId(Long complaintId) { this.complaintId = complaintId; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public String getSerialNo() { return serialNo; }
    public void setSerialNo(String serialNo) { this.serialNo = serialNo; }
    public String getModelNo() { return modelNo; }
    public void setModelNo(String modelNo) { this.modelNo = modelNo; }
    public Timestamp getProblemDate() { return problemDate; }
    public void setProblemDate(Timestamp problemDate) { this.problemDate = problemDate; }
    public Timestamp getProblemTime() { return problemTime; }
    public void setProblemTime(Timestamp problemTime) { this.problemTime = problemTime; }
    public Timestamp getReportedDate() { return reportedDate; }
    public void setReportedDate(Timestamp reportedDate) { this.reportedDate = reportedDate; }
    public Timestamp getReportedTime() { return reportedTime; }
    public void setReportedTime(Timestamp reportedTime) { this.reportedTime = reportedTime; }
    public Long getReportedBy() { return reportedBy; }
    public void setReportedBy(Long reportedBy) { this.reportedBy = reportedBy; }
    public Long getServiceAgentId() { return serviceAgentId; }
    public void setServiceAgentId(Long serviceAgentId) { this.serviceAgentId = serviceAgentId; }
    public Long getPriorityId() { return priorityId; }
    public void setPriorityId(Long priorityId) { this.priorityId = priorityId; }
    public Timestamp getExpectedResolutionDate() { return expectedResolutionDate; }
    public void setExpectedResolutionDate(Timestamp expectedResolutionDate) { this.expectedResolutionDate = expectedResolutionDate; }
    public Timestamp getExpectedResolutionTime() { return expectedResolutionTime; }
    public void setExpectedResolutionTime(Timestamp expectedResolutionTime) { this.expectedResolutionTime = expectedResolutionTime; }
    public Long getEqpMachineStatus() { return eqpMachineStatus; }
    public void setEqpMachineStatus(Long eqpMachineStatus) { this.eqpMachineStatus = eqpMachineStatus; }
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public Long getWarrantyTypeId() { return warrantyTypeId; }
    public void setWarrantyTypeId(Long warrantyTypeId) { this.warrantyTypeId = warrantyTypeId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
