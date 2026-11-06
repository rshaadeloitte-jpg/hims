package com.hims.cssd.domain.model;

import java.io.Serializable;
import java.util.Objects;

/** Composite key of t_kit_item: UNIQUE(kit_id, item_id). */
public class KitItemKey implements Serializable {

    private Long kitId;
    private Long itemId;

    public KitItemKey() {}
    public KitItemKey(Long kitId, Long itemId) {
        this.kitId = kitId;
        this.itemId = itemId;
    }

    public Long getKitId() { return kitId; }
    public Long getItemId() { return itemId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof KitItemKey that)) return false;
        return Objects.equals(kitId, that.kitId) && Objects.equals(itemId, that.itemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kitId, itemId);
    }
}
