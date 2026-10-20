package com.hims.ordering.domain.model;

import java.io.Serializable;
import java.util.Objects;

/** Composite PK of order_item: (order_id, line_no). */
public class OrderItemKey implements Serializable {
    private Long orderId;
    private Long lineNo;

    public OrderItemKey() {}
    public OrderItemKey(Long orderId, Long lineNo) { this.orderId = orderId; this.lineNo = lineNo; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getLineNo() { return lineNo; }
    public void setLineNo(Long lineNo) { this.lineNo = lineNo; }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderItemKey that)) return false;
        return Objects.equals(orderId, that.orderId) && Objects.equals(lineNo, that.lineNo);
    }
    @Override public int hashCode() { return Objects.hash(orderId, lineNo); }
}
