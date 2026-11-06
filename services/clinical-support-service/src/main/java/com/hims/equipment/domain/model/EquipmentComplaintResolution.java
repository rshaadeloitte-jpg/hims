package com.hims.equipment.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

/**
 * Complaint resolution record (equipment_complaint_resolution) — clean target
 * of legacy t_eqpcomplaintresolution. {@code resolutionNotes} maps legacy
 * REMARKS (TEXT); {@code partsReplaced} is the free-text summary while
 * structured part lines live in equipment_complaint_resolution_details.
 * {@code resolvedStatus} holds the complaint_status_master id the complaint
 * moves to when this resolution is applied (1..4). Server-owned id from
 * ihms_equipment.equipment_complaint_resolution_seq (IMP-DEC-003).
 */
@Entity
@Table(name = "equipment_complaint_resolution")
public class EquipmentComplaintResolution extends EquipmentBase {

    @Id
    private Long id;

    @Column(name = "complaint_id", nullable = false)
    private Long complaintId;

    /** Optional human reference (legacy RESOLVENO); no generator in this slice. */
    @Column(name = "resolve_no")
    private String resolveNo;

    @Column(name = "resolution_date")
    private Timestamp resolutionDate;

    @Column(name = "resolved_by")
    private Long resolvedBy;

    /** TEXT column — resolution narrative (legacy REMARKS). */
    @Column(name = "resolution_notes")
    private String resolutionNotes;

    /** TEXT column — free-text replaced-parts summary (structured lines in
     *  equipment_complaint_resolution_details). */
    @Column(name = "parts_replaced")
    private String partsReplaced;

    /** service_agent_master id. */
    @Column(name = "service_agent_id")
    private Long serviceAgentId;

    @Column(name = "assigned_to")
    private Long assignedTo;

    @Column(name = "assigned_by_id")
    private Long assignedById;

    /** complaint_status_master id the complaint moves to (CHECK 1..4). */
    @Column(name = "resolved_status")
    private Long resolvedStatus;

    @Override
    public Long getId() { return id; }
    @Override
    public void setId(Long id) { this.id = id; }
    public Long getComplaintId() { return complaintId; }
    public void setComplaintId(Long complaintId) { this.complaintId = complaintId; }
    public String getResolveNo() { return resolveNo; }
    public void setResolveNo(String resolveNo) { this.resolveNo = resolveNo; }
    public Timestamp getResolutionDate() { return resolutionDate; }
    public void setResolutionDate(Timestamp resolutionDate) { this.resolutionDate = resolutionDate; }
    public Long getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(Long resolvedBy) { this.resolvedBy = resolvedBy; }
    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }
    public String getPartsReplaced() { return partsReplaced; }
    public void setPartsReplaced(String partsReplaced) { this.partsReplaced = partsReplaced; }
    public Long getServiceAgentId() { return serviceAgentId; }
    public void setServiceAgentId(Long serviceAgentId) { this.serviceAgentId = serviceAgentId; }
    public Long getAssignedTo() { return assignedTo; }
    public void setAssignedTo(Long assignedTo) { this.assignedTo = assignedTo; }
    public Long getAssignedById() { return assignedById; }
    public void setAssignedById(Long assignedById) { this.assignedById = assignedById; }
    public Long getResolvedStatus() { return resolvedStatus; }
    public void setResolvedStatus(Long resolvedStatus) { this.resolvedStatus = resolvedStatus; }
}
