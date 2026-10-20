package com.hims.bcma.domain.model;

import jakarta.persistence.*;

@Entity
@Table(name = "t_override_reason")
public class OverrideReason extends BcmaBase {

    @Id
    @Column(name = "reason_id")
    private Long reasonId;

    @Column(name = "code", nullable = false, length = 20, unique = true)
    private String code;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Override
    public Long getId() { return reasonId; }
    @Override
    public void setId(Long id) { this.reasonId = id; }

    public Long getReasonId() { return reasonId; }
    public void setReasonId(Long reasonId) { this.reasonId = reasonId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
