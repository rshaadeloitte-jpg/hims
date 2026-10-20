package com.hims.ordering.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * One clinical service line of an order (MIG-033): item_type selects the
 * downstream saga consumer(s); rate NUMERIC(19,2) &gt;= 0 and qty INTEGER &gt; 0
 * are the semantics Billing validates before charging (legacy CIMS_ADDCHARGE).
 * doctorSharePercent (BR-ORD-004) travels in the OrderInitiated payload so
 * Billing can write its doctor-share charges (legacy T_DOCTORSHARECHARGESDETAIL).
 */
@Entity
@IdClass(OrderItemKey.class)
@Table(name = "order_item")
public class OrderItem extends PrescriptionBase {

    @Id
    @Column(name = "order_id")
    private Long orderId;

    @Id
    @Column(name = "line_no")
    private Long lineNo;

    @Column(name = "item_type", nullable = false, length = 20)
    private String itemType;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "service_name", nullable = false, length = 200)
    private String serviceName;

    @Column(name = "rate", nullable = false, precision = 19, scale = 2)
    private BigDecimal rate;

    @Column(name = "qty", nullable = false)
    private Integer qty;

    /** BR-ORD-004 doctor-share tag: percent 0..100, default 0, validated bounds. */
    @Column(name = "doctor_share_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal doctorSharePercent = BigDecimal.ZERO;

    /** Composite key has no single surrogate id (base contract satisfied structurally). */
    @Override public Long getId() { return orderId; }
    @Override public void setId(Long id) { this.orderId = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getLineNo() { return lineNo; }
    public void setLineNo(Long lineNo) { this.lineNo = lineNo; }
    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }
    public Integer getQty() { return qty; }
    public void setQty(Integer qty) { this.qty = qty; }
    public BigDecimal getDoctorSharePercent() { return doctorSharePercent; }
    public void setDoctorSharePercent(BigDecimal doctorSharePercent) { this.doctorSharePercent = doctorSharePercent; }

    public OrderItemType typeEnum() { return OrderItemType.valueOf(itemType); }
}
