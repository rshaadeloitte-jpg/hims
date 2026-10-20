package com.hims.interop.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * EHR job read-model row (register WP-T1). PHASE-1 NOTE: no write path exists
 * yet — job creation rides the OUT-OF-SCOPE dispatcher slice (comment-only,
 * disclosed in the WP report); GET /api/v1/health/jobs?status= exposes the
 * read model so ops can observe jobs once dispatchers land.
 * Status SMALLINT 0..3: 0=QUEUED 1=RUNNING 2=DONE 3=FAILED.
 */
@Entity
@Table(name = "t_ehr_job_details")
public class EhrJobDetails extends InteropBase {

    public static final short STATUS_QUEUED = 0;
    public static final short STATUS_RUNNING = 1;
    public static final short STATUS_DONE = 2;
    public static final short STATUS_FAILED = 3;

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "job_kind", length = 30)
    private String jobKind;

    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "status", nullable = false)
    private Short status = STATUS_QUEUED;

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public String getJobKind() { return jobKind; }
    public void setJobKind(String jobKind) { this.jobKind = jobKind; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Short getStatus() { return status; }
    public void setStatus(Short status) { this.status = status; }
}
