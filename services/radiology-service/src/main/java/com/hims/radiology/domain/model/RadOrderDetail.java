package com.hims.radiology.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * One procedure/item line of a rad order (register WP-R1): item_id BIGINT NOT NULL,
 * qty NUMERIC(12,2) CHECK qty &gt; 0, UNIQUE(rad_order_id, line_no) as the composite PK.
 */
@Entity
@IdClass(RadOrderDetailKey.class)
@Table(name = "t_radorder_detail")
public class RadOrderDetail extends RadOrderBase {

    @Id
    @Column(name = "rad_order_id")
    private Long radOrderId;

    @Id
    @Column(name = "line_no")
    private Long lineNo;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "qty", nullable = false, precision = 12, scale = 2)
    private BigDecimal qty;
    @Column(name = "retest_requested", nullable = false) private Boolean retestRequested = false;
    public Boolean getRetestRequested() { return retestRequested; } public void setRetestRequested(Boolean v) { this.retestRequested = v; }
    @Column(name = "retest_reason", length = 500) private String retestReason;
    public String getRetestReason() { return retestReason; } public void setRetestReason(String v) { this.retestReason = v; }
    @Column(name = "rescheduled_to") private Timestamp rescheduledTo;
    public Timestamp getRescheduledTo() { return rescheduledTo; } public void setRescheduledTo(Timestamp v) { this.rescheduledTo = v; }

    /** Composite key has no single surrogate id (base contract satisfied structurally). */
    @Override
    public Long getId() { return radOrderId; }
    @Override
    public void setId(Long id) { this.radOrderId = id; }
    public Long getRadOrderId() { return radOrderId; }
    public void setRadOrderId(Long radOrderId) { this.radOrderId = radOrderId; }
    public Long getLineNo() { return lineNo; }
    public void setLineNo(Long lineNo) { this.lineNo = lineNo; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public BigDecimal getQty() { return qty; }
    public void setQty(BigDecimal qty) { this.qty = qty; }
}
