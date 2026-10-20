package com.hims.iam.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

/** Clean target of T_EMAILTEMPLATE (EmailTemplate). */
@Entity
@Table(name = "t_notification_template")
public class NotificationTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "channel", nullable = false)
    private String channel = "EMAIL";

    @Column(name = "subject")
    private String subject;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "sms_text")
    private String smsText;

    @Column(name = "template_id")
    private String templateId;

    @Column(name = "is_variable", nullable = false)
    private Boolean isVariable = false;

    @Column(name = "status", nullable = false)
    private Boolean status = Boolean.TRUE;

    @Column(name = "added_by")
    private Long addedBy;

    @Column(name = "added_datetime")
    private Timestamp addedDatetime;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_datetime")
    private Timestamp updatedDatetime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getSmsText() { return smsText; }
    public void setSmsText(String smsText) { this.smsText = smsText; }
    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    public Boolean getIsVariable() { return isVariable; }
    public void setIsVariable(Boolean isVariable) { this.isVariable = isVariable; }
    public Boolean getStatus() { return status; }
    public void setStatus(Boolean status) { this.status = status; }
    public Long getAddedBy() { return addedBy; }
    public void setAddedBy(Long addedBy) { this.addedBy = addedBy; }
    public Timestamp getAddedDatetime() { return addedDatetime; }
    public void setAddedDatetime(Timestamp addedDatetime) { this.addedDatetime = addedDatetime; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
    public Timestamp getUpdatedDatetime() { return updatedDatetime; }
    public void setUpdatedDatetime(Timestamp updatedDatetime) { this.updatedDatetime = updatedDatetime; }
}
