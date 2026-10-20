package com.hims.transport.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Clean target of legacy M_VEHICLEREQUESTTYPEMASTER (VehicleRequestTypeMaster.java).
 * Request-type dictionary (legacy getTripRequestCount matches description
 * emergency / normal / scheduled case-insensitively).
 */
@Entity
@Table(name = "vehicle_request_type")
public class VehicleRequestType extends TransportBase {

    @Id
    private Long id;

    @Column(name = "description")
    private String description;

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
