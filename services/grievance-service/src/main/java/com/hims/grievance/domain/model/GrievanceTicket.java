package com.hims.grievance.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.sql.Timestamp;

/**
 * Clean target of T_PATIENTGRIEVANCE — the grievance ticket aggregate.
 * issue_status: 1=NEW 2=ASSIGNED 3=RESOLVED 4=ASSIGN_TO_CREATOR.
 * ticket_close_status: 1=PENDING 2=COMPLETED 3=CLOSED.
 * IT-support long-tail flags (machine/connectivity/etc.) live in attributes_json (D-4).
 */
@Entity
@Table(name = "grievance_ticket")
public class GrievanceTicket extends GrievanceBase {

    @Id
    private Long id;

    @Column(name = "ticket_no")
    private String ticketNo;

    @Column(name = "issue_date")
    private Timestamp issueDate;

    @Column(name = "mrno")
    private String mrno;

    @Column(name = "name")
    private String name;

    @Column(name = "address")
    private String address;

    @Column(name = "mobile_no")
    private Long mobileNo;

    @Column(name = "category_id")
    private Long categoryId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sub_category_ids", nullable = false, columnDefinition = "jsonb")
    private String subCategoryIds = "[]";

    @Column(name = "issue")
    private String issue;

    @Column(name = "issue_status", nullable = false)
    private Short issueStatus = 1;

    @Column(name = "resolved_date")
    private Timestamp resolvedDate;

    @Column(name = "ticket_close_status")
    private Short ticketCloseStatus;

    @Column(name = "feedback_id")
    private Long feedbackId;

    @Column(name = "severity_id", nullable = false)
    private Long severityId = 0L;

    @Column(name = "assigned_user_id", nullable = false)
    private Long assignedUserId = 0L;

    @Column(name = "attached_file_name")
    private String attachedFileName;

    @Column(name = "expected_resolution_date")
    private Timestamp expectedResolutionDate;

    @Column(name = "user_sso_id")
    private String userSsoId;

    @Column(name = "issue_type")
    private String issueType;

    @Column(name = "reopened_datetime")
    private Timestamp reopenedDatetime;

    @Column(name = "reopened_by")
    private Long reopenedBy;

    @Column(name = "is_citizen", nullable = false)
    private Boolean isCitizen = false;

    @Column(name = "hospital_ticket")
    private String hospitalTicket;

    @Column(name = "service_request_type")
    private String serviceRequestType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attributes_json", nullable = false, columnDefinition = "jsonb")
    private String attributesJson = "{}";

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public String getTicketNo() { return ticketNo; }
    public void setTicketNo(String ticketNo) { this.ticketNo = ticketNo; }
    public Timestamp getIssueDate() { return issueDate; }
    public void setIssueDate(Timestamp issueDate) { this.issueDate = issueDate; }
    public String getMrno() { return mrno; }
    public void setMrno(String mrno) { this.mrno = mrno; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Long getMobileNo() { return mobileNo; }
    public void setMobileNo(Long mobileNo) { this.mobileNo = mobileNo; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getSubCategoryIds() { return subCategoryIds; }
    public void setSubCategoryIds(String subCategoryIds) { this.subCategoryIds = subCategoryIds; }
    public String getIssue() { return issue; }
    public void setIssue(String issue) { this.issue = issue; }
    public Short getIssueStatus() { return issueStatus; }
    public void setIssueStatus(Short issueStatus) { this.issueStatus = issueStatus; }
    public Timestamp getResolvedDate() { return resolvedDate; }
    public void setResolvedDate(Timestamp resolvedDate) { this.resolvedDate = resolvedDate; }
    public Short getTicketCloseStatus() { return ticketCloseStatus; }
    public void setTicketCloseStatus(Short ticketCloseStatus) { this.ticketCloseStatus = ticketCloseStatus; }
    public Long getFeedbackId() { return feedbackId; }
    public void setFeedbackId(Long feedbackId) { this.feedbackId = feedbackId; }
    public Long getSeverityId() { return severityId; }
    public void setSeverityId(Long severityId) { this.severityId = severityId; }
    public Long getAssignedUserId() { return assignedUserId; }
    public void setAssignedUserId(Long assignedUserId) { this.assignedUserId = assignedUserId; }
    public String getAttachedFileName() { return attachedFileName; }
    public void setAttachedFileName(String attachedFileName) { this.attachedFileName = attachedFileName; }
    public Timestamp getExpectedResolutionDate() { return expectedResolutionDate; }
    public void setExpectedResolutionDate(Timestamp expectedResolutionDate) { this.expectedResolutionDate = expectedResolutionDate; }
    public String getUserSsoId() { return userSsoId; }
    public void setUserSsoId(String userSsoId) { this.userSsoId = userSsoId; }
    public String getIssueType() { return issueType; }
    public void setIssueType(String issueType) { this.issueType = issueType; }
    public Timestamp getReopenedDatetime() { return reopenedDatetime; }
    public void setReopenedDatetime(Timestamp reopenedDatetime) { this.reopenedDatetime = reopenedDatetime; }
    public Long getReopenedBy() { return reopenedBy; }
    public void setReopenedBy(Long reopenedBy) { this.reopenedBy = reopenedBy; }
    public Boolean getIsCitizen() { return isCitizen; }
    public void setIsCitizen(Boolean isCitizen) { this.isCitizen = isCitizen; }
    public String getHospitalTicket() { return hospitalTicket; }
    public void setHospitalTicket(String hospitalTicket) { this.hospitalTicket = hospitalTicket; }
    public String getServiceRequestType() { return serviceRequestType; }
    public void setServiceRequestType(String serviceRequestType) { this.serviceRequestType = serviceRequestType; }
    public String getAttributesJson() { return attributesJson; }
    public void setAttributesJson(String attributesJson) { this.attributesJson = attributesJson; }
}
