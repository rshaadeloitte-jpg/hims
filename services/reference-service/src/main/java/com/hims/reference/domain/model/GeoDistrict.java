package com.hims.reference.domain.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Clean target of M_DISTRICTMASTER (AGT-02 D-6 global scope, V5). MIG-011 follow-up. */
@Entity
@Table(name = "geo_district")
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
public class GeoDistrict extends MasterEntityBase {

    @Column(name = "state_id", nullable = false)
    private Long stateId;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "census_2011_code")
    private String census2011Code;

    @Column(name = "zone_id")
    private Long zoneId;

    @Column(name = "abdm_district_code")
    private String abdmDistrictCode;

    public Long getStateId() { return stateId; }
    public void setStateId(Long stateId) { this.stateId = stateId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCensus2011Code() { return census2011Code; }
    public void setCensus2011Code(String census2011Code) { this.census2011Code = census2011Code; }
    public Long getZoneId() { return zoneId; }
    public void setZoneId(Long zoneId) { this.zoneId = zoneId; }
    public String getAbdmDistrictCode() { return abdmDistrictCode; }
    public void setAbdmDistrictCode(String abdmDistrictCode) { this.abdmDistrictCode = abdmDistrictCode; }
}
