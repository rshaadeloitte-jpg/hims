package com.hims.chemotherapy.domain.model;

import jakarta.persistence.*;

@Entity
@Table(name = "chemo_coversheet")
public class ChemoCoversheet {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_id")
    private Long planId;

    @Column(name = "current_cycle")
    private Integer currentCycle;

    @Column(name = "lab_values", columnDefinition = "TEXT")
    private String labValues;

    @Column(name = "vitals", columnDefinition = "TEXT")
    private String vitals;

    public ChemoCoversheet() {}
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getPlanId() { return planId; } public void setPlanId(Long planId) { this.planId = planId; }
    public Integer getCurrentCycle() { return currentCycle; } public void setCurrentCycle(Integer currentCycle) { this.currentCycle = currentCycle; }
    public String getLabValues() { return labValues; } public void setLabValues(String labValues) { this.labValues = labValues; }
    public String getVitals() { return vitals; } public void setVitals(String vitals) { this.vitals = vitals; }
}
