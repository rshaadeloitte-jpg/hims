package com.hims.lab.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

/**
 * Clean target of T_PATHOPATIENTPARAMETERDETAILS — one result parameter row.
 * Upsert keyed on (detail_id, parameter_name); finalize flips is_final=true +
 * stamps finalized_at on every row of the detail.
 */
@Entity
@Table(name = "t_pathopatientparameterdetails")
public class PathoPatientParameterDetails extends LabBase {

    @Id
    @Column(name = "param_id")
    private Long paramId;

    @Column(name = "detail_id", nullable = false)
    private Long detailId;

    @Column(name = "parameter_name", nullable = false, length = 200)
    private String parameterName;

    @Column(name = "value", columnDefinition = "varchar")
    private String value;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "reference_range", length = 100)
    private String referenceRange;

    @Column(name = "is_final", nullable = false)
    private Boolean isFinal = false;

    @Column(name = "finalized_at")
    private Timestamp finalizedAt;

    @Column(name = "is_critical", nullable = false)
    private Boolean isCritical = false;

    @Column(name = "critical_notified_at")
    private Timestamp criticalNotifiedAt;

    public Long getParamId() { return paramId; }
    public void setParamId(Long paramId) { this.paramId = paramId; }
    public Long getId() { return paramId; }
    public void setId(Long id) { this.paramId = id; }
    public Long getDetailId() { return detailId; }
    public void setDetailId(Long detailId) { this.detailId = detailId; }
    public String getParameterName() { return parameterName; }
    public void setParameterName(String parameterName) { this.parameterName = parameterName; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getReferenceRange() { return referenceRange; }
    public void setReferenceRange(String referenceRange) { this.referenceRange = referenceRange; }
    public Boolean getIsFinal() { return isFinal; }
    public void setIsFinal(Boolean isFinal) { this.isFinal = isFinal; }
    public Timestamp getFinalizedAt() { return finalizedAt; }
    public void setFinalizedAt(Timestamp finalizedAt) { this.finalizedAt = finalizedAt; }
    public Boolean getIsCritical() { return isCritical; }
    public void setIsCritical(Boolean isCritical) { this.isCritical = isCritical; }
    public Timestamp getCriticalNotifiedAt() { return criticalNotifiedAt; }
    public void setCriticalNotifiedAt(Timestamp criticalNotifiedAt) { this.criticalNotifiedAt = criticalNotifiedAt; }
}
