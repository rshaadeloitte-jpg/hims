package com.hims.bcma.domain.model;

import jakarta.persistence.*;

@Entity
@Table(name = "t_ivp_drug_config")
public class IvpDrugConfig extends BcmaBase {

    @Id
    @Column(name = "config_id")
    private Long configId;

    @Column(name = "drug_ndc", nullable = false, length = 50, unique = true)
    private String drugNdc;

    @Column(name = "max_rate_ml_per_min", precision = 8, scale = 2)
    private java.math.BigDecimal maxRateMlPerMin;

    @Column(name = "requires_dilution", nullable = false)
    private Boolean requiresDilution = false;

    @Column(name = "standard_dilution_volume_ml", precision = 8, scale = 2)
    private java.math.BigDecimal standardDilutionVolumeMl;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Override
    public Long getId() { return configId; }
    @Override
    public void setId(Long id) { this.configId = id; }

    public Long getConfigId() { return configId; }
    public void setConfigId(Long configId) { this.configId = configId; }
    public String getDrugNdc() { return drugNdc; }
    public void setDrugNdc(String drugNdc) { this.drugNdc = drugNdc; }
    public java.math.BigDecimal getMaxRateMlPerMin() { return maxRateMlPerMin; }
    public void setMaxRateMlPerMin(java.math.BigDecimal maxRateMlPerMin) { this.maxRateMlPerMin = maxRateMlPerMin; }
    public Boolean getRequiresDilution() { return requiresDilution; }
    public void setRequiresDilution(Boolean requiresDilution) { this.requiresDilution = requiresDilution; }
    public java.math.BigDecimal getStandardDilutionVolumeMl() { return standardDilutionVolumeMl; }
    public void setStandardDilutionVolumeMl(java.math.BigDecimal standardDilutionVolumeMl) { this.standardDilutionVolumeMl = standardDilutionVolumeMl; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
