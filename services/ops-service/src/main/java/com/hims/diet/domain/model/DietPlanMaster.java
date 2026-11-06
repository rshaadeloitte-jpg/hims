package com.hims.diet.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Clean target of legacy M_DIETPLANMASTER (Dietplanmaster.java). A named diet
 * plan (code + description + plan information blob) attached to a service.
 */
@Entity
@Table(name = "diet_plan_master")
public class DietPlanMaster extends DietBase {

    @Id
    private Long id;

    @Column(name = "code")
    private String code;

    @Column(name = "description")
    private String description;

    @Column(name = "plan_information")
    private byte[] planInformation;

    @Column(name = "service_id")
    private Long serviceId;

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public byte[] getPlanInformation() { return planInformation; }
    public void setPlanInformation(byte[] planInformation) { this.planInformation = planInformation; }
    public Long getServiceId() { return serviceId; }
    public void setServiceId(Long serviceId) { this.serviceId = serviceId; }
}
