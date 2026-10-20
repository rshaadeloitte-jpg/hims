package com.hims.pharmacy.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Entity
@Table(name = "t_drug_mapping")
public class DrugMapping extends PharmacyBase {
    @Id
    @Column(name = "mapping_id")
    private Long mappingId;
    @Column(name = "item_id")
    private Long itemId;
    @Column(name = "item_name")
    private String itemName;
    @Column(name = "generic_name")
    private String genericName;
    @Column(name = "brand_name")
    private String brandName;
    @Column(name = "category")
    private String category;
    @Column(name = "strength")
    private String strength;
    @Column(name = "uom")
    private String uom;
    @Column(name = "mrp")
    private BigDecimal mrp;
    @Column(name = "tax_percent")
    private BigDecimal taxPercent;
    @Column(name = "is_active")
    private Boolean isActive;

    @Override public Long getId() { return mappingId; }
    @Override public void setId(Long id) { this.mappingId = id; }

    public Long getMappingId() { return mappingId; }
    public void setMappingId(Long mappingId) { this.mappingId = mappingId; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public String getGenericName() { return genericName; }
    public void setGenericName(String genericName) { this.genericName = genericName; }
    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getStrength() { return strength; }
    public void setStrength(String strength) { this.strength = strength; }
    public String getUom() { return uom; }
    public void setUom(String uom) { this.uom = uom; }
    public BigDecimal getMrp() { return mrp; }
    public void setMrp(BigDecimal mrp) { this.mrp = mrp; }
    public BigDecimal getTaxPercent() { return taxPercent; }
    public void setTaxPercent(BigDecimal taxPercent) { this.taxPercent = taxPercent; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
