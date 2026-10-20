package com.hims.equipment.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

/**
 * Equipment complaint header (equipment_complaint) — clean target of legacy
 * T_EqpComplaint. One row per reported equipment problem.
 *
 * <p>{@code complaintNo} is server-generated {@code IHMS/EQ/{yyyy}/{%05d}}
 * (grievance-service ticket-number pattern) from
 * ihms_equipment.equipment_complaint_no_seq, unique per partial index
 * uq_equipment_complaint_no. {@code complaintStatus} holds a
 * complaint_status_master id (V1 seed: 1=OPEN 2=IN_PROGRESS 3=RESOLVED
 * 4=CLOSED — the ids the ComplaintStatus constants, Task 7, wrap), default
 * OPEN. {@code categoryId}/{@code complaintTypeId}/{@code severityId} hold
 * master-table ids; {@code description} maps legacy COMPLAINTDETAILS (TEXT).
 * Server-owned id from ihms_equipment.equipment_complaint_seq (IMP-DEC-003).
 */
@Entity
@Table(name = "equipment_complaint")
public class EquipmentComplaint extends EquipmentBase {

    @Id
    private Long id;

    @Column(name = "equipment_id", nullable = false)
    private Long equipmentId;

    @Column(name = "complaint_no")
    private String complaintNo;

    /** complaint_category_master id. */
    @Column(name = "category_id")
    private Long categoryId;

    /** complaint_type_master id. */
    @Column(name = "complaint_type_id")
    private Long complaintTypeId;

    @Column(name = "severity_id")
    private Long severityId;

    /** TEXT column — free-text problem description (legacy COMPLAINTDETAILS). */
    @Column(name = "description")
    private String description;

    @Column(name = "complaint_date")
    private Timestamp complaintDate;

    @Column(name = "reported_by")
    private Long reportedBy;

    @Column(name = "reported_date")
    private Timestamp reportedDate;

    /** complaint_status_master id: 1=OPEN 2=IN_PROGRESS 3=RESOLVED 4=CLOSED. */
    @Column(name = "complaint_status", nullable = false)
    private Long complaintStatus = 1L;

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "store_id")
    private Long storeId;

    /** Asset accession snapshot at raise time (asset may transfer later). */
    @Column(name = "accession_no")
    private String accessionNo;

    @Column(name = "room_no")
    private String roomNo;

    @Column(name = "extension_no")
    private String extensionNo;

    @Column(name = "assigned_to")
    private Long assignedTo;

    @Column(name = "expected_resolution_date")
    private Timestamp expectedResolutionDate;

    @Column(name = "resolved_date")
    private Timestamp resolvedDate;

    @Column(name = "visit_date")
    private Timestamp visitDate;

    @Override
    public Long getId() { return id; }
    @Override
    public void setId(Long id) { this.id = id; }
    public Long getEquipmentId() { return equipmentId; }
    public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }
    public String getComplaintNo() { return complaintNo; }
    public void setComplaintNo(String complaintNo) { this.complaintNo = complaintNo; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Long getComplaintTypeId() { return complaintTypeId; }
    public void setComplaintTypeId(Long complaintTypeId) { this.complaintTypeId = complaintTypeId; }
    public Long getSeverityId() { return severityId; }
    public void setSeverityId(Long severityId) { this.severityId = severityId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Timestamp getComplaintDate() { return complaintDate; }
    public void setComplaintDate(Timestamp complaintDate) { this.complaintDate = complaintDate; }
    public Long getReportedBy() { return reportedBy; }
    public void setReportedBy(Long reportedBy) { this.reportedBy = reportedBy; }
    public Timestamp getReportedDate() { return reportedDate; }
    public void setReportedDate(Timestamp reportedDate) { this.reportedDate = reportedDate; }
    public Long getComplaintStatus() { return complaintStatus; }
    public void setComplaintStatus(Long complaintStatus) { this.complaintStatus = complaintStatus; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public String getAccessionNo() { return accessionNo; }
    public void setAccessionNo(String accessionNo) { this.accessionNo = accessionNo; }
    public String getRoomNo() { return roomNo; }
    public void setRoomNo(String roomNo) { this.roomNo = roomNo; }
    public String getExtensionNo() { return extensionNo; }
    public void setExtensionNo(String extensionNo) { this.extensionNo = extensionNo; }
    public Long getAssignedTo() { return assignedTo; }
    public void setAssignedTo(Long assignedTo) { this.assignedTo = assignedTo; }
    public Timestamp getExpectedResolutionDate() { return expectedResolutionDate; }
    public void setExpectedResolutionDate(Timestamp expectedResolutionDate) { this.expectedResolutionDate = expectedResolutionDate; }
    public Timestamp getResolvedDate() { return resolvedDate; }
    public void setResolvedDate(Timestamp resolvedDate) { this.resolvedDate = resolvedDate; }
    public Timestamp getVisitDate() { return visitDate; }
    public void setVisitDate(Timestamp visitDate) { this.visitDate = visitDate; }
}
