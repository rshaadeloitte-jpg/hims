package com.hims.reference.domain.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Clean target of M_STATEMASTER (AGT-02 D-6 global scope, V5). MIG-011 follow-up. */
@Entity
@Table(name = "geo_state")
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
public class GeoState extends MasterEntityBase {

    @Column(name = "country_id", nullable = false)
    private Long countryId;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "name_hindi")
    private String nameHindi;

    @Column(name = "abdm_code")
    private String abdmCode;

    public Long getCountryId() { return countryId; }
    public void setCountryId(Long countryId) { this.countryId = countryId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getNameHindi() { return nameHindi; }
    public void setNameHindi(String nameHindi) { this.nameHindi = nameHindi; }
    public String getAbdmCode() { return abdmCode; }
    public void setAbdmCode(String abdmCode) { this.abdmCode = abdmCode; }
}
