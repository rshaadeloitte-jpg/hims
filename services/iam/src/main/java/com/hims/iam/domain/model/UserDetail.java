package com.hims.iam.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

/** Clean target of T_USERDETAILS (login subject store). password_hash is BCrypt only. */
@Entity
@Table(name = "t_userdetails")
public class UserDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "unit_id")
    private Long unitId;

    @Column(name = "username", nullable = false)
    private String username;

    /** BCrypt hash — never serialized to clients (see UserView). */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "email")
    private String email;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "employee_id")
    private Long employeeId;

    @Column(name = "status", nullable = false)
    private Boolean status = Boolean.TRUE;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "ward_id")
    private Long wardId;

    @Column(name = "department_id")
    private Long departmentId;

    // ------------------------------------------------------------- audit cols
    @Column(name = "added_by")
    private Long addedBy;

    @Column(name = "added_datetime")
    private Timestamp addedDatetime;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_datetime")
    private Timestamp updatedDatetime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUnitId() { return unitId; }
    public void setUnitId(Long unitId) { this.unitId = unitId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public Boolean getStatus() { return status; }
    public void setStatus(Boolean status) { this.status = status; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public Long getWardId() { return wardId; }
    public void setWardId(Long wardId) { this.wardId = wardId; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public Long getAddedBy() { return addedBy; }
    public void setAddedBy(Long addedBy) { this.addedBy = addedBy; }
    public Timestamp getAddedDatetime() { return addedDatetime; }
    public void setAddedDatetime(Timestamp addedDatetime) { this.addedDatetime = addedDatetime; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
    public Timestamp getUpdatedDatetime() { return updatedDatetime; }
    public void setUpdatedDatetime(Timestamp updatedDatetime) { this.updatedDatetime = updatedDatetime; }
}
