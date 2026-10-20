package com.hims.lab.domain.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Report print template storage (201 legacy templates).
 * Unifies M_PATHTEMPLATEMASTER (blob) + M_PATHOTEMPLATEMASTER (text).
 * template_type: REPORT, LABEL, RECEIPT, BARCODE.
 */
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Entity
@Table(name = "t_report_print_template")
public class ReportPrintTemplate extends LabBase {

    @Id
    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "code", nullable = false, length = 50, unique = true)
    private String code;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "template_content", columnDefinition = "TEXT")
    private String templateContent;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(name = "template_blob", columnDefinition = "BYTEA")
    private byte[] templateBlob;

    @Column(name = "gender_id")
    private Long genderId;

    @Column(name = "service_id")
    private Long serviceId;

    @Column(name = "organ_id")
    private Long organId;

    @Column(name = "is_clinical", nullable = false)
    private Boolean isClinical = false;

    @Column(name = "template_type", nullable = false, length = 20)
    private String templateType = "REPORT";

    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public Long getId() { return templateId; }
    public void setId(Long id) { this.templateId = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getTemplateContent() { return templateContent; }
    public void setTemplateContent(String templateContent) { this.templateContent = templateContent; }
    public byte[] getTemplateBlob() { return templateBlob; }
    public void setTemplateBlob(byte[] templateBlob) { this.templateBlob = templateBlob; }
    public Long getGenderId() { return genderId; }
    public void setGenderId(Long genderId) { this.genderId = genderId; }
    public Long getServiceId() { return serviceId; }
    public void setServiceId(Long serviceId) { this.serviceId = serviceId; }
    public Long getOrganId() { return organId; }
    public void setOrganId(Long organId) { this.organId = organId; }
    public Boolean getIsClinical() { return isClinical; }
    public void setIsClinical(Boolean isClinical) { this.isClinical = isClinical; }
    public String getTemplateType() { return templateType; }
    public void setTemplateType(String templateType) { this.templateType = templateType; }
}
