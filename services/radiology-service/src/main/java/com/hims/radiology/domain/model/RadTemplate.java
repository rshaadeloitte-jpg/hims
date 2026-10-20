package com.hims.radiology.domain.model;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "t_rad_template")
public class RadTemplate extends RadOrderBase {
    @Id @Column(name = "id") private Long id;
    public Long getId() { return id; } public void setId(Long v) { this.id = v; }
    @Column(name = "template_code", nullable = false, length = 64, unique = true) private String templateCode;
    public String getTemplateCode() { return templateCode; } public void setTemplateCode(String v) { this.templateCode = v; }
    @Column(name = "modality", nullable = false, length = 40) private String modality;
    public String getModality() { return modality; } public void setModality(String v) { this.modality = v; }
    @Column(name = "body_template", nullable = false, columnDefinition = "text") private String bodyTemplate;
    public String getBodyTemplate() { return bodyTemplate; } public void setBodyTemplate(String v) { this.bodyTemplate = v; }
    @Column(name = "description", length = 500) private String description;
    public String getDescription() { return description; } public void setDescription(String v) { this.description = v; }
}
