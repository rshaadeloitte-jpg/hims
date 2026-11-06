package com.hims.cssd.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

/**
 * One sterilization cycle run against a kit (register WP-R3).
 * result SMALLINT NULL while running: 1=PASS 2=FAIL (set by PUT /cycles/{id}/complete).
 * PASS -&gt; kit STERILE (sterilized_at stamped); FAIL -&gt; kit REJECTED (unusable).
 */
@Entity
@Table(name = "t_sterilization_cycle")
public class SterilizationCycle extends CssdBase {

    public static final short RESULT_PASS = 1;
    public static final short RESULT_FAIL = 2;

    @Id
    @Column(name = "cycle_id")
    private Long cycleId;

    @Column(name = "kit_id", nullable = false)
    private Long kitId;

    @Column(name = "started_at", nullable = false)
    private Timestamp startedAt;

    @Column(name = "completed_at")
    private Timestamp completedAt;

    @Column(name = "method", nullable = false, length = 30)
    private String method;

    @Column(name = "biological_indicator") private String biologicalIndicator;
    public String getBiologicalIndicator() { return biologicalIndicator; } public void setBiologicalIndicator(String v) { this.biologicalIndicator = v; }
    @Column(name = "chemical_indicator") private String chemicalIndicator;
    public String getChemicalIndicator() { return chemicalIndicator; } public void setChemicalIndicator(String v) { this.chemicalIndicator = v; }
    @Column(name = "result")
    private Short result;

    public Long getCycleId() { return cycleId; }
    public void setCycleId(Long cycleId) { this.cycleId = cycleId; }
    @Override public Long getId() { return cycleId; }
    @Override public void setId(Long id) { this.cycleId = id; }
    public Long getKitId() { return kitId; }
    public void setKitId(Long kitId) { this.kitId = kitId; }
    public Timestamp getStartedAt() { return startedAt; }
    public void setStartedAt(Timestamp startedAt) { this.startedAt = startedAt; }
    public Timestamp getCompletedAt() { return completedAt; }
    public void setCompletedAt(Timestamp completedAt) { this.completedAt = completedAt; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public Short getResult() { return result; }
    public void setResult(Short result) { this.result = result; }

    public boolean isComplete() { return completedAt != null; }
    public boolean isQualityTestPassed() { return result != null && result == RESULT_PASS; }
}
