package com.hims.reference.domain.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Clean target of M_AREAMASTER (AGT-02 D-6 global scope, V5). MIG-011 follow-up. */
@Entity
@Table(name = "geo_area")
@AttributeOverrides({
        @AttributeOverride(name = "id", column = @Column(name = "id")),
        @AttributeOverride(name = "unitId", column = @Column(name = "unit_id")),
        @AttributeOverride(name = "status", column = @Column(name = "status")),
        @AttributeOverride(name = "createdunitid", column = @Column(name = "created_unit_id")),
        @AttributeOverride(name = "updatedunitid", column = @Column(name = "updated_unit_id")),
        @AttributeOverride(name = "addedby", column = @Column(name = "added_by")),
        @AttributeOverride(name = "addeddatetime", column = @Column(name = "added_datetime")),
        @AttributeOverride(name = "updatedby", column = @Column(name = "updated_by")),
        @AttributeOverride(name = "updateddatetime", column = @Column(name = "updated_datetime")),
        @AttributeOverride(name = "synchronizedd", column = @Column(name = "synchronized"))
})
public class GeoArea extends MasterEntityBase {

    @Column(name = "city_id", nullable = false)
    private Long cityId;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    public Long getCityId() { return cityId; }
    public void setCityId(Long cityId) { this.cityId = cityId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
