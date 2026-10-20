package com.hims.ot.domain.model;

import jakarta.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "t_anesthesia_drug")
public class AnesthesiaDrug extends OtBase {

    @Id
    @Column(name = "drug_id")
    private Long drugId;

    @Column(name = "record_id", nullable = false)
    private Long recordId;

    @Column(name = "drug_name", nullable = false, length = 200)
    private String drugName;

    @Column(name = "dose", length = 100)
    private String dose;

    @Column(name = "route", length = 50)
    private String route;

    @Column(name = "administered_at")
    private Timestamp administeredAt;

    public Long getId() { return drugId; }
    public void setId(Long id) { this.drugId = id; }
    public Long getDrugId() { return drugId; }
    public void setDrugId(Long drugId) { this.drugId = drugId; }
    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }
    public String getDrugName() { return drugName; }
    public void setDrugName(String drugName) { this.drugName = drugName; }
    public String getDose() { return dose; }
    public void setDose(String dose) { this.dose = dose; }
    public String getRoute() { return route; }
    public void setRoute(String route) { this.route = route; }
    public Timestamp getAdministeredAt() { return administeredAt; }
    public void setAdministeredAt(Timestamp administeredAt) { this.administeredAt = administeredAt; }
}
