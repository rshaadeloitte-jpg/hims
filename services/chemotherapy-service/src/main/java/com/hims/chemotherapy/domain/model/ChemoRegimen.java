package com.hims.chemotherapy.domain.model;

import jakarta.persistence.*;

@Entity
@Table(name = "chemo_regimen_detail")
public class ChemoRegimen {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_id")
    private Long planId;

    @Column(name = "drug_name")
    private String drugName;

    @Column(name = "dosage")
    private String dosage;

    @Column(name = "unit")
    private String unit;

    @Column(name = "route")
    private String route;

    @Column(name = "day")
    private Integer day;

    public ChemoRegimen() {}
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getPlanId() { return planId; } public void setPlanId(Long planId) { this.planId = planId; }
    public String getDrugName() { return drugName; } public void setDrugName(String drugName) { this.drugName = drugName; }
    public String getDosage() { return dosage; } public void setDosage(String dosage) { this.dosage = dosage; }
    public String getUnit() { return unit; } public void setUnit(String unit) { this.unit = unit; }
    public String getRoute() { return route; } public void setRoute(String route) { this.route = route; }
    public Integer getDay() { return day; } public void setDay(Integer day) { this.day = day; }
}
