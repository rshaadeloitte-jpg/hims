package com.hims.rcm.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Admission -> NHCX transaction id (register WP-R4): admission_id NOT NULL UNIQUE
 * (one TID per admission; duplicate assignment rejected with 409 TID_EXISTS),
 * tid VARCHAR(60) NOT NULL.
 */
@Entity
@Table(name = "t_admission_tid")
public class AdmissionTid extends RcmBase {

    @Id
    @Column(name = "tid_id")
    private Long tidId;

    @Column(name = "admission_id", nullable = false, unique = true)
    private Long admissionId;

    @Column(name = "tid", nullable = false, length = 60)
    private String tid;

    public Long getTidId() { return tidId; }
    public void setTidId(Long tidId) { this.tidId = tidId; }
    public Long getAdmissionId() { return admissionId; }
    public void setAdmissionId(Long admissionId) { this.admissionId = admissionId; }
    public String getTid() { return tid; }
    public void setTid(String tid) { this.tid = tid; }
}
