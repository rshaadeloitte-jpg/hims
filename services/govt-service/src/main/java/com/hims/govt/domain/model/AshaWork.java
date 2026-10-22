package com.hims.govt.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_ejs_asha_work")
public class AshaWork {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "beneficiary_id")
    private Long beneficiaryId;

    @Column(name = "beneficiary_name")
    private String beneficiaryName;

    @Column(name = "tracking_type", nullable = false)
    private String trackingType;

    @Column(name = "visit_date")
    private String visitDate;

    @Column(name = "asha_id")
    private Long ashaId;

    @Column(name = "asha_name")
    private String ashaName;

    @Column(name = "status")
    private String status = "SCHEDULED";

    @Column(name = "gestational_week")
    private Integer gestationalWeek;

    @Column(name = "weight")
    private BigDecimal weight;

    @Column(name = "bp_systolic")
    private Integer bpSystolic;

    @Column(name = "bp_diastolic")
    private Integer bpDiastolic;

    @Column(name = "hemoglobin")
    private BigDecimal hemoglobin;

    @Column(name = "immunization_given")
    private String immunizationGiven;

    @Column(name = "delivery_date")
    private String deliveryDate;

    @Column(name = "delivery_type")
    private String deliveryType;

    @Column(name = "postnatal_day")
    private Integer postnatalDay;

    @Column(name = "child_age_months")
    private Integer childAgeMonths;

    @Column(name = "weight_child")
    private BigDecimal weightChild;

    @Column(name = "height_child")
    private BigDecimal heightChild;

    @Column(name = "immunization_status")
    private String immunizationStatus;

    @Column(name = "nutrition_status")
    private String nutritionStatus;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "next_visit_date")
    private String nextVisitDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public AshaWork() {}

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBeneficiaryId() { return beneficiaryId; }
    public void setBeneficiaryId(Long beneficiaryId) { this.beneficiaryId = beneficiaryId; }
    public String getBeneficiaryName() { return beneficiaryName; }
    public void setBeneficiaryName(String beneficiaryName) { this.beneficiaryName = beneficiaryName; }
    public String getTrackingType() { return trackingType; }
    public void setTrackingType(String trackingType) { this.trackingType = trackingType; }
    public String getVisitDate() { return visitDate; }
    public void setVisitDate(String visitDate) { this.visitDate = visitDate; }
    public Long getAshaId() { return ashaId; }
    public void setAshaId(Long ashaId) { this.ashaId = ashaId; }
    public String getAshaName() { return ashaName; }
    public void setAshaName(String ashaName) { this.ashaName = ashaName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getGestationalWeek() { return gestationalWeek; }
    public void setGestationalWeek(Integer gestationalWeek) { this.gestationalWeek = gestationalWeek; }
    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    public Integer getBpSystolic() { return bpSystolic; }
    public void setBpSystolic(Integer bpSystolic) { this.bpSystolic = bpSystolic; }
    public Integer getBpDiastolic() { return bpDiastolic; }
    public void setBpDiastolic(Integer bpDiastolic) { this.bpDiastolic = bpDiastolic; }
    public BigDecimal getHemoglobin() { return hemoglobin; }
    public void setHemoglobin(BigDecimal hemoglobin) { this.hemoglobin = hemoglobin; }
    public String getImmunizationGiven() { return immunizationGiven; }
    public void setImmunizationGiven(String immunizationGiven) { this.immunizationGiven = immunizationGiven; }
    public String getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(String deliveryDate) { this.deliveryDate = deliveryDate; }
    public String getDeliveryType() { return deliveryType; }
    public void setDeliveryType(String deliveryType) { this.deliveryType = deliveryType; }
    public Integer getPostnatalDay() { return postnatalDay; }
    public void setPostnatalDay(Integer postnatalDay) { this.postnatalDay = postnatalDay; }
    public Integer getChildAgeMonths() { return childAgeMonths; }
    public void setChildAgeMonths(Integer childAgeMonths) { this.childAgeMonths = childAgeMonths; }
    public BigDecimal getWeightChild() { return weightChild; }
    public void setWeightChild(BigDecimal weightChild) { this.weightChild = weightChild; }
    public BigDecimal getHeightChild() { return heightChild; }
    public void setHeightChild(BigDecimal heightChild) { this.heightChild = heightChild; }
    public String getImmunizationStatus() { return immunizationStatus; }
    public void setImmunizationStatus(String immunizationStatus) { this.immunizationStatus = immunizationStatus; }
    public String getNutritionStatus() { return nutritionStatus; }
    public void setNutritionStatus(String nutritionStatus) { this.nutritionStatus = nutritionStatus; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public String getNextVisitDate() { return nextVisitDate; }
    public void setNextVisitDate(String nextVisitDate) { this.nextVisitDate = nextVisitDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}