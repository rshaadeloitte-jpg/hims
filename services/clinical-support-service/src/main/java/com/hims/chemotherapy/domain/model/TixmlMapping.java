package com.hims.chemotherapy.domain.model;

import jakarta.persistence.*;

@Entity
@Table(name = "tixml_mapping")
public class TixmlMapping {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_name")
    private String templateName; // SURGONCOEXAM / SURGONCOHIST

    @Column(name = "template_json", columnDefinition = "TEXT")
    private String templateJson;

    public TixmlMapping() {}
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getTemplateName() { return templateName; } public void setTemplateName(String templateName) { this.templateName = templateName; }
    public String getTemplateJson() { return templateJson; } public void setTemplateJson(String templateJson) { this.templateJson = templateJson; }
}
