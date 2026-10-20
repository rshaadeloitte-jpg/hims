package com.hims.transport.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Clean target of legacy M_AMBULANCETYPEMASTER (AmbulanceTypeMaster.java).
 * Ambulance / vehicle-type dictionary.
 */
@Entity
@Table(name = "ambulance_type")
public class AmbulanceType extends TransportBase {

    @Id
    private Long id;

    @Column(name = "description")
    private String description;

    @Column(name = "is_for_patient")
    private Boolean isForPatient;

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getIsForPatient() { return isForPatient; }
    public void setIsForPatient(Boolean isForPatient) { this.isForPatient = isForPatient; }
}
