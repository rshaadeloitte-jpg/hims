package com.hims.pharmacy.domain.model;

import jakarta.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "t_home_medication")
public class HomeMedication extends PharmacyBase {

    @Id
    @Column(name = "home_med_id")
    private Long homeMedId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "encounter_id", nullable = false)
    private Long encounterId;

    @Column(name = "admission_id")
    private Long admissionId;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;

    @Column(name = "dosage", length = 100)
    private String dosage;

    @Column(name = "frequency", length = 100)
    private String frequency;

    @Column(name = "duration_days")
    private Integer durationDays;

    @Column(name = "quantity", precision = 10, scale = 2)
    private java.math.BigDecimal quantity;

    @Column(name = "uom", length = 20)
    private String uom;

    @Column(name = "instructions", length = 500)
    private String instructions;

    @Column(name = "prescribed_by")
    private Long prescribedBy;

    @Column(name = "prescribed_date", nullable = false)
    private Timestamp prescribedDate;

    @Column(name = "dispensed", nullable = false)
    private Boolean dispensed = false;

    @Column(name = "dispensed_at")
    private Timestamp dispensedAt;

    public Long getHomeMedId() { return homeMedId; }
    public void setHomeMedId(Long homeMedId) { this.homeMedId = homeMedId; }
    @Override public Long getId() { return homeMedId; }
    @Override public void setId(Long id) { this.homeMedId = id; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getEncounterId() { return encounterId; }
    public void setEncounterId(Long encounterId) { this.encounterId = encounterId; }
    public Long getAdmissionId() { return admissionId; }
    public void setAdmissionId(Long admissionId) { this.admissionId = admissionId; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }
    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }
    public Integer getDurationDays() { return durationDays; }
    public void setDurationDays(Integer durationDays) { this.durationDays = durationDays; }
    public java.math.BigDecimal getQuantity() { return quantity; }
    public void setQuantity(java.math.BigDecimal quantity) { this.quantity = quantity; }
    public String getUom() { return uom; }
    public void setUom(String uom) { this.uom = uom; }
    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    public Long getPrescribedBy() { return prescribedBy; }
    public void setPrescribedBy(Long prescribedBy) { this.prescribedBy = prescribedBy; }
    public Timestamp getPrescribedDate() { return prescribedDate; }
    public void setPrescribedDate(Timestamp prescribedDate) { this.prescribedDate = prescribedDate; }
    public Boolean getDispensed() { return dispensed; }
    public void setDispensed(Boolean dispensed) { this.dispensed = dispensed; }
    public Timestamp getDispensedAt() { return dispensedAt; }
    public void setDispensedAt(Timestamp dispensedAt) { this.dispensedAt = dispensedAt; }
}
