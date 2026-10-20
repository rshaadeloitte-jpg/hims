package com.hims.pharmacy.domain.model;

import java.io.Serializable;
import java.util.Objects;

/** Composite key of t_item_sale_detail: UNIQUE(sale_id, line_no). */
public class ItemSaleDetailKey implements Serializable {

    private Long saleId;
    private Long lineNo;

    public ItemSaleDetailKey() {}
    public ItemSaleDetailKey(Long saleId, Long lineNo) {
        this.saleId = saleId;
        this.lineNo = lineNo;
    }

    public Long getSaleId() { return saleId; }
    public Long getLineNo() { return lineNo; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemSaleDetailKey that)) return false;
        return Objects.equals(saleId, that.saleId) && Objects.equals(lineNo, that.lineNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(saleId, lineNo);
    }
}
