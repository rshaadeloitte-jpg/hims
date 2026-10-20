package com.hims.reference.domain.model;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "concession_template")
public class ConcessionTemplate extends MasterEntityBase {
    public static final short STATUS_ACTIVE = 1;
    public static final short STATUS_INACTIVE = 2;
    @Id @Column(name = "id") private Long id;
    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    @Column(name = "code", nullable = false, length = 64) private String code;
    public String getCode() { return code; } public void setCode(String v) { this.code = v; }
    @Column(name = "name", nullable = false, length = 200) private String name;
    public String getName() { return name; } public void setName(String v) { this.name = v; }
    @Column(name = "scheme", nullable = false, length = 64) private String scheme;
    public String getScheme() { return scheme; } public void setScheme(String v) { this.scheme = v; }
    @Column(name = "description", length = 500) private String description;
    public String getDescription() { return description; } public void setDescription(String v) { this.description = v; }
    @Column(name = "max_percent_off", nullable = false, precision = 5, scale = 2) private BigDecimal maxPercentOff;
    public BigDecimal getMaxPercentOff() { return maxPercentOff; } public void setMaxPercentOff(BigDecimal v) { this.maxPercentOff = v; }
    @Column(name = "requires_approval", nullable = false) private Boolean requiresApproval = false;
    public Boolean getRequiresApproval() { return requiresApproval; } public void setRequiresApproval(Boolean v) { this.requiresApproval = v; }
    @Column(name = "doc_status", nullable = false) private Short docStatus = STATUS_ACTIVE;
    public Short getDocStatus() { return docStatus; } public void setDocStatus(Short v) { this.docStatus = v; }
}
