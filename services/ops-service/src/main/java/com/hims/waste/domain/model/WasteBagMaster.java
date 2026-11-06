package com.hims.waste.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Clean target of M_WASTEBAGMASTER — bio-medical waste bag categories
 * (colour-coded bag types). Legacy @CrudApi(allowedFilters = {"status"}).
 * Server-owned id from waste_bag_master_seq (IMP-DEC-003).
 */
@Entity
@Table(name = "waste_bag_master")
public class WasteBagMaster extends WasteBase {

    @Id
    private Long id;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Override
    public Long getId() { return id; }
    @Override
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
