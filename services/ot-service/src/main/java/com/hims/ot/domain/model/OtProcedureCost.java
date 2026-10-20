package com.hims.ot.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "t_ot_procedure_cost")
public class OtProcedureCost extends OtBase {

    @Id
    @Column(name = "cost_id")
    private Long costId;

    @Column(name = "estimate_id", nullable = false)
    private Long estimateId;

    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;

    @Column(name = "item_type", length = 50)
    private String itemType;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    public Long getId() { return costId; }
    public void setId(Long id) { this.costId = id; }
    public Long getCostId() { return costId; }
    public void setCostId(Long costId) { this.costId = costId; }
    public Long getEstimateId() { return estimateId; }
    public void setEstimateId(Long estimateId) { this.estimateId = estimateId; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
