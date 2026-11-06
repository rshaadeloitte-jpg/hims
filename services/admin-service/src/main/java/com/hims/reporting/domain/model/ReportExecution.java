package com.hims.reporting.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.sql.Timestamp;

/**
 * One synchronous execution of a report definition (register WP-T2).
 * status SMALLINT: 0=QUEUED 1=DONE 2=FAILED. The dispatcher is emulated inline:
 * executions are created DONE with the JSON result + row_count (register:
 * "report dispatcher emulated as JSON result" — disclosed; QUEUED/FAILED are
 * reserved for the async dispatcher which is OUT OF SCOPE).
 */
@Entity
@Table(name = "t_report_execution")
public class ReportExecution extends ReportingBase {

    public static final short STATUS_QUEUED = 0;
    public static final short STATUS_DONE = 1;
    public static final short STATUS_FAILED = 2;

    @Id
    @Column(name = "execution_id")
    private Long executionId;

    @Column(name = "report_id", nullable = false)
    private Long reportId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "params", columnDefinition = "jsonb")
    private String params;

    @Column(name = "status", nullable = false)
    private Short status = STATUS_QUEUED;

    @Column(name = "row_count")
    private Integer rowCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result", columnDefinition = "jsonb")
    private String result;

    @Column(name = "executed_at")
    private Timestamp executedAt;

    public Long getExecutionId() { return executionId; }
    public void setExecutionId(Long executionId) { this.executionId = executionId; }
    public Long getId() { return executionId; }
    public void setId(Long id) { this.executionId = id; }
    public Long getReportId() { return reportId; }
    public void setReportId(Long reportId) { this.reportId = reportId; }
    public String getParams() { return params; }
    public void setParams(String params) { this.params = params; }
    public Short getStatus() { return status; }
    public void setStatus(Short status) { this.status = status; }
    public Integer getRowCount() { return rowCount; }
    public void setRowCount(Integer rowCount) { this.rowCount = rowCount; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public Timestamp getExecutedAt() { return executedAt; }
    public void setExecutedAt(Timestamp executedAt) { this.executedAt = executedAt; }
}
