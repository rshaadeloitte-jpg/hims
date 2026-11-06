package com.hims.equipment.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "equipment_scrap")
public class EquipmentScrap {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "equipment_id") private Long equipmentId;
    @Column(name = "scrap_date") private LocalDate scrapDate;
    @Column(name = "scrap_value", precision = 12, scale = 2) private BigDecimal scrapValue;
    @Column(name = "buyer") private String buyer;
    @Column(name = "status") private String status = "PENDING";
    @Column(name = "remarks", columnDefinition = "TEXT") private String remarks;
    @Column(name = "created_at") private LocalDateTime createdAt = LocalDateTime.now();

    public EquipmentScrap() {}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEquipmentId() { return equipmentId; }
    public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }
    public LocalDate getScrapDate() { return scrapDate; }
    public void setScrapDate(LocalDate scrapDate) { this.scrapDate = scrapDate; }
    public BigDecimal getScrapValue() { return scrapValue; }
    public void setScrapValue(BigDecimal scrapValue) { this.scrapValue = scrapValue; }
    public String getBuyer() { return buyer; }
    public void setBuyer(String buyer) { this.buyer = buyer; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}