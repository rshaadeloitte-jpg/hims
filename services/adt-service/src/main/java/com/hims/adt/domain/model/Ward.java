package com.hims.adt.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Ward master (clean target of the legacy ward master cluster — ownership moved to the
 * ADT service per MICROSERVICE-CATALOG #3 / POSTGRESQL-MIGRATION §1.1).
 * census flag drives whether beds in the ward count toward IPD census.
 */
@Entity
@Table(name = "m_ward")
public class Ward extends AdtBase {

    @Id
    @Column(name = "ward_id")
    private Long wardId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "ward_type")
    private String wardType;

    /** Whether this ward participates in the IPD census. */
    @Column(name = "iscensus", nullable = false)
    private Boolean iscensus = true;

    public Long getWardId() { return wardId; }
    public void setWardId(Long wardId) { this.wardId = wardId; }
    public Long getId() { return wardId; }
    public void setId(Long id) { this.wardId = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getWardType() { return wardType; }
    public void setWardType(String wardType) { this.wardType = wardType; }
    public Boolean getIscensus() { return iscensus; }
    public void setIscensus(Boolean iscensus) { this.iscensus = iscensus; }
}
