package com.hims.cssd.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * One instrument line of a kit (register WP-R3): item_id BIGINT NOT NULL,
 * qty INT CHECK qty &gt; 0, UNIQUE(kit_id, item_id) as the composite PK
 * (same composite-PK convention as t_prescription_detail in ordering-service).
 */
@Entity
@IdClass(KitItemKey.class)
@Table(name = "t_kit_item")
public class KitItem extends CssdBase {

    @Id
    @Column(name = "kit_id")
    private Long kitId;

    @Id
    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "qty", nullable = false)
    private Integer qty;

    @Override public Long getId() { return kitId; }
    @Override public void setId(Long id) { this.kitId = id; }
    public Long getKitId() { return kitId; }
    public void setKitId(Long kitId) { this.kitId = kitId; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public Integer getQty() { return qty; }
    public void setQty(Integer qty) { this.qty = qty; }
}
