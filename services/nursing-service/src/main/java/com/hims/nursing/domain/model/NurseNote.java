package com.hims.nursing.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

/**
 * Clean target of T_NURSENOTE — a free-text nursing note attached to an admission.
 *
 * BOUNDARY NOTE (phase 1): {@code admission_id} is accepted as an OPAQUE ID.
 * Cross-service validation against adt-service is deferred to the inter-service phase.
 */
@Entity
@Table(name = "t_nursenote")
public class NurseNote extends NursingBase {

    @Id
    private Long id;

    /** Opaque id in phase 1 (adt-service owns admissions). */
    @Column(name = "admission_id", nullable = false)
    private Long admissionId;

    @Column(name = "note", columnDefinition = "text", nullable = false)
    private String note;

    @Column(name = "nurse_id")
    private Long nurseId;

    @Column(name = "noted_at", nullable = false)
    private Timestamp notedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAdmissionId() { return admissionId; }
    public void setAdmissionId(Long admissionId) { this.admissionId = admissionId; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public Long getNurseId() { return nurseId; }
    public void setNurseId(Long nurseId) { this.nurseId = nurseId; }
    public Timestamp getNotedAt() { return notedAt; }
    public void setNotedAt(Timestamp notedAt) { this.notedAt = notedAt; }
}
