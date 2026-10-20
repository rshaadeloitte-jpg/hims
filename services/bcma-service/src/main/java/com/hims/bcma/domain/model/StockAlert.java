package com.hims.bcma.domain.model;

import jakarta.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "t_stock_alert")
public class StockAlert extends BcmaBase {

    @Id
    @Column(name = "alert_id")
    private Long alertId;

    @Column(name = "drug_ndc", nullable = false, length = 50)
    private String drugNdc;

    @Column(name = "alert_type", nullable = false, length = 50)
    private String alertType;

    @Column(name = "message", length = 200)
    private String message;

    @Column(name = "severity", length = 20)
    private String severity = "WARNING";

    @Column(name = "resolved", nullable = false)
    private Boolean resolved = false;

    @Column(name = "resolved_at")
    private Timestamp resolvedAt;

    @Override
    public Long getId() { return alertId; }
    @Override
    public void setId(Long id) { this.alertId = id; }

    public Long getAlertId() { return alertId; }
    public void setAlertId(Long alertId) { this.alertId = alertId; }
    public String getDrugNdc() { return drugNdc; }
    public void setDrugNdc(String drugNdc) { this.drugNdc = drugNdc; }
    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public Boolean getResolved() { return resolved; }
    public void setResolved(Boolean resolved) { this.resolved = resolved; }
    public Timestamp getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Timestamp resolvedAt) { this.resolvedAt = resolvedAt; }
}
