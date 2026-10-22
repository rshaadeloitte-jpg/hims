package com.hims.govt.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_ejs_geography")
public class Geography {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "asha_id")
    private Long ashaId;

    @Column(name = "asha_name")
    private String ashaName;

    @Column(name = "state")
    private String state;

    @Column(name = "district")
    private String district;

    @Column(name = "block")
    private String block;

    @Column(name = "village")
    private String village;

    @Column(name = "sub_center")
    private String subCenter;

    @Column(name = "total_population")
    private Integer totalPopulation = 0;

    @Column(name = "target_couples")
    private Integer targetCouples = 0;

    @Column(name = "pregnant_women")
    private Integer pregnantWomen = 0;

    @Column(name = "children_under5")
    private Integer childrenUnder5 = 0;

    @Column(name = "households")
    private Integer households = 0;

    @Column(name = "eligible_couples")
    private Integer eligibleCouples = 0;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "last_enumeration_date")
    private String lastEnumerationDate;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Geography() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAshaId() { return ashaId; }
    public void setAshaId(Long ashaId) { this.ashaId = ashaId; }
    public String getAshaName() { return ashaName; }
    public void setAshaName(String ashaName) { this.ashaName = ashaName; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public String getBlock() { return block; }
    public void setBlock(String block) { this.block = block; }
    public String getVillage() { return village; }
    public void setVillage(String village) { this.village = village; }
    public String getSubCenter() { return subCenter; }
    public void setSubCenter(String subCenter) { this.subCenter = subCenter; }
    public Integer getTotalPopulation() { return totalPopulation; }
    public void setTotalPopulation(Integer totalPopulation) { this.totalPopulation = totalPopulation; }
    public Integer getTargetCouples() { return targetCouples; }
    public void setTargetCouples(Integer targetCouples) { this.targetCouples = targetCouples; }
    public Integer getPregnantWomen() { return pregnantWomen; }
    public void setPregnantWomen(Integer pregnantWomen) { this.pregnantWomen = pregnantWomen; }
    public Integer getChildrenUnder5() { return childrenUnder5; }
    public void setChildrenUnder5(Integer childrenUnder5) { this.childrenUnder5 = childrenUnder5; }
    public Integer getHouseholds() { return households; }
    public void setHouseholds(Integer households) { this.households = households; }
    public Integer getEligibleCouples() { return eligibleCouples; }
    public void setEligibleCouples(Integer eligibleCouples) { this.eligibleCouples = eligibleCouples; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public String getLastEnumerationDate() { return lastEnumerationDate; }
    public void setLastEnumerationDate(String lastEnumerationDate) { this.lastEnumerationDate = lastEnumerationDate; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}