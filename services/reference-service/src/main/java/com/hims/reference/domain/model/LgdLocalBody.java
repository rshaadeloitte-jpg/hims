package com.hims.reference.domain.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "lgd_local_body")
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
public class LgdLocalBody extends MasterEntityBase {

    @Column(name = "state_id", nullable = false)
    private Long stateId;

    @Column(name = "body_category", nullable = false, length = 3)
    private String bodyCategory;

    @Column(name = "type_code")
    private String typeCode;

    @Column(name = "type_name")
    private String typeName;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "lgd_version")
    private Integer lgdVersion;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "name_local")
    private String nameLocal;

    @Column(name = "parent_code")
    private String parentCode;

    @Column(name = "census_2001_code")
    private String census2001Code;

    @Column(name = "census_2011_code")
    private String census2011Code;

    public Long getStateId() { return stateId; }
    public void setStateId(Long stateId) { this.stateId = stateId; }
    public String getBodyCategory() { return bodyCategory; }
    public void setBodyCategory(String bodyCategory) { this.bodyCategory = bodyCategory; }
    public String getTypeCode() { return typeCode; }
    public void setTypeCode(String typeCode) { this.typeCode = typeCode; }
    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { this.typeName = typeName; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Integer getLgdVersion() { return lgdVersion; }
    public void setLgdVersion(Integer lgdVersion) { this.lgdVersion = lgdVersion; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getNameLocal() { return nameLocal; }
    public void setNameLocal(String nameLocal) { this.nameLocal = nameLocal; }
    public String getParentCode() { return parentCode; }
    public void setParentCode(String parentCode) { this.parentCode = parentCode; }
    public String getCensus2001Code() { return census2001Code; }
    public void setCensus2001Code(String census2001Code) { this.census2001Code = census2001Code; }
    public String getCensus2011Code() { return census2011Code; }
    public void setCensus2011Code(String census2011Code) { this.census2011Code = census2011Code; }
}
