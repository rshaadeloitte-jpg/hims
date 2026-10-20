package com.hims.pharmacy.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Pharmacist clinical intervention record (Catalog #9 / register WP-R2):
 * intervention_type, notes, pharmacist_id. patient_id is required because the
 * register API is GET /api/v1/interventions?patientId=; sale_id is optional
 * (interventions can be recorded against any sale).
 */
@Entity
@Table(name = "t_drug_intervention")
public class DrugIntervention extends PharmacyBase {

    @Id
    @Column(name = "intervention_id")
    private Long interventionId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "sale_id")
    private Long saleId;

    @Column(name = "intervention_type", nullable = false, length = 50)
    private String interventionType;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "pharmacist_id", nullable = false)
    private Long pharmacistId;

    @Override public Long getId() { return interventionId; }
    @Override public void setId(Long id) { this.interventionId = id; }
    public Long getInterventionId() { return interventionId; }
    public void setInterventionId(Long interventionId) { this.interventionId = interventionId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getSaleId() { return saleId; }
    public void setSaleId(Long saleId) { this.saleId = saleId; }
    public String getInterventionType() { return interventionType; }
    public void setInterventionType(String interventionType) { this.interventionType = interventionType; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Long getPharmacistId() { return pharmacistId; }
    public void setPharmacistId(Long pharmacistId) { this.pharmacistId = pharmacistId; }
}
