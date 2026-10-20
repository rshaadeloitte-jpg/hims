package com.hims.iam.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

/** Clean target of t_role_enabledpermission (legacy RoleBasedPermission.enabledModulePermissionIdsJson). */
@Entity
@Table(name = "t_role_enabledpermission")
public class RolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id")
    private Long roleId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "enabled_permission_ids", columnDefinition = "TEXT")
    private String enabledPermissionIds; // JSON array "[1,2,3]"

    @Column(name = "status", nullable = false)
    private Boolean status = Boolean.TRUE;

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
    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getEnabledPermissionIds() { return enabledPermissionIds; }
    public void setEnabledPermissionIds(String enabledPermissionIds) { this.enabledPermissionIds = enabledPermissionIds; }
    public Boolean getStatus() { return status; }
    public void setStatus(Boolean status) { this.status = status; }
    public Long getAddedBy() { return addedBy; }
    public void setAddedBy(Long addedBy) { this.addedBy = addedBy; }
    public Timestamp getAddedDatetime() { return addedDatetime; }
    public void setAddedDatetime(Timestamp addedDatetime) { this.addedDatetime = addedDatetime; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
    public Timestamp getUpdatedDatetime() { return updatedDatetime; }
    public void setUpdatedDatetime(Timestamp updatedDatetime) { this.updatedDatetime = updatedDatetime; }
}
