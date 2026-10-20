package com.hims.diet.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Clean target of legacy M_DIETPLANDETAILS (Dietplandetails.java). A line of a
 * diet plan: item + timing (session) + quantity/unit/calorie/carb/fat/protein
 * breakdown + instruction blob.
 */
@Entity
@Table(name = "diet_plan_details")
public class DietPlanDetails extends DietBase {

    @Id
    private Long id;

    @Column(name = "plan_id")
    private Long planId;

    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "timing")
    private Long timing;

    @Column(name = "item_qty")
    private String itemQty;

    @Column(name = "item_unit")
    private String itemUnit;

    @Column(name = "item_cal")
    private String itemCal;

    @Column(name = "item_ch")
    private String itemCh;

    @Column(name = "item_fat")
    private String itemFat;

    @Column(name = "item_expected_cal")
    private String itemExpectedCal;

    @Column(name = "item_instruction")
    private byte[] itemInstruction;

    @Column(name = "item_protein")
    private BigDecimal itemProtein;

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public Long getTiming() { return timing; }
    public void setTiming(Long timing) { this.timing = timing; }
    public String getItemQty() { return itemQty; }
    public void setItemQty(String itemQty) { this.itemQty = itemQty; }
    public String getItemUnit() { return itemUnit; }
    public void setItemUnit(String itemUnit) { this.itemUnit = itemUnit; }
    public String getItemCal() { return itemCal; }
    public void setItemCal(String itemCal) { this.itemCal = itemCal; }
    public String getItemCh() { return itemCh; }
    public void setItemCh(String itemCh) { this.itemCh = itemCh; }
    public String getItemFat() { return itemFat; }
    public void setItemFat(String itemFat) { this.itemFat = itemFat; }
    public String getItemExpectedCal() { return itemExpectedCal; }
    public void setItemExpectedCal(String itemExpectedCal) { this.itemExpectedCal = itemExpectedCal; }
    public byte[] getItemInstruction() { return itemInstruction; }
    public void setItemInstruction(byte[] itemInstruction) { this.itemInstruction = itemInstruction; }
    public BigDecimal getItemProtein() { return itemProtein; }
    public void setItemProtein(BigDecimal itemProtein) { this.itemProtein = itemProtein; }
}
