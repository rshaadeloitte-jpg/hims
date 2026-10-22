package com.hims.equipment.domain.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "equipment_grn")
public class EquipmentGrn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "grn_no", unique = true)
    private String grnNo;

    @Column(name = "equipment_id")
    private Long equipmentId;

    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "grn_date")
    private LocalDate grnDate;

    @Column(name = "quantity")
    private Integer quantity = 1;

    @Column(name = "status")
    private String status = "PENDING";

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public EquipmentGrn() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getGrnNo() { return grnNo; }
    public void setGrnNo(String grnNo) { this.grnNo = grnNo; }
    public Long getEquipmentId() { return equipmentId; }
    public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public LocalDate getGrnDate() { return grnDate; }
    public void setGrnDate(LocalDate grnDate) { this.grnDate = grnDate; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}