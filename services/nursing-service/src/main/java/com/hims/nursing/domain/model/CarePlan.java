package com.hims.nursing.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "t_care_plan")
public class CarePlan extends NursingBase {

    public static final short STATUS_ACTIVE = 1;
    public static final short STATUS_RESOLVED = 2;
    public static final short STATUS_CANCELLED = 3;

    @Id
    @Column(name = "care_plan_id")
    private Long carePlanId;

    @Column(name = "admission_id", nullable = false)
    private Long admissionId;

    @Column(name = "nursing_diagnosis", nullable = false, length = 500)
    private String nursingDiagnosis;

    @Column(name = "goal", length = 500)
    private String goal;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "interventions", columnDefinition = "jsonb", nullable = false)
    private String interventions = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "outcomes", columnDefinition = "jsonb", nullable = false)
    private String outcomes = "[]";

    @Column(name = "status_code", nullable = false)
    private Short statusCode = STATUS_ACTIVE;

    @Override public Long getId() { return carePlanId; }
    @Override public void setId(Long id) { this.carePlanId = id; }
    public Long getCarePlanId() { return carePlanId; }
    public void setCarePlanId(Long v) { this.carePlanId = v; }
    public Long getAdmissionId() { return admissionId; }
    public void setAdmissionId(Long v) { this.admissionId = v; }
    public String getNursingDiagnosis() { return nursingDiagnosis; }
    public void setNursingDiagnosis(String v) { this.nursingDiagnosis = v; }
    public String getGoal() { return goal; }
    public void setGoal(String v) { this.goal = v; }
    public String getInterventions() { return interventions; }
    public void setInterventions(String v) { this.interventions = v; }
    public String getOutcomes() { return outcomes; }
    public void setOutcomes(String v) { this.outcomes = v; }
    public Short getStatusCode() { return statusCode; }
    public void setStatusCode(Short v) { this.statusCode = v; }
}
