package com.hims.reference.domain.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Clean target of M_SPECIALIZATION (AGT-02 §4.5, V5). MIG-011. */
@Entity
@Table(name = "specialization")
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
public class Specialization extends MasterEntityBase {

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "print_order")
    private Integer printOrder;

    @Column(name = "ledger_name")
    private String ledgerName;

    @Column(name = "is_doctor_show")
    private Boolean isDoctorShow = true;

    @Column(name = "is_department_show")
    private Boolean isDepartmentShow = true;

    @Column(name = "is_recalculate_service")
    private Boolean isRecalculateService = false;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getPrintOrder() { return printOrder; }
    public void setPrintOrder(Integer printOrder) { this.printOrder = printOrder; }
    public Boolean getIsDoctorShow() { return isDoctorShow; }
    public void setIsDoctorShow(Boolean isDoctorShow) { this.isDoctorShow = isDoctorShow; }
}
