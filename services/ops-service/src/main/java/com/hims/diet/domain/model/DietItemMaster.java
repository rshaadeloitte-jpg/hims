package com.hims.diet.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Clean target of legacy M_DIETITEMMASTER (Dietitemmaster.java). A diet food
 * item: type, IEN code, description, energy (calories), diet-type IEN,
 * session ids (comma string in legacy), local unit. Legacy allowedFilters:
 * status.
 */
@Entity
@Table(name = "diet_item_master")
public class DietItemMaster extends DietBase {

    @Id
    private Long id;

    @Column(name = "diet_item_type")
    private String dietItemType;

    @Column(name = "diet_item_ien")
    private Long dietItemIen;

    @Column(name = "description")
    private String description;

    @Column(name = "energy")
    private Long energy;

    @Column(name = "diet_type_ien")
    private String dietTypeIen;

    @Column(name = "diet_session_id")
    private String dietSessionId;

    @Column(name = "local_unit")
    private String localUnit;

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public String getDietItemType() { return dietItemType; }
    public void setDietItemType(String dietItemType) { this.dietItemType = dietItemType; }
    public Long getDietItemIen() { return dietItemIen; }
    public void setDietItemIen(Long dietItemIen) { this.dietItemIen = dietItemIen; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getEnergy() { return energy; }
    public void setEnergy(Long energy) { this.energy = energy; }
    public String getDietTypeIen() { return dietTypeIen; }
    public void setDietTypeIen(String dietTypeIen) { this.dietTypeIen = dietTypeIen; }
    public String getDietSessionId() { return dietSessionId; }
    public void setDietSessionId(String dietSessionId) { this.dietSessionId = dietSessionId; }
    public String getLocalUnit() { return localUnit; }
    public void setLocalUnit(String localUnit) { this.localUnit = localUnit; }
}
