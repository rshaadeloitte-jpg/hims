package com.hims.equipment.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "equipment_med_gas_indent")
public class EquipmentMedGasIndent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "indent_no", unique = true) private String indentNo;
    @Column(name = "department_id") private Long departmentId;
    @Column(name = "gas_type") private String gasType;
    @Column(name = "quantity") private Integer quantity = 1;
    @Column(name = "status") private String status = "PENDING";
    @Column(name = "requested_by") private Long requestedBy;
    @Column(name = "approved_by") private Long approvedBy;
    @Column(name = "remarks", columnDefinition = "TEXT") private String remarks;
    @Column(name = "created_at") private LocalDateTime createdAt = LocalDateTime.now();

    public EquipmentMedGasIndent() {}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getIndentNo() { return indentNo; }
    public void setIndentNo(String indentNo) { this.indentNo = indentNo; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public String getGasType() { return gasType; }
    public void setGasType(String gasType) { this.gasType = gasType; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getRequestedBy() { return requestedBy; }
    public void setRequestedBy(Long requestedBy) { this.requestedBy = requestedBy; }
    public Long getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Long approvedBy) { this.approvedBy = approvedBy; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}