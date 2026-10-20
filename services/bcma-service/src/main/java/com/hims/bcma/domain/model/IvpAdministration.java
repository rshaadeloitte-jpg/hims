package com.hims.bcma.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Entity
@Table(name = "t_ivp_administration")
public class IvpAdministration extends BcmaBase {

    @Id
    @Column(name = "ivp_id")
    private Long ivpId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "drug_name", nullable = false, length = 200)
    private String drugName;

    @Column(name = "dose_amount", precision = 10, scale = 3)
    private BigDecimal doseAmount;

    @Column(name = "dose_unit", length = 20)
    private String doseUnit;

    @Column(name = "rate_ml_per_min", precision = 8, scale = 2)
    private BigDecimal rateMlPerMin;

    @Column(name = "dilution_required", nullable = false)
    private Boolean dilutionRequired = false;

    @Column(name = "dilution_volume_ml", precision = 8, scale = 2)
    private BigDecimal dilutionVolumeMl;

    @Column(name = "administered_by")
    private Long administeredBy;

    @Column(name = "administration_time")
    private Timestamp administrationTime;

    @Column(name = "administration_status", length = 20)
    private String administrationStatus = "PENDING";

    @Column(name = "ward_id")
    private Long wardId;

    @Override
    public Long getId() { return ivpId; }
    @Override
    public void setId(Long id) { this.ivpId = id; }

    public Long getIvpId() { return ivpId; }
    public void setIvpId(Long ivpId) { this.ivpId = ivpId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getDrugName() { return drugName; }
    public void setDrugName(String drugName) { this.drugName = drugName; }
    public BigDecimal getDoseAmount() { return doseAmount; }
    public void setDoseAmount(BigDecimal doseAmount) { this.doseAmount = doseAmount; }
    public String getDoseUnit() { return doseUnit; }
    public void setDoseUnit(String doseUnit) { this.doseUnit = doseUnit; }
    public BigDecimal getRateMlPerMin() { return rateMlPerMin; }
    public void setRateMlPerMin(BigDecimal rateMlPerMin) { this.rateMlPerMin = rateMlPerMin; }
    public Boolean getDilutionRequired() { return dilutionRequired; }
    public void setDilutionRequired(Boolean dilutionRequired) { this.dilutionRequired = dilutionRequired; }
    public BigDecimal getDilutionVolumeMl() { return dilutionVolumeMl; }
    public void setDilutionVolumeMl(BigDecimal dilutionVolumeMl) { this.dilutionVolumeMl = dilutionVolumeMl; }
    public Long getAdministeredBy() { return administeredBy; }
    public void setAdministeredBy(Long administeredBy) { this.administeredBy = administeredBy; }
    public Timestamp getAdministrationTime() { return administrationTime; }
    public void setAdministrationTime(Timestamp administrationTime) { this.administrationTime = administrationTime; }
    public String getAdministrationStatus() { return administrationStatus; }
    public void setAdministrationStatus(String administrationStatus) { this.administrationStatus = administrationStatus; }
    public Long getWardId() { return wardId; }
    public void setWardId(Long wardId) { this.wardId = wardId; }
}
