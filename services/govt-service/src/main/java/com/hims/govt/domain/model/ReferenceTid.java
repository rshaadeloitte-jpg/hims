package com.hims.govt.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Scheme-side TID linked to a Chiranjeevi enrolment (register WP-T4).
 * tid UNIQUE NOT NULL (dup -> 409 GOVT_DUPLICATE_TID).
 */
@Entity
@Table(name = "t_reference_tid")
public class ReferenceTid extends GovtBase {

    @Id
    @Column(name = "tid_id")
    private Long tidId;

    @Column(name = "enrolment_id", nullable = false)
    private Long enrolmentId;

    @Column(name = "tid", nullable = false, length = 60)
    private String tid;

    public Long getTidId() { return tidId; }
    public void setTidId(Long tidId) { this.tidId = tidId; }
    @Override public Long getId() { return tidId; }
    @Override public void setId(Long id) { this.tidId = id; }
    public Long getEnrolmentId() { return enrolmentId; }
    public void setEnrolmentId(Long enrolmentId) { this.enrolmentId = enrolmentId; }
    public String getTid() { return tid; }
    public void setTid(String tid) { this.tid = tid; }
}
