package com.hims.nursing.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "t_partograph")
public class Partograph extends NursingBase {

    @Id
    @Column(name = "partograph_id")
    private Long partographId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "admission_id", nullable = false)
    private Long admissionId;

    @Column(name = "opd_ipd_id")
    private Long opdIpdId;

    @Column(name = "opd_ipd")
    private Long opdIpd;

    @Column(name = "parity", length = 20)
    private String parity;

    @Column(name = "start_of_labour_date", length = 50)
    private String startOfLabourDate;

    @Column(name = "membrane_status", length = 20)
    private String membraneStatus;

    @Column(name = "membrane_rupture_date", columnDefinition = "TIMESTAMP(3)")
    private java.sql.Timestamp membraneRuptureDate;

    @Column(name = "membrane_vital_abb", length = 50)
    private String membraneVitalAbb;

    @Column(name = "cervix_dilation", precision = 4, scale = 1)
    private BigDecimal cervixDilation;

    @Column(name = "descent")
    private Integer descent;

    @Column(name = "systolic_bp")
    private Integer systolicBp;

    @Column(name = "diastolic_bp")
    private Integer diastolicBp;

    @Column(name = "heart_rate")
    private Integer heartRate;

    @Column(name = "temperature", precision = 4, scale = 1)
    private BigDecimal temperature;

    @Column(name = "respiration_rate")
    private Integer respirationRate;

    @Column(name = "oxygen_saturation", precision = 5, scale = 2)
    private BigDecimal oxygenSaturation;

    @Column(name = "contraction_freq")
    private Integer contractionFreq;

    @Column(name = "contraction_duration")
    private Integer contractionDuration;

    @Column(name = "foetal_heart_rate")
    private Integer foetalHeartRate;

    @Column(name = "moulding")
    private Integer moulding;

    @Column(name = "liquor", length = 20)
    private String liquor;

    @Column(name = "drugs_and_fluid", columnDefinition = "TEXT")
    private String drugsAndFluid;

    @Column(name = "oxytocin", length = 100)
    private String oxytocin;

    @Column(name = "urine_output")
    private Integer urineOutput;

    @Column(name = "urine_acetone", length = 20)
    private String urineAcetone;

    @Column(name = "urine_protein", length = 20)
    private String urineProtein;

    @Column(name = "delivery_date_time", length = 50)
    private String deliveryDateTime;

    @Column(name = "delivery_outcome", length = 50)
    private String deliveryOutcome;

    @Column(name = "baby_weight", precision = 6, scale = 2)
    private BigDecimal babyWeight;

    @Column(name = "apgar_score_1min")
    private Integer apgarScore1min;

    @Column(name = "apgar_score_5min")
    private Integer apgarScore5min;

    @Column(name = "apgar_score_10min")
    private Integer apgarScore10min;

    @Column(name = "apgar_score_15min")
    private Integer apgarScore15min;

    @Column(name = "apgar_score_20min")
    private Integer apgarScore20min;

    @Column(name = "placenta_type", length = 50)
    private String placentaType;

    @Column(name = "blood_loss_ml")
    private Integer bloodLossMl;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Override public Long getId() { return partographId; }
    @Override public void setId(Long id) { this.partographId = id; }
    public Long getPartographId() { return partographId; }
    public void setPartographId(Long v) { this.partographId = v; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long v) { this.patientId = v; }
    public Long getAdmissionId() { return admissionId; }
    public void setAdmissionId(Long v) { this.admissionId = v; }
    public Long getOpdIpdId() { return opdIpdId; }
    public void setOpdIpdId(Long v) { this.opdIpdId = v; }
    public Long getOpdIpd() { return opdIpd; }
    public void setOpdIpd(Long v) { this.opdIpd = v; }
    public String getParity() { return parity; }
    public void setParity(String v) { this.parity = v; }
    public String getStartOfLabourDate() { return startOfLabourDate; }
    public void setStartOfLabourDate(String v) { this.startOfLabourDate = v; }
    public String getMembraneStatus() { return membraneStatus; }
    public void setMembraneStatus(String v) { this.membraneStatus = v; }
    public java.sql.Timestamp getMembraneRuptureDate() { return membraneRuptureDate; }
    public void setMembraneRuptureDate(java.sql.Timestamp v) { this.membraneRuptureDate = v; }
    public String getMembraneVitalAbb() { return membraneVitalAbb; }
    public void setMembraneVitalAbb(String v) { this.membraneVitalAbb = v; }
    public BigDecimal getCervixDilation() { return cervixDilation; }
    public void setCervixDilation(BigDecimal v) { this.cervixDilation = v; }
    public Integer getDescent() { return descent; }
    public void setDescent(Integer v) { this.descent = v; }
    public Integer getSystolicBp() { return systolicBp; }
    public void setSystolicBp(Integer v) { this.systolicBp = v; }
    public Integer getDiastolicBp() { return diastolicBp; }
    public void setDiastolicBp(Integer v) { this.diastolicBp = v; }
    public Integer getHeartRate() { return heartRate; }
    public void setHeartRate(Integer v) { this.heartRate = v; }
    public BigDecimal getTemperature() { return temperature; }
    public void setTemperature(BigDecimal v) { this.temperature = v; }
    public Integer getRespirationRate() { return respirationRate; }
    public void setRespirationRate(Integer v) { this.respirationRate = v; }
    public BigDecimal getOxygenSaturation() { return oxygenSaturation; }
    public void setOxygenSaturation(BigDecimal v) { this.oxygenSaturation = v; }
    public Integer getContractionFreq() { return contractionFreq; }
    public void setContractionFreq(Integer v) { this.contractionFreq = v; }
    public Integer getContractionDuration() { return contractionDuration; }
    public void setContractionDuration(Integer v) { this.contractionDuration = v; }
    public Integer getFoetalHeartRate() { return foetalHeartRate; }
    public void setFoetalHeartRate(Integer v) { this.foetalHeartRate = v; }
    public Integer getMoulding() { return moulding; }
    public void setMoulding(Integer v) { this.moulding = v; }
    public String getLiquor() { return liquor; }
    public void setLiquor(String v) { this.liquor = v; }
    public String getDrugsAndFluid() { return drugsAndFluid; }
    public void setDrugsAndFluid(String v) { this.drugsAndFluid = v; }
    public String getOxytocin() { return oxytocin; }
    public void setOxytocin(String v) { this.oxytocin = v; }
    public Integer getUrineOutput() { return urineOutput; }
    public void setUrineOutput(Integer v) { this.urineOutput = v; }
    public String getUrineAcetone() { return urineAcetone; }
    public void setUrineAcetone(String v) { this.urineAcetone = v; }
    public String getUrineProtein() { return urineProtein; }
    public void setUrineProtein(String v) { this.urineProtein = v; }
    public String getDeliveryDateTime() { return deliveryDateTime; }
    public void setDeliveryDateTime(String v) { this.deliveryDateTime = v; }
    public String getDeliveryOutcome() { return deliveryOutcome; }
    public void setDeliveryOutcome(String v) { this.deliveryOutcome = v; }
    public BigDecimal getBabyWeight() { return babyWeight; }
    public void setBabyWeight(BigDecimal v) { this.babyWeight = v; }
    public Integer getApgarScore1min() { return apgarScore1min; }
    public void setApgarScore1min(Integer v) { this.apgarScore1min = v; }
    public Integer getApgarScore5min() { return apgarScore5min; }
    public void setApgarScore5min(Integer v) { this.apgarScore5min = v; }
    public Integer getApgarScore10min() { return apgarScore10min; }
    public void setApgarScore10min(Integer v) { this.apgarScore10min = v; }
    public Integer getApgarScore15min() { return apgarScore15min; }
    public void setApgarScore15min(Integer v) { this.apgarScore15min = v; }
    public Integer getApgarScore20min() { return apgarScore20min; }
    public void setApgarScore20min(Integer v) { this.apgarScore20min = v; }
    public String getPlacentaType() { return placentaType; }
    public void setPlacentaType(String v) { this.placentaType = v; }
    public Integer getBloodLossMl() { return bloodLossMl; }
    public void setBloodLossMl(Integer v) { this.bloodLossMl = v; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String v) { this.remarks = v; }
}
