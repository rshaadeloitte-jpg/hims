package com.hims.reference.domain.model;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_config")
public class AppConfig extends MasterEntityBase {
    @Id @Column(name = "id") private Long id;
    public Long getId() { return id; } public void setId(Long v) { this.id = v; }
    @Column(name = "config_key", nullable = false, length = 120) private String configKey;
    public String getConfigKey() { return configKey; } public void setConfigKey(String v) { this.configKey = v; }
    @Column(name = "config_value", columnDefinition = "text") private String configValue;
    public String getConfigValue() { return configValue; } public void setConfigValue(String v) { this.configValue = v; }
    @Column(name = "description", length = 500) private String description;
    public String getDescription() { return description; } public void setDescription(String v) { this.description = v; }
}
