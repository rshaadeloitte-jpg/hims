package com.hims.reference.domain.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "geo_block")
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
public class GeoBlock extends MasterEntityBase {

    @Column(name = "district_id", nullable = false)
    private Long districtId;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "lgd_version")
    private Integer lgdVersion;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "name_local")
    private String nameLocal;

    public Long getDistrictId() { return districtId; }
    public void setDistrictId(Long districtId) { this.districtId = districtId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Integer getLgdVersion() { return lgdVersion; }
    public void setLgdVersion(Integer lgdVersion) { this.lgdVersion = lgdVersion; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getNameLocal() { return nameLocal; }
    public void setNameLocal(String nameLocal) { this.nameLocal = nameLocal; }
}
