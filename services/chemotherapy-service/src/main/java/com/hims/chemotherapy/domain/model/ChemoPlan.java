package com.hims.chemotherapy.domain.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "chemo_plan")
public class ChemoPlan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "plan_name")
    private String planName;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "status")
    private String status;

    public ChemoPlan() {}
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getPatientId() { return patientId; } public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getPlanName() { return planName; } public void setPlanName(String planName) { this.planName = planName; }
    public LocalDate getStartDate() { return startDate; } public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public String getStatus() { return status; } public void setStatus(String status) { this.status = status; }
}
