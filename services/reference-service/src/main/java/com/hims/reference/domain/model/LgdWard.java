package com.hims.reference.domain.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "lgd_ward", schema = "ihms_master")
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
public class LgdWard extends MasterEntityBase {

    @Column(name = "local_body_id", nullable = false)
    private Long localBodyId;

    @Column(name = "ward_category", nullable = false, length = 3)
    private String wardCategory;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "ward_number")
    private String wardNumber;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "name_local")
    private String nameLocal;

    public Long getLocalBodyId() { return localBodyId; }
    public void setLocalBodyId(Long localBodyId) { this.localBodyId = localBodyId; }
    public String getWardCategory() { return wardCategory; }
    public void setWardCategory(String wardCategory) { this.wardCategory = wardCategory; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getWardNumber() { return wardNumber; }
    public void setWardNumber(String wardNumber) { this.wardNumber = wardNumber; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getNameLocal() { return nameLocal; }
    public void setNameLocal(String nameLocal) { this.nameLocal = nameLocal; }
}
