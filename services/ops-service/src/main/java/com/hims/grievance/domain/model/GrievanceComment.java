package com.hims.grievance.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Clean target of T_PATIENTGRIEVANCECOMMENTS (child of ticket). */
@Entity
@Table(name = "grievance_comment")
public class GrievanceComment extends GrievanceBase {

    @Id
    private Long id;

    @Column(name = "grievance_id", nullable = false)
    private Long grievanceId;

    @Column(name = "comment")
    private String comment;

    @Column(name = "user_name")
    private String userName;

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public Long getGrievanceId() { return grievanceId; }
    public void setGrievanceId(Long grievanceId) { this.grievanceId = grievanceId; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
}
