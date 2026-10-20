package com.hims.chemotherapy.domain.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "chemo_cycle")
public class ChemoCycle {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_id")
    private Long planId;

    @Column(name = "cycle_number")
    private Integer cycleNumber;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "status")
    private String status; // SCHEDULED / IN_PROGRESS / COMPLETED / SKIPPED

    public ChemoCycle() {}
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getPlanId() { return planId; } public void setPlanId(Long planId) { this.planId = planId; }
    public Integer getCycleNumber() { return cycleNumber; } public void setCycleNumber(Integer cycleNumber) { this.cycleNumber = cycleNumber; }
    public LocalDate getStartDate() { return startDate; } public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public String getStatus() { return status; } public void setStatus(String status) { this.status = status; }
}
