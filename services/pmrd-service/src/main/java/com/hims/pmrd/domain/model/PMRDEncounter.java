package com.hims.pmrd.domain.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "pmrd_encounter")
public class PMRDEncounter {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "visit_id")
    private Long visitId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "visit_ien")
    private String visitIen;

    @Column(name = "patient_ien")
    private String patientIen;

    @Column(name = "unit_id")
    private Long unitId;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "type")
    private String type; // OP / IP

    @PrePersist
    protected void onCreate() {}

    public PMRDEncounter() {}

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getVisitId() { return visitId; }
    public void setVisitId(Long visitId) { this.visitId = visitId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getVisitIen() { return visitIen; }
    public void setVisitIen(String visitIen) { this.visitIen = visitIen; }
    public String getPatientIen() { return patientIen; }
    public void setPatientIen(String patientIen) { this.patientIen = patientIen; }
    public Long getUnitId() { return unitId; }
    public void setUnitId(Long unitId) { this.unitId = unitId; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
