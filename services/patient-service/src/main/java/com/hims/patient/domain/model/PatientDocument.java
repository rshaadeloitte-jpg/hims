package com.hims.patient.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Document linkage (metadata only — blob storage is MIG-021 MinIO). */
@Entity
@Table(name = "patient_document")
public class PatientDocument extends PatientBase {

    @Id
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "doc_type", nullable = false)
    private String docType;

    @Column(name = "doc_ref", nullable = false)
    private String docRef;

    @Column(name = "verified", nullable = false)
    private Boolean verified = false;

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getDocType() { return docType; }
    public void setDocType(String docType) { this.docType = docType; }
    public String getDocRef() { return docRef; }
    public void setDocRef(String docRef) { this.docRef = docRef; }
    public Boolean getVerified() { return verified; }
    public void setVerified(Boolean verified) { this.verified = verified; }
}
