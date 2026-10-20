package com.hims.aasmaan.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_aasmaan_monitor")
public class AasmaanMonitor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "monitor_id")
    private Long monitorId;

    @Column(name = "case_id", nullable = false)
    private Long caseId;

    @Column(name = "metric", length = 40)
    private String metric;

    @Column(name = "value")
    private Double value;

    @Column(name = "recorded_at")
    private LocalDateTime recordedAt;

    public Long getMonitorId() { return monitorId; }
    public void setMonitorId(Long monitorId) { this.monitorId = monitorId; }
    public Long getCaseId() { return caseId; }
    public void setCaseId(Long caseId) { this.caseId = caseId; }
    public String getMetric() { return metric; }
    public void setMetric(String metric) { this.metric = metric; }
    public Double getValue() { return value; }
    public void setValue(Double value) { this.value = value; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
}
