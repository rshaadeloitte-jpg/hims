package com.hims.chemotherapy.domain.model;

import jakarta.persistence.*;

import java.time.LocalDate;

/**
 * Chemotherapy regimen header (M29) — legacy IHMS {@code t_chemogeneral}/{@code t_chemoplan}
 * row: one prescribed protocol for one patient, tracked cycle by cycle.
 *
 * BR-M29-001: cycle progression — {@code currentCycle} starts at 1 and advances by one
 *   per completed cycle; the regimen auto-completes when {@code currentCycle >= totalCycles}.
 * BR-M29-002: {@code nextDueDate} is derived from {@code cycleIntervalDays} on each advance.
 * BR-M29-003: status machine PLANNED -> ACTIVE -> COMPLETED / CANCELLED / ON_HOLD.
 */
@Entity
@Table(name = "chemo_regimen")
public class ChemoRegimen {

    public static final String STATUS_PLANNED = "PLANNED";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_ON_HOLD = "ON_HOLD";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "patient_name")
    private String patientName;

    @Column(name = "regimen_name", nullable = false)
    private String regimenName;

    @Column(name = "protocol_code")
    private String protocolCode;

    @Column(name = "diagnosis")
    private String diagnosis;

    @Column(name = "current_cycle", nullable = false)
    private Integer currentCycle = 1;

    @Column(name = "total_cycles", nullable = false)
    private Integer totalCycles = 1;

    @Column(name = "cycle_interval_days")
    private Integer cycleIntervalDays;

    @Column(name = "status", nullable = false)
    private String status = STATUS_PLANNED;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "next_due_date")
    private LocalDate nextDueDate;

    @Column(name = "prescribed_by")
    private String prescribedBy;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "unit_id")
    private Long unitId;

    public ChemoRegimen() {}

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getPatientId() { return patientId; } public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getPatientName() { return patientName; } public void setPatientName(String patientName) { this.patientName = patientName; }
    public String getRegimenName() { return regimenName; } public void setRegimenName(String regimenName) { this.regimenName = regimenName; }
    public String getProtocolCode() { return protocolCode; } public void setProtocolCode(String protocolCode) { this.protocolCode = protocolCode; }
    public String getDiagnosis() { return diagnosis; } public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
    public Integer getCurrentCycle() { return currentCycle; } public void setCurrentCycle(Integer currentCycle) { this.currentCycle = currentCycle; }
    public Integer getTotalCycles() { return totalCycles; } public void setTotalCycles(Integer totalCycles) { this.totalCycles = totalCycles; }
    public Integer getCycleIntervalDays() { return cycleIntervalDays; } public void setCycleIntervalDays(Integer cycleIntervalDays) { this.cycleIntervalDays = cycleIntervalDays; }
    public String getStatus() { return status; } public void setStatus(String status) { this.status = status; }
    public LocalDate getStartDate() { return startDate; } public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; } public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public LocalDate getNextDueDate() { return nextDueDate; } public void setNextDueDate(LocalDate nextDueDate) { this.nextDueDate = nextDueDate; }
    public String getPrescribedBy() { return prescribedBy; } public void setPrescribedBy(String prescribedBy) { this.prescribedBy = prescribedBy; }
    public String getNotes() { return notes; } public void setNotes(String notes) { this.notes = notes; }
    public Long getUnitId() { return unitId; } public void setUnitId(Long unitId) { this.unitId = unitId; }

    /**
     * Legacy alias: the IHMS UI/API calls the planned cycle count "cycles".
     * Kept as a plain bean pair (JPA uses field access, so it is not persisted twice).
     */
    public Integer getCycles() { return totalCycles; }
    public void setCycles(Integer cycles) { this.totalCycles = cycles; }
}
