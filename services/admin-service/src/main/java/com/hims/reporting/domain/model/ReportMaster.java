package com.hims.reporting.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Report master definition (register WP-T2). kind SMALLINT: 1=TABULAR 2=SUMMARY.
 * definition JSONB over the rm_* read models:
 *   TABULAR: {"table":"rm_patient_registry","columns":["patientId","mrn","name"],
 *             "dateField":"registeredAt"}
 *   SUMMARY: {"table":"rm_revenue_daily","aggregate":"total"}
 * Jasper/Velocity rendering and regulatory exports are OUT OF SCOPE (comment-only).
 */
@Entity
@Table(name = "t_report_master")
public class ReportMaster extends ReportingBase {

    public static final short KIND_TABULAR = 1;
    public static final short KIND_SUMMARY = 2;

    @Id
    @Column(name = "report_id")
    private Long reportId;

    @Column(name = "report_code", nullable = false, length = 40)
    private String reportCode;

    @Column(name = "title", nullable = false, length = 120)
    private String title;

    @Column(name = "kind", nullable = false)
    private Short kind;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "definition", nullable = false, columnDefinition = "jsonb")
    private String definition;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = Boolean.TRUE;

    public Long getReportId() { return reportId; }
    public void setReportId(Long reportId) { this.reportId = reportId; }
    public void setId(Long id) { this.reportId = id; }
    public Long getId() { return reportId; }
    public String getReportCode() { return reportCode; }
    public void setReportCode(String reportCode) { this.reportCode = reportCode; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Short getKind() { return kind; }
    public void setKind(Short kind) { this.kind = kind; }
    public String getDefinition() { return definition; }
    public void setDefinition(String definition) { this.definition = definition; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
